package com.thebrandolorian.counterweight.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.thebrandolorian.counterweight.CounterweightPlugin;
import com.thebrandolorian.counterweight.components.AnchorComponent;
import com.thebrandolorian.counterweight.components.RopeComponent;

import com.thebrandolorian.counterweight.components.RopeSegmentComponent;
import org.jetbrains.annotations.Nullable;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.thebrandolorian.counterweight.managers.RopeManager.getBlockRefFromPosition;

public class RopeSystems {
    public static class RopeTickingSystem extends EntityTickingSystem<ChunkStore> {
        private final Query<ChunkStore> query;

        public RopeTickingSystem() {
            this.query = AnchorComponent.getComponentType();
        }

        @Override
        public void tick(float dt, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk, @Nonnull Store<ChunkStore> chunkStore, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
            AnchorComponent anchorComponent = archetypeChunk.getComponent(index, AnchorComponent.getComponentType());
            if (anchorComponent == null) return;

            Set<Vector3i> linkedPositions = anchorComponent.getLinkedAnchorPositions();
            if (linkedPositions == null || linkedPositions.isEmpty()) return;

            World world = chunkStore.getExternalData().getWorld();

            List<Vector3i> anchorsToRemove = new ArrayList<>();
            for (Vector3i position : linkedPositions) {
                Ref<ChunkStore> linkedRef = getBlockRefFromPosition(world, position);
                if (linkedRef == null || chunkStore.getComponent(linkedRef, AnchorComponent.getComponentType()) == null) {
                    anchorsToRemove.add(position);
                }
            }
            anchorsToRemove.forEach(anchorComponent::removeAnchor);

            // TODO: rope functionality e.g moving etc
        }

        @Override
        public @Nullable Query<ChunkStore> getQuery() { return this.query; }
    }

    public static class RopeListener extends RefChangeSystem<ChunkStore, RopeComponent> {
        @Override
        public void onComponentAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull RopeComponent ropeComponent, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        }

        @Override
        public void onComponentSet(@Nonnull Ref<ChunkStore> ref, @Nullable RopeComponent ropeComponent, @Nonnull RopeComponent ropeComponent2, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        }

        @Override
        public void onComponentRemoved(@Nonnull Ref<ChunkStore> ref, @Nonnull RopeComponent ropeComponent, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
            World world = store.getExternalData().getWorld();
            world.execute(() -> {
                List<Vector3i> allSegments = new ArrayList<>();
                for (RopeComponent.RopePath path : ropeComponent.getPaths()) {
                    if (path.getSegments() != null) {
                        allSegments.addAll(path.getSegments());
                    }
                }

                if (!allSegments.isEmpty()) {
                    clearBlocks(world, allSegments);
                    CounterweightPlugin.get().getLogger().atInfo().log("Rope source removed. Clearing " + allSegments.size() + " segments.");
                }
            });
        }

        @Override
        public @Nonnull Query<ChunkStore> getQuery() { return RopeComponent.getComponentType(); }

        @Override
        public @Nonnull ComponentType<ChunkStore, RopeComponent> componentType() { return RopeComponent.getComponentType(); }
    }

    public static class AnchorListener extends RefSystem<ChunkStore> {
        @Override
        public void onEntityAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull AddReason reason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {

        }

        @Override
        public void onEntityRemove(@Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
            if (reason == RemoveReason.UNLOAD) return;

            AnchorComponent anchorComponent = store.getComponent(ref, AnchorComponent.getComponentType());
            if (anchorComponent == null) return;

            World world = store.getExternalData().getWorld();
            RopeComponent ropeComponent = store.getComponent(ref, RopeComponent.getComponentType());
            if (ropeComponent != null) {
                world.execute(() -> {
                    List<Vector3i> allMySegments = new ArrayList<>();
                    for (RopeComponent.RopePath path : ropeComponent.getPaths()) allMySegments.addAll(path.getSegments());
                    clearBlocks(world, allMySegments);
                    CounterweightPlugin.get().getLogger().atInfo().log("Anchor broken. Clearing source ropes: " + allMySegments.size() + " segments.");
                });
            }

            Set<Vector3i> linkedPositions = anchorComponent.getLinkedAnchorPositions();
            Vector3i brokenAnchorPosition = anchorComponent.getPosition();
            if (linkedPositions != null && brokenAnchorPosition != null) {
                for (Vector3i partnerPos : linkedPositions) {
                    Ref<ChunkStore> partnerRef = getBlockRefFromPosition(world, partnerPos);
                    if (partnerRef == null || !partnerRef.isValid()) continue;

                    RopeComponent partnerRope = store.getComponent(partnerRef, RopeComponent.getComponentType());
                    if (partnerRope != null) {
                        partnerRope.getPaths().removeIf(path -> {
                            if (path.getTarget().equals(brokenAnchorPosition)) {
                                world.execute(() -> clearBlocks(world, new ArrayList<>(path.getSegments())));
                                return true;
                            }
                            return false;
                        });

                        if (partnerRope.getPaths().isEmpty()) {
                            commandBuffer.removeComponent(partnerRef, RopeComponent.getComponentType());
                        }
                    }
                }
            }
        }

        @Override
        public Query<ChunkStore> getQuery() { return AnchorComponent.getComponentType(); }
    }

    public static class RopeSegmentListener extends RefSystem<ChunkStore> {
        @Override
        public void onEntityAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull AddReason reason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {

        }

        @Override
        public void onEntityRemove(@Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
            if (reason == RemoveReason.UNLOAD) return;

            RopeSegmentComponent ropeSegmentComponent = store.getComponent(ref, RopeSegmentComponent.getComponentType());
            if (ropeSegmentComponent == null) return;

            Vector3i startAnchorPosition = ropeSegmentComponent.getStartAnchor();
            Vector3i endAnchorPosition = ropeSegmentComponent.getEndAnchor();
            if (startAnchorPosition == null || endAnchorPosition == null) return;

            World world = store.getExternalData().getWorld();
            Ref<ChunkStore> anchorRef = getBlockRefFromPosition(world, startAnchorPosition);

            if (anchorRef != null && anchorRef.isValid()) {
                RopeComponent ropeComponent = store.getComponent(anchorRef, RopeComponent.getComponentType());
                if (ropeComponent != null) {
                    ropeComponent.getPaths().removeIf(path -> {
                        if (path.getTarget().equals(endAnchorPosition)) {
                            world.execute(() -> clearBlocks(world, new ArrayList<>(path.getSegments())));
                            return true;
                        }
                        return false;
                    });

                    if (ropeComponent.getPaths().isEmpty()) commandBuffer.removeComponent(anchorRef, RopeComponent.getComponentType());
                }
            }
        }

        @Override
        public Query<ChunkStore> getQuery() { return RopeSegmentComponent.getComponentType(); }
    }

    private static void clearBlocks(World world, List<Vector3i> blockPositions) {
        if (blockPositions == null || blockPositions.isEmpty()) return;

        for (Vector3i position : blockPositions) {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(position.x, position.z);
            Ref<ChunkStore> segmentChunkRef = world.getChunkStore().getStore().getExternalData().getChunkReference(chunkIndex);

            if (segmentChunkRef != null && segmentChunkRef.isValid()) {
                BlockHarvestUtils.performBlockBreak(
                        null,
                        null,
                        position,
                        1024,
                        segmentChunkRef,
                        world.getEntityStore().getStore(),
                        world.getChunkStore().getStore()
                );
            }
        }
    }
}
