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
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
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

            // TODO: rope functionality
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
                List<Vector3i> blockPositions = new ArrayList<>(ropeComponent.getSegmentPositions());
                clearBlocks(world, blockPositions);
                CounterweightPlugin.get().getLogger().atInfo().log("Rope collapsed. Clearing " + blockPositions.size() + " segments.");
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

            RopeComponent ropeComponent = store.getComponent(ref, RopeComponent.getComponentType());
            if (ropeComponent != null) {
                World world = store.getExternalData().getWorld();
                world.execute(() -> {
                    List<Vector3i> blockPositions = new ArrayList<>(ropeComponent.getSegmentPositions());
                    clearBlocks(world, blockPositions);
                    CounterweightPlugin.get().getLogger().atInfo().log("Anchor broken. Clearing " + ropeComponent.getSegmentPositions().size() + " segments.");
                });
            }

            Set<Vector3i> linkedPositions = anchorComponent.getLinkedAnchorPositions();
            if (linkedPositions != null && !linkedPositions.isEmpty()) {
                Vector3i brokenAnchorPosition = anchorComponent.getPosition();
                if (brokenAnchorPosition == null) return;

                for (Vector3i linkedPosition : linkedPositions) {
                    Ref<ChunkStore> linkedAnchorRef = getBlockRefFromPosition(store.getExternalData().getWorld(), linkedPosition);
                    if (linkedAnchorRef == null || !linkedAnchorRef.isValid()) continue;

                    RopeComponent linkedRopeComponent = store.getComponent(linkedAnchorRef, RopeComponent.getComponentType());
                    if (linkedRopeComponent != null && linkedRopeComponent.getAnchorNodes().stream().anyMatch(node -> node.getAnchorPosition().equals(brokenAnchorPosition))) {
                        commandBuffer.removeComponent(linkedAnchorRef, RopeComponent.getComponentType());
                        CounterweightPlugin.get().getLogger().atInfo().log("Partner Anchor broken. Signaling owner to clear rope.");
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

            Vector3i anchorPosition = ropeSegmentComponent.getStartAnchor();
            if (anchorPosition == null) return;

            World world = store.getExternalData().getWorld();
            Ref<ChunkStore> anchorRef = getBlockRefFromPosition(world, anchorPosition);

            if (anchorRef != null && anchorRef.isValid()) {
                RopeComponent ropeComponent = store.getComponent(anchorRef, RopeComponent.getComponentType());
                if (ropeComponent != null) {
                    commandBuffer.removeComponent(anchorRef, RopeComponent.getComponentType());
                    CounterweightPlugin.get().getLogger().atInfo().log("Rope segment broken. Triggered full collapse.");
                }
            }
        }

        @Override
        public Query<ChunkStore> getQuery() { return RopeSegmentComponent.getComponentType(); }
    }

    private static void clearBlocks(World world, List<Vector3i> blockPositions) {
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
