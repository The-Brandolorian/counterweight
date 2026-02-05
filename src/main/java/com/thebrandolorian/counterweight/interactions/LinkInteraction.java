package com.thebrandolorian.counterweight.interactions;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.thebrandolorian.counterweight.managers.RopeManager;
import com.thebrandolorian.counterweight.utils.DebugUtils;
import com.thebrandolorian.counterweight.utils.InteractionUtils;
import com.thebrandolorian.counterweight.utils.PlayerUtils;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public class LinkInteraction extends SimpleInstantInteraction {
	public static final BuilderCodec<LinkInteraction> CODEC = BuilderCodec.builder(
					LinkInteraction.class, LinkInteraction::new, SimpleInstantInteraction.CODEC)
			
			.append(new KeyedCodec<>("AllowedBlockTags", new ArrayCodec<>(Codec.STRING, String[]::new)),
					(i, v) -> i.allowedBlockTags = v, i -> i.allowedBlockTags)
			.add()
			
			.append(new KeyedCodec<>("DurabilityCost", Codec.DOUBLE),
					(i, v) -> i.durabilityCost = v, i -> i.durabilityCost)
			.add()
			
			.append(new KeyedCodec<>("RequiredMatchingAxes", Codec.INTEGER),
					(i, v) -> i.requiredMatchingAxes = v, i -> i.requiredMatchingAxes)
			.add()
			
			.afterDecode(i -> i.cachedTagIndices = null)
			.build();
	
	enum LinkResult {START, COMPLETE, FAIL_SAME, FAIL_ALIGNMENT, FAIL_DURABILITY}
	
	private static final class Messages {
		static final String LINK_START = "server.message.counterweight.link_start";
		static final String LINK_COMPLETE = "server.message.counterweight.link_complete";
		static final String LINK_FAIL_SAME = "server.message.counterweight.link_fail_same";
		static final String LINK_FAIL_ALIGNMENT = "server.message.counterweight.link_fail_alignment";
		static final String LINK_FAIL_DURABILITY = "server.message.counterweight.link_fail_durability";
		static final String LINK_FAIL_ERROR = "server.message.counterweight.link_error";
	}
	
	private static final String META_DATA_KEY = "source_position";
	
	protected @Nullable String[] allowedBlockTags;
	protected @Nullable int[] cachedTagIndices;
	protected double durabilityCost = 1.0;
	protected int requiredMatchingAxes = 2;
	
	public LinkInteraction() { }
	
	protected void firstRun(@NonNullDecl InteractionType type, @NonNullDecl InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler) {
		CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
		assert commandBuffer != null;
		
		World world = commandBuffer.getExternalData().getWorld();
		InteractionSyncData state = context.getState();
		Ref<EntityStore> playerStoreRef = context.getEntity();
		
		Player player = commandBuffer.getComponent(playerStoreRef, Player.getComponentType());
		PlayerRef playerRef = playerStoreRef.getStore().getComponent(playerStoreRef, PlayerRef.getComponentType());
		TransformComponent transformComponent = commandBuffer.getComponent(playerStoreRef, TransformComponent.getComponentType());
		ModelComponent modelComponent = commandBuffer.getComponent(playerStoreRef, ModelComponent.getComponentType());
		HeadRotation headRotation = commandBuffer.getComponent(playerStoreRef, HeadRotation.getComponentType());
		ItemStack heldItem = context.getHeldItem();
		ItemContainer heldContainer = context.getHeldItemContainer();
		if (player == null || playerRef == null || transformComponent == null || modelComponent == null || headRotation == null || heldItem == null || heldContainer == null) {
			DebugUtils.logWarn("LinkInteraction failed: Missing components. Player: {}, PlayerRef: {}, Transform: {}, Model: {}, HeadRotation: {}, HeldItem: {}, HeldContainer: {}",
							   player != null, playerRef != null, transformComponent != null, modelComponent != null, headRotation != null, heldItem != null, heldContainer != null);
			state.state = InteractionState.Failed;
			return;
		}
		
		float interactionDistance = heldItem.getItem().getInteractionConfig().getUseDistance(player.getGameMode());
		Vector3i sourcePosition = heldItem.getFromMetadataOrNull(LinkInteraction.META_DATA_KEY, Vector3i.CODEC);
		Vector3i foundPosition = getAllowedBlockOrNull(commandBuffer, world, interactionDistance, playerStoreRef, transformComponent, modelComponent, headRotation);
		if (foundPosition == null) {
			DebugUtils.logWarn("LinkInteraction failed: No valid block found within interaction distance. Player: {}, Distance: {}", playerRef, interactionDistance);
			state.state = InteractionState.Failed;
			return;
		}
		
		LinkResult result = canLink(heldItem.getDurability(), sourcePosition, foundPosition);
		if (!tryApplyLinkResult(world, result, context, heldContainer, heldItem, playerRef, sourcePosition, foundPosition)) state.state = InteractionState.Failed;
		else state.state = InteractionState.Finished;
	}
	
	private Vector3i getAllowedBlockOrNull(CommandBuffer<EntityStore> commandBuffer, World world, float distance, Ref<EntityStore> entityStoreRef, TransformComponent transformComponent,
										   ModelComponent modelComponent, HeadRotation headRotation)
	{
		Vector3d fromPosition = transformComponent.getPosition().clone();
		Vector3d lookDirection = headRotation.getDirection();
		Vector3d toPosition = fromPosition.clone().add(lookDirection.scale(distance));
		fromPosition.y += modelComponent.getModel().getEyeHeight(entityStoreRef, commandBuffer);
		
		return InteractionUtils.getFirstBlockWithTagOrNull(world, fromPosition, toPosition, ensureAndGetCachedAllowedBlockTags());
	}
	
	protected int[] ensureAndGetCachedAllowedBlockTags() {
		if (this.cachedTagIndices != null && this.cachedTagIndices.length > 0) return this.cachedTagIndices;
		
		if (this.allowedBlockTags == null || this.allowedBlockTags.length == 0) { this.cachedTagIndices = new int[0]; return this.cachedTagIndices; }
		
		int[] indices = new int[this.allowedBlockTags.length];
		for (int i = 0; i < this.allowedBlockTags.length; i++) {
			indices[i] = AssetRegistry.getTagIndex(this.allowedBlockTags[i]);
		}
		
		Arrays.sort(indices);
		this.cachedTagIndices = indices;
		
		return this.cachedTagIndices;
	}
	
	private LinkResult canLink(double durability, Vector3i sourcePosition, Vector3i targetPosition) {
		if (sourcePosition == null) return LinkResult.START;
		if (sourcePosition.equals(targetPosition)) return LinkResult.FAIL_SAME;
		if (!isAxisAligned(sourcePosition, targetPosition)) return LinkResult.FAIL_ALIGNMENT;
		if (durability < durabilityCost) return LinkResult.FAIL_DURABILITY;
		return LinkResult.COMPLETE;
	}
	
	private boolean isAxisAligned(Vector3i pos1, Vector3i pos2) {
		int matchingAxes = 0;
		if (pos1.x == pos2.x) matchingAxes++;
		if (pos1.y == pos2.y) matchingAxes++;
		if (pos1.z == pos2.z) matchingAxes++;
		
		return matchingAxes >= requiredMatchingAxes;
	}
	
	private boolean tryApplyLinkResult(World world, LinkResult result, InteractionContext context, ItemContainer heldContainer, ItemStack heldItem, PlayerRef playerRef, Vector3i sourcePosition,
									   Vector3i targetPosition)
	{
		ItemStack updatedItem = null;
		String message;
		switch (result) {
			case START:
				updatedItem = heldItem.withMetadata(LinkInteraction.META_DATA_KEY, Vector3i.CODEC, targetPosition);
				message = Messages.LINK_START;
				break;
			case COMPLETE:
				updatedItem = heldItem.withMetadata(LinkInteraction.META_DATA_KEY, Vector3i.CODEC, null).withDurability(heldItem.getDurability() - durabilityCost);
				message = Messages.LINK_COMPLETE;
				break;
			case FAIL_SAME:
				message = Messages.LINK_FAIL_SAME;
				break;
			case FAIL_ALIGNMENT:
				updatedItem = heldItem.withMetadata(LinkInteraction.META_DATA_KEY, Vector3i.CODEC, null);
				message = Messages.LINK_FAIL_ALIGNMENT;
				break;
			case FAIL_DURABILITY:
				updatedItem = heldItem.withMetadata(LinkInteraction.META_DATA_KEY, Vector3i.CODEC, null);
				message = Messages.LINK_FAIL_DURABILITY;
				break;
			default:
				return false;
		}
		
		if (updatedItem == null) { playerRef.sendMessage(Message.translation(message)); return false; }
		
		if (!PlayerUtils.tryUpdateHeldItem(context, heldContainer, updatedItem)) {
			playerRef.sendMessage(Message.translation(Messages.LINK_FAIL_ERROR));
			return false;
		}
		
		if (result == LinkResult.COMPLETE && !RopeManager.tryCreateRopeBetweenBlocks(world, sourcePosition, targetPosition)) {
			playerRef.sendMessage(Message.translation(Messages.LINK_FAIL_ERROR));
			return false;
		}
		
		playerRef.sendMessage(Message.translation(message));
		return true;
	}
	
	@Nonnull
	@Override
	public String toString() {
		return "LinkInteraction{allowedTagIndices="
				+ Arrays.toString(this.cachedTagIndices)
				+ ", durabilityCost="
				+ durabilityCost
				+ "} "
				+ super.toString();
	}
}
