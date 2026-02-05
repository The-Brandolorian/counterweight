package com.thebrandolorian.counterweight.managers;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.thebrandolorian.counterweight.components.AnchorComponent;
import com.thebrandolorian.counterweight.components.RopeComponent;
import com.thebrandolorian.counterweight.utils.BlockUtils;
import com.thebrandolorian.counterweight.utils.DebugUtils;
import com.thebrandolorian.counterweight.utils.RopeUtils;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class RopeManager {
	private static final RotationTuple X_AXIS_ROTATION = RotationTuple.of(Rotation.None, Rotation.None, Rotation.Ninety);
	private static final RotationTuple Z_AXIS_ROTATION = RotationTuple.of(Rotation.Ninety, Rotation.None, Rotation.Ninety);
	
	private RopeManager() { }
	
	public static boolean tryCreateRopeBetweenBlocks(@Nonnull World world, @Nonnull Vector3i startPosition, @Nonnull Vector3i endPosition) {
		Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
		
		Ref<ChunkStore> startBlockRef = BlockUtils.getBlockRefFromPosition(world, startPosition);
		Ref<ChunkStore> endBlockRef = BlockUtils.getBlockRefFromPosition(world, endPosition);
		if (startBlockRef == null || endBlockRef == null) {
			DebugUtils.logWarn("Failed to create rope: One or both block positions are invalid. Start: {}, End: {}", startPosition, endPosition);
			return false;
		}
		
		AnchorComponent startAnchorComponent = chunkStore.getComponent(startBlockRef, AnchorComponent.getComponentType());
		AnchorComponent endAnchorComponent = chunkStore.getComponent(endBlockRef, AnchorComponent.getComponentType());
		if (startAnchorComponent == null || endAnchorComponent == null) {
			DebugUtils.logWarn("Failed to create rope: One or both blocks do not have AnchorComponents. Start: {}, End: {}", startPosition, endPosition);
			return false;
		}
		
		RopeComponent ropeComponent = chunkStore.ensureAndGetComponent(startBlockRef, RopeComponent.getComponentType());
		
		startAnchorComponent.setPosition(startPosition);
		startAnchorComponent.addAnchor(endPosition);
		
		endAnchorComponent.setPosition(endPosition);
		endAnchorComponent.addAnchor(startPosition);
		
		int dx = endPosition.getX() - startPosition.getX();
		int dy = endPosition.getY() - startPosition.getY();
		int dz = endPosition.getZ() - startPosition.getZ();
		
		world.execute(() -> {
			Set<Vector3i> segmentsForThisPath = new HashSet<>();
			
			if (dx != 0) RopeUtils.addSegmentsAlongAxis(segmentsForThisPath, world, chunkStore, startPosition, endPosition, 'x', RopeManager.X_AXIS_ROTATION);
			else if (dz != 0) RopeUtils.addSegmentsAlongAxis(segmentsForThisPath, world, chunkStore, startPosition, endPosition, 'z', RopeManager.Z_AXIS_ROTATION);
			else if (dy != 0) RopeUtils.addSegmentsAlongAxis(segmentsForThisPath, world, chunkStore, startPosition, endPosition, 'y', RotationTuple.NONE);
			
			ropeComponent.addPath(endPosition, segmentsForThisPath);
			DebugUtils.logInfo("New rope path registered from {} to {} with {} segments.", startPosition, endPosition, segmentsForThisPath.size());
		});
		
		return true;
	}
	
	
}