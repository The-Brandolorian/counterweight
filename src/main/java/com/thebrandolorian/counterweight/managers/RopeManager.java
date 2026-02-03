package com.thebrandolorian.counterweight.managers;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.thebrandolorian.counterweight.CounterweightPlugin;
import com.thebrandolorian.counterweight.components.AnchorComponent;
import com.thebrandolorian.counterweight.components.RopeComponent;
import com.thebrandolorian.counterweight.components.RopeSegmentComponent;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class RopeManager {

    static final HytaleLogger logger = CounterweightPlugin.get().getLogger();
    public static boolean trySpawnRopeBetweenBlocks(
            @Nonnull World world,
            @Nonnull Vector3i start,
            @Nonnull Vector3i end) {

        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();

        Ref<ChunkStore> startBlockRef = getBlockRefFromPosition(world, start);
        Ref<ChunkStore> endBlockRef = getBlockRefFromPosition(world, end);
        if (startBlockRef == null || endBlockRef == null) return false;

        AnchorComponent startAnchorComponent = chunkStore.getComponent(startBlockRef, AnchorComponent.getComponentType());
        AnchorComponent endAnchorComponent = chunkStore.getComponent(endBlockRef, AnchorComponent.getComponentType());
        if (startAnchorComponent == null || endAnchorComponent == null) return false;

        RopeComponent ropeComponent = chunkStore.ensureAndGetComponent(startBlockRef, RopeComponent.getComponentType());

        startAnchorComponent.setPosition(start);
        startAnchorComponent.addAnchor(end);

        endAnchorComponent.setPosition(end);
        endAnchorComponent.addAnchor(start);

        int dx = end.getX() - start.getX();
        int dy = end.getY() - start.getY();
        int dz = end.getZ() - start.getZ();

        world.execute(() -> {
            Set<Vector3i> segmentsForThisPath = new HashSet<>();
            int min, max;

            if (dx != 0) {
                min = Math.min(start.getX(), end.getX());
                max = Math.max(start.getX(), end.getX());
                for (int x = min + 1; x < max; x++) {
                    Vector3i pos = new Vector3i(x, start.getY(), start.getZ());
                    RotationTuple rotation = RotationTuple.of(Rotation.None, Rotation.None, Rotation.Ninety);
                    if (trySpawnRopeSegment(world, chunkStore, segmentsForThisPath, pos, start, end, rotation)) segmentsForThisPath.add(pos);
                }
            } else if (dz != 0) {
                min = Math.min(start.getZ(), end.getZ());
                max = Math.max(start.getZ(), end.getZ());
                for (int z = min + 1; z < max; z++) {
                    Vector3i pos = new Vector3i(start.getX(), start.getY(), z);
                    RotationTuple rotation = RotationTuple.of(Rotation.Ninety, Rotation.None, Rotation.Ninety);
                    if (trySpawnRopeSegment(world, chunkStore, segmentsForThisPath, pos, start, end, rotation)) segmentsForThisPath.add(pos);
                }
            } else if (dy != 0) {
                min = Math.min(start.getY(), end.getY());
                max = Math.max(start.getY(), end.getY());
                for (int y = min + 1; y < max; y++) {
                    Vector3i pos = new Vector3i(start.getX(), y, start.getZ());
                    RotationTuple rotation = RotationTuple.NONE;
                    if (trySpawnRopeSegment(world, chunkStore, segmentsForThisPath, pos, start, end, rotation)) segmentsForThisPath.add(pos);
                }
            }

            if (!segmentsForThisPath.isEmpty()) ropeComponent.addPath(end, segmentsForThisPath);
            else ropeComponent.addPath(end, new HashSet<>());
            logger.atInfo().log("New rope path registered from " + start + " to " + end + " with " + segmentsForThisPath.size() + " segments.");
        });

        return true;
    }

    public static Ref<ChunkStore> getBlockRefFromPosition(World world, Vector3i position) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(position.getX(), position.getZ());
        WorldChunk worldChunk = world.getChunkIfLoaded(chunkIndex);
        if (worldChunk == null || worldChunk.getBlockComponentChunk() == null) return null;

        int blockIndex = ChunkUtil.indexBlockInColumn(ChunkUtil.localCoordinate(position.getX()), position.getY(), ChunkUtil.localCoordinate(position.getZ()));
        Ref<ChunkStore> blockRef = worldChunk.getBlockComponentChunk().getEntityReference(blockIndex);
        if (blockRef == null) return null;

        return blockRef;
    }

    private static boolean trySpawnRopeSegment(World world, Store<ChunkStore> store, Set<Vector3i> currentPath, Vector3i position, Vector3i start, Vector3i end, RotationTuple rotation) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(position.x, position.z);
        WorldChunk worldChunk = world.getChunkIfLoaded(chunkIndex);

        if (worldChunk != null) {
            worldChunk.placeBlock(position.getX(), position.getY(), position.getZ(), "Rope", rotation, 10, false);

            Ref<ChunkStore> spawnedBlockRef = getBlockRefFromPosition(world, position);
            if (spawnedBlockRef != null) {
                RopeSegmentComponent segment = store.ensureAndGetComponent(spawnedBlockRef, RopeSegmentComponent.getComponentType());
                segment.setAnchors(start, end);
                return true;
            }
        }
        return false;
    }
}