package com.thebrandolorian.counterweight.interactions;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.iterator.BlockIterator;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.InteractionConfiguration;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.thebrandolorian.counterweight.managers.RopeManager;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class LinkInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<LinkInteraction> CODEC = BuilderCodec.builder(
            LinkInteraction.class, LinkInteraction::new, SimpleInstantInteraction.CODEC)

            .append(new KeyedCodec<>("AllowedBlockTags", new ArrayCodec<>(Codec.STRING, String[]::new)),
                    (i, v) -> i.allowedBlockTags = v,
                    (i) -> i.allowedBlockTags)
            .add()

            .append(new KeyedCodec<>("DurabilityCost", Codec.DOUBLE),
                    (i, v) -> i.durabilityCost = v,
                    (i) -> i.durabilityCost)
            .add()

            .afterDecode((i) -> i.cachedTagIndices = null)
            .build();

    protected @Nullable String[] allowedBlockTags;
    protected double durabilityCost = 1.0;

    protected @Nullable int[] cachedTagIndices;

    enum LinkResult { START, COMPLETE, IGNORED, FAIL_ALIGNMENT, FAIL_DURABILITY }

    public LinkInteraction() { }

    protected int[] getAllowedBlockTags() {
        if (this.cachedTagIndices != null && this.cachedTagIndices.length > 0) return this.cachedTagIndices;
        if (this.allowedBlockTags == null || this.allowedBlockTags.length == 0) return this.cachedTagIndices = new int[0];

        this.cachedTagIndices = Arrays.stream(this.allowedBlockTags)
                .mapToInt(AssetRegistry::getTagIndex)
                .sorted()
                .toArray();

        return this.cachedTagIndices;
    }

    protected void firstRun(@NonNullDecl InteractionType type, @NonNullDecl InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        assert commandBuffer != null;

        // Get components
        InteractionSyncData state = context.getState();
        Ref<EntityStore> playerEntity = context.getEntity();
        Player player = commandBuffer.getComponent(playerEntity, Player.getComponentType());
        PlayerRef playerRef = playerEntity.getStore().getComponent(playerEntity, PlayerRef.getComponentType());

        World world = commandBuffer.getExternalData().getWorld();
        TransformComponent transformComponent = commandBuffer.getComponent(playerEntity, TransformComponent.getComponentType());
        ModelComponent modelComponent = commandBuffer.getComponent(playerEntity, ModelComponent.getComponentType());
        HeadRotation headRotation = commandBuffer.getComponent(playerEntity, HeadRotation.getComponentType());

        ItemStack heldItem = context.getHeldItem();
        ItemContainer heldContainer = context.getHeldItemContainer();
        if (player == null || playerRef == null || transformComponent == null || modelComponent == null || headRotation == null || heldItem == null || heldContainer == null) {
            state.state = InteractionState.Failed;
            return;
        }

        // Check block
        InteractionConfiguration heldItemInteractionConfig = heldItem.getItem().getInteractionConfig();
        float distance = heldItemInteractionConfig.getUseDistance(player.getGameMode());
        Vector3d fromPosition = transformComponent.getPosition().clone();
        fromPosition.y += modelComponent.getModel().getEyeHeight(playerEntity, commandBuffer);
        Vector3d lookDirection = headRotation.getDirection();
        Vector3d toPosition = fromPosition.clone().add(lookDirection.scale((double)distance));

        AtomicBoolean linked = new AtomicBoolean(false);
        BlockIterator.iterateFromTo(fromPosition, toPosition, (x, y, z, px, py, pz, qx, qy, qz) -> {
            int blockId = world.getBlock(x, y, z);
            BlockType blockType = BlockType.getAssetMap().getAsset(blockId);

            if (blockType != null && blockType.getData() != null) {
                IntSet blockTags = blockType.getData().getExpandedTagIndexes();
                int[] allowedTagIndices = this.getAllowedBlockTags();

                for (int tagIndex : blockTags) {
                    if (Arrays.binarySearch(allowedTagIndices, tagIndex) >= 0) {
                        var sourcePosition = heldItem.getFromMetadataOrNull("source_position", Vector3i.CODEC);
                        Vector3i targetPosition = new Vector3i(x, y, z);
                        LinkResult result = canLink(heldItem.getDurability(), sourcePosition, targetPosition);

                        switch (result) {
                            case START:
                                ItemStack startItem = heldItem.withMetadata("source_position", Vector3i.CODEC, targetPosition);
                                if (updateHeldItem(context, heldContainer, startItem)) {
                                    playerRef.sendMessage(Message.translation("server.message.counterweight.link_start"));
                                    linked.set(true);
                                }
                                break;

                            case COMPLETE:
                                ItemStack linkedItem = heldItem.withMetadata("source_position", Vector3i.CODEC, null).withDurability(heldItem.getDurability() - durabilityCost);
                                if (updateHeldItem(context, heldContainer, linkedItem)) {
                                    // TODO: linking logic - create rope entity/update components?

                                    boolean spawned = RopeManager.trySpawnRopeBetweenBlocks(world, sourcePosition, targetPosition);

                                    playerRef.sendMessage(Message.translation("server.message.counterweight.link_complete"));
                                    linked.set(true);
                                }
                                break;

                            case IGNORED:
                                playerRef.sendMessage(Message.translation("server.message.counterweight.link_error_same"));
                                linked.set(true);
                                break;

                            case FAIL_ALIGNMENT:
                            case FAIL_DURABILITY:
                                ItemStack clearedItem = heldItem.withMetadata("source_position", Vector3i.CODEC, null);
                                if (updateHeldItem(context, heldContainer, clearedItem)) {
                                    String failType = (result == LinkResult.FAIL_ALIGNMENT) ? "link_fail_alignment" : "link_fail_durability";
                                    playerRef.sendMessage(Message.translation("server.message.counterweight." + failType));
                                    linked.set(false);
                                }
                                break;
                        }
                        return false;
                    }
                }
                return false;
            }
            return true;
        });

        if (!linked.get()) state.state = InteractionState.Failed;
        else state.state = InteractionState.Finished;
    }

    private LinkResult canLink(double durability, Vector3i sourcePosition, Vector3i targetPosition) {
        if (sourcePosition == null) return LinkResult.START;
        if (sourcePosition.equals(targetPosition)) return LinkResult.IGNORED;
        if (!checkInline(sourcePosition, targetPosition)) return LinkResult.FAIL_ALIGNMENT;
        if (durability < durabilityCost) return LinkResult.FAIL_DURABILITY;
        return LinkResult.COMPLETE;
    }

    private boolean checkInline(Vector3i pos1, Vector3i pos2) {
        int matchingAxes = 0;
        if (pos1.x == pos2.x) matchingAxes++;
        if (pos1.y == pos2.y) matchingAxes++;
        if (pos1.z == pos2.z) matchingAxes++;

        return matchingAxes >= 2;
    }

    private boolean updateHeldItem(InteractionContext context, ItemContainer container, ItemStack newItem) {
        ItemStackSlotTransaction transaction = container.setItemStackForSlot(context.getHeldItemSlot(), newItem);
        if (transaction.succeeded()) { context.setHeldItem(newItem); return true; }

        return false;
    }

    @Nonnull
    public String toString() {
        return "LinkInteraction{allowedTagIndices="
                + Arrays.toString(this.allowedBlockTags)
                + ", durabilityCost="
                + durabilityCost
                + "} "
                + super.toString();
    }
}
