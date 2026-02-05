package com.thebrandolorian.counterweight.utils;

import com.hypixel.hytale.math.iterator.BlockIterator;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import it.unimi.dsi.fastutil.ints.IntSet;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class InteractionUtils {
	private InteractionUtils() { }
	
	public static @Nullable Vector3i getFirstBlockWithTagOrNull(World world, Vector3d fromPosition, Vector3d toPosition, int[] allowedTagIndices) {
		AtomicReference<Vector3i> foundPosition = new AtomicReference<>();
		
		BlockIterator.iterateFromTo(fromPosition, toPosition, (x, y, z, px, py, pz, qx, qy, qz) -> {
			int blockId = world.getBlock(x, y, z);
			BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
			
			if (blockType != null && blockType.getData() != null) {
				IntSet blockTags = blockType.getData().getExpandedTagIndexes();
				
				for (int tagIndex : blockTags) {
					if (Arrays.binarySearch(allowedTagIndices, tagIndex) >= 0) {
						foundPosition.set(new Vector3i(x, y, z));
						return false;
					}
				}
			}
			return true;
		});
		
		return foundPosition.get();
	}
}
