package com.thebrandolorian.counterweight.utils;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.thebrandolorian.counterweight.components.AnchorComponent;
import com.thebrandolorian.counterweight.components.RopeComponent;
import com.thebrandolorian.counterweight.components.RopeSegmentComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RopeUtils {
    private RopeUtils() { }

    // Main operations
    public static void removeRopeSegments(@Nonnull World world, @Nonnull RopeComponent ropeComponent) {
        world.execute(() -> {
            List<Vector3i> segments = new ArrayList<>();
            ropeComponent.getPaths().forEach(path -> {
                if (path.getSegments() != null) segments.addAll(path.getSegments());
            });

            if (!segments.isEmpty()) {
                BlockUtils.breakBlocksAtPositions(world, segments);
                DebugUtils.logInfo("Breaking rope with " + segments.size() + " segments.");
            }
        });
    }

    public static void removePartnerRopes(@Nonnull World world, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer, @Nonnull AnchorComponent anchorComponent) {
        Set<Vector3i> linkedPositions = anchorComponent.getLinkedAnchorPositions();
        Vector3i brokenAnchorPosition = anchorComponent.getPosition();
        if (linkedPositions == null || brokenAnchorPosition == null) { DebugUtils.logWarn("Cannot clear partner ropes - missing anchor position data"); return; }

        for (Vector3i partnerPos : linkedPositions) {
            processPartnerAnchor(world, store, commandBuffer, partnerPos, brokenAnchorPosition);
        }
    }

    // Helper methods
    public static void processPartnerAnchor(@Nonnull World world, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer, @Nonnull Vector3i partnerPos, @Nonnull Vector3i brokenAnchorPosition) {
        Ref<ChunkStore> partnerRef = BlockUtils.getBlockRefFromPosition(world, partnerPos);
        if (partnerRef == null || !partnerRef.isValid()) { DebugUtils.logWarn("Cannot process partner anchor at {} - invalid reference", partnerPos); return; }

        RopeComponent partnerRope = store.getComponent(partnerRef, RopeComponent.getComponentType());
        if (partnerRope == null) { DebugUtils.logInfo("No rope component on partner anchor at {}", partnerPos); return; }

        boolean pathsRemoved = removePathsToAnchor(world, partnerRope, brokenAnchorPosition);
        if (pathsRemoved) removeRopeComponentIfEmpty(commandBuffer, partnerRef, partnerRope);
    }

    public static boolean removePathsToAnchor(@Nonnull World world, @Nonnull RopeComponent ropeComponent, @Nonnull Vector3i targetAnchor) {
        List<Vector3i> allSegmentsToRemove = new ArrayList<>();

        for (RopeComponent.RopePath path : ropeComponent.getPaths()) {
            if (path.getTarget().equals(targetAnchor)) allSegmentsToRemove.addAll(path.getSegments());
        }

        boolean changed = ropeComponent.getPaths().removeIf(path -> path.getTarget().equals(targetAnchor));

        if (!allSegmentsToRemove.isEmpty()) {
            world.execute(() -> BlockUtils.breakBlocksAtPositions(world, allSegmentsToRemove));
        }

        return changed;
    }

    public static void removeRopeComponentIfEmpty(@Nonnull CommandBuffer<ChunkStore> commandBuffer, @Nonnull Ref<ChunkStore> partnerRef, @Nonnull RopeComponent partnerRope) {
        if (partnerRope.getPaths().isEmpty()) commandBuffer.removeComponent(partnerRef, RopeComponent.getComponentType());
    }

    public static void cleanupAnchorConnections(@Nonnull World world, @Nonnull Store<ChunkStore> store, @Nonnull Vector3i startAnchorPosition, @Nonnull Vector3i endAnchorPosition) {
        Ref<ChunkStore> startRef = BlockUtils.getBlockRefFromPosition(world, startAnchorPosition);
        if (startRef != null && startRef.isValid()) {
            AnchorComponent startAnchor = store.getComponent(startRef, AnchorComponent.getComponentType());
            if (startAnchor != null) startAnchor.removeAnchor(endAnchorPosition);
        }

        Ref<ChunkStore> endRef = BlockUtils.getBlockRefFromPosition(world, endAnchorPosition);
        if (endRef != null && endRef.isValid()) {
            AnchorComponent endAnchor = store.getComponent(endRef, AnchorComponent.getComponentType());
            if (endAnchor != null) endAnchor.removeAnchor(startAnchorPosition);
        }
    }

    public static boolean tryCreateRopeSegment(@Nonnull World world, @Nonnull Store<ChunkStore> store, @Nonnull Vector3i position, @Nonnull Vector3i start, @Nonnull Vector3i end, @Nonnull RotationTuple rotation) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(position.x, position.z);
        WorldChunk worldChunk = world.getChunkIfLoaded(chunkIndex);
        if (worldChunk == null) { DebugUtils.logWarn("Could not create rope segment, world chunk is null"); return false; }

        worldChunk.placeBlock(position.getX(), position.getY(), position.getZ(), "Rope", rotation, 10, false);
        Ref<ChunkStore> spawnedBlockRef = BlockUtils.getBlockRefFromPosition(world, position);
        if (spawnedBlockRef == null || !spawnedBlockRef.isValid()) { DebugUtils.logWarn("Could not create rope segment, spawned block reference is invalid"); return false; }

        RopeSegmentComponent segment = store.ensureAndGetComponent(spawnedBlockRef, RopeSegmentComponent.getComponentType());
        segment.setAnchors(start, end);
        return true;
    }

    public static void addSegmentsAlongAxis(Set<Vector3i> segmentsForThisPath, World world, Store<ChunkStore> chunkStore, Vector3i startPosition, Vector3i endPosition, char x, RotationTuple of) {
        switch (x) {
            case 'x' -> addSegmentsAlongXAxis(segmentsForThisPath, world, chunkStore, startPosition, endPosition, of);
            case 'y' -> addSegmentsAlongYAxis(segmentsForThisPath, world, chunkStore, startPosition, endPosition, of);
            case 'z' -> addSegmentsAlongZAxis(segmentsForThisPath, world, chunkStore, startPosition, endPosition, of);
            default -> DebugUtils.logWarn("Invalid axis character: " + x + ". Expected 'x', 'y', or 'z'");
        }
    }

    private static void addSegmentsAlongXAxis(Set<Vector3i> segmentsForThisPath, World world, Store<ChunkStore> chunkStore, Vector3i startPosition, Vector3i endPosition, RotationTuple of) {
        int min = Math.min(startPosition.getX(), endPosition.getX());
        int max = Math.max(startPosition.getX(), endPosition.getX());
        for (int xPos = min + 1; xPos < max; xPos++) {
            Vector3i segmentPos = new Vector3i(xPos, startPosition.getY(), startPosition.getZ());
            if (tryCreateRopeSegment(world, chunkStore, segmentPos, startPosition, endPosition, of)) segmentsForThisPath.add(segmentPos);
        }
    }

    private static void addSegmentsAlongYAxis(Set<Vector3i> segmentsForThisPath, World world, Store<ChunkStore> chunkStore, Vector3i startPosition, Vector3i endPosition, RotationTuple of) {
        int min = Math.min(startPosition.getY(), endPosition.getY());
        int max = Math.max(startPosition.getY(), endPosition.getY());
        for (int yPos = min + 1; yPos < max; yPos++) {
            Vector3i segmentPos = new Vector3i(startPosition.getX(), yPos, startPosition.getZ());
            if (tryCreateRopeSegment(world, chunkStore, segmentPos, startPosition, endPosition, of)) segmentsForThisPath.add(segmentPos);
        }
    }

    private static void addSegmentsAlongZAxis(Set<Vector3i> segmentsForThisPath, World world, Store<ChunkStore> chunkStore, Vector3i startPosition, Vector3i endPosition, RotationTuple of) {
        int min = Math.min(startPosition.getZ(), endPosition.getZ());
        int max = Math.max(startPosition.getZ(), endPosition.getZ());
        for (int zPos = min + 1; zPos < max; zPos++) {
            Vector3i segmentPos = new Vector3i(startPosition.getX(), startPosition.getY(), zPos);
            if (tryCreateRopeSegment(world, chunkStore, segmentPos, startPosition, endPosition, of)) segmentsForThisPath.add(segmentPos);
        }
    }
}