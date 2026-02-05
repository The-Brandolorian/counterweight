package com.thebrandolorian.counterweight.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.thebrandolorian.counterweight.components.AnchorComponent;
import com.thebrandolorian.counterweight.components.RopeComponent;

import com.thebrandolorian.counterweight.components.RopeSegmentComponent;
import com.thebrandolorian.counterweight.utils.BlockUtils;
import com.thebrandolorian.counterweight.utils.RopeUtils;
import org.jetbrains.annotations.Nullable;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class RopeSystems {
    public static class RopeTickingSystem extends EntityTickingSystem<ChunkStore> {
        private final Query<ChunkStore> query;

        public RopeTickingSystem() {
            this.query = AnchorComponent.getComponentType();
        }

        @Override
        public void tick(float dt, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk, @Nonnull Store<ChunkStore> chunkStore, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
//            AnchorComponent anchorComponent = archetypeChunk.getComponent(index, AnchorComponent.getComponentType());
//            if (anchorComponent == null) return;
//
//            Set<Vector3i> linkedPositions = anchorComponent.getLinkedAnchorPositions();
//            if (linkedPositions == null || linkedPositions.isEmpty()) return;
//
//            World world = chunkStore.getExternalData().getWorld();
//
//            List<Vector3i> anchorsToRemove = new ArrayList<>();
//            for (Vector3i position : linkedPositions) {
//                Ref<ChunkStore> linkedRef = getBlockRefFromPosition(world, position);
//                if (linkedRef == null || chunkStore.getComponent(linkedRef, AnchorComponent.getComponentType()) == null) {
//                    anchorsToRemove.add(position);
//                }
//            }
//            anchorsToRemove.forEach(anchorComponent::removeAnchor);

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
            RopeUtils.removeRopeSegments(world, ropeComponent);
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
            if (ropeComponent != null) RopeUtils.removeRopeSegments(world, ropeComponent);
            RopeUtils.removePartnerRopes(world, store, commandBuffer, anchorComponent);
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
            Ref<ChunkStore> anchorRef = BlockUtils.getBlockRefFromPosition(world, startAnchorPosition);
            if (anchorRef == null || !anchorRef.isValid()) return;

            RopeComponent ropeComponent = store.getComponent(anchorRef, RopeComponent.getComponentType());
            if (ropeComponent == null) return;

            boolean pathsRemoved = RopeUtils.removePathsToAnchor(world, ropeComponent, endAnchorPosition);
            if (pathsRemoved) {
                RopeUtils.removeRopeComponentIfEmpty(commandBuffer, anchorRef, ropeComponent);
                RopeUtils.cleanupAnchorConnections(world, store, startAnchorPosition, endAnchorPosition);
            }
        }

        @Override
        public Query<ChunkStore> getQuery() { return RopeSegmentComponent.getComponentType(); }
    }
}
