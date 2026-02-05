package com.thebrandolorian.counterweight.utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import java.util.List;

public class BlockUtils {
    public static Ref<ChunkStore> getBlockRefFromPosition(@Nonnull World world, @Nonnull Vector3i position) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(position.getX(), position.getZ());
        WorldChunk worldChunk = world.getChunkIfLoaded(chunkIndex);
        if (worldChunk == null) { DebugUtils.logWarn("Chunk not loaded for position {}", position); return null; }

        var blockComponentChunk = worldChunk.getBlockComponentChunk();
        if (blockComponentChunk == null) { DebugUtils.logWarn("Block component chunk missing for position {}", position); return null; }

        int blockIndex = ChunkUtil.indexBlockInColumn(ChunkUtil.localCoordinate(position.getX()), position.getY(), ChunkUtil.localCoordinate(position.getZ()));
        return blockComponentChunk.getEntityReference(blockIndex);
    }
    
    public static void breakBlocksAtPositions(@Nonnull World world, @Nonnull List<Vector3i> blockPositions) {
        if (blockPositions.isEmpty()) { DebugUtils.logWarn("Cannot break blocks, list is empty"); return; }

        for (Vector3i position : blockPositions) {
			BlockUtils.breakBlockAtPosition(world, position);
        }
    }

    public static void breakBlockAtPosition(@Nonnull World world, @Nonnull Vector3i blockPosition) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockPosition.x, blockPosition.z);
        WorldChunk worldChunk = world.getChunkIfLoaded(chunkIndex);
        if (worldChunk == null) { DebugUtils.logWarn("Cannot break block at {} - chunk not loaded", blockPosition); return; }

        Ref<ChunkStore> segmentChunkRef = world.getChunkStore().getStore().getExternalData().getChunkReference(chunkIndex);
        if (segmentChunkRef == null || !segmentChunkRef.isValid()) { DebugUtils.logWarn("Cannot break block at {} - invalid chunk reference", blockPosition); return; }

        BlockHarvestUtils.performBlockBreak(
                null,
                null,
                blockPosition,
                1024,
                segmentChunkRef,
                world.getEntityStore().getStore(),
                world.getChunkStore().getStore()
        );
    }
}
