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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.thebrandolorian.counterweight.CounterweightPlugin;
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
        Ref<EntityStore> playerRef = context.getEntity();
        Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
        World world = commandBuffer.getExternalData().getWorld();
        TransformComponent transformComponent = commandBuffer.getComponent(playerRef, TransformComponent.getComponentType());
        ModelComponent modelComponent = commandBuffer.getComponent(playerRef, ModelComponent.getComponentType());
        HeadRotation headRotation = commandBuffer.getComponent(playerRef, HeadRotation.getComponentType());
        ItemStack heldItem = context.getHeldItem();
        if (player == null || transformComponent == null || modelComponent == null || headRotation == null || heldItem == null) {
            state.state = InteractionState.Failed;
            return;
        }

        // Check block
        InteractionConfiguration heldItemInteractionConfig = heldItem.getItem().getInteractionConfig();
        float distance = heldItemInteractionConfig.getUseDistance(player.getGameMode());
        Vector3d fromPos = transformComponent.getPosition().clone();
        fromPos.y += (double)modelComponent.getModel().getEyeHeight(playerRef, commandBuffer);
        Vector3d lookDir = headRotation.getDirection();
        Vector3d toPos = fromPos.clone().add(lookDir.scale((double)distance));

        AtomicBoolean linked = new AtomicBoolean(false);
        BlockIterator.iterateFromTo(fromPos, toPos, (x, y, z, px, py, pz, qx, qy, qz) -> {
            int blockId = world.getBlock(x, y, z);
            BlockType blockType = BlockType.getAssetMap().getAsset(blockId);

            if (blockType != null && blockType.getData() != null) {
                if (blockType.getId().equals("air")) {
                    CounterweightPlugin.get().getLogger().atSevere().log("Found air");
                    return true;
                }

                IntSet blockTags = blockType.getData().getExpandedTagIndexes();
                int[] allowedTagIndices = this.getAllowedBlockTags();

                for (int tagIndex : blockTags) {
                    if (Arrays.binarySearch(allowedTagIndices, tagIndex) >= 0) {
                        boolean succeeded = processLinking(context, new Vector3i(x, y, z));
                        linked.set(succeeded);
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

    private boolean processLinking(InteractionContext context, Vector3i pos) {
        ItemStack heldItem = context.getHeldItem();
        ItemContainer heldContainer = context.getHeldItemContainer();
        if (heldItem == null || heldContainer == null) return false;

        var sourcePos = heldItem.getFromMetadataOrNull("source_pos", Vector3i.CODEC);
        if (sourcePos == null) {
            ItemStack itemWithMetadata = heldItem.withMetadata("source_pos", Vector3i.CODEC, new Vector3i(pos.x, pos.y, pos.z));
            heldContainer.setItemStackForSlot(context.getHeldItemSlot(), itemWithMetadata);
            context.setHeldItem(itemWithMetadata);

            CounterweightPlugin.get().getLogger().atInfo().log("Link started");
        } else {
            if (sourcePos.equals(pos) || heldItem.getDurability() < durabilityCost) return false;

            ItemStack newItem = heldItem.withMetadata("source_pos", Vector3i.CODEC, null).withDurability(heldItem.getDurability() - durabilityCost);
            ItemStackSlotTransaction transaction = heldContainer.setItemStackForSlot(context.getHeldItemSlot(), newItem);
            if (!transaction.succeeded()) return false;

            context.setHeldItem(newItem);

            //TODO: linking logic - create rope entity/update components?

            CounterweightPlugin.get().getLogger().atInfo().log("Link complete");
        }
        return true;
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
