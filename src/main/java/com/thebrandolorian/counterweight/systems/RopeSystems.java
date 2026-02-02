package com.thebrandolorian.counterweight.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSystems;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.thebrandolorian.counterweight.components.AnchorComponent;
import com.thebrandolorian.counterweight.components.RopeComponent;
import com.thebrandolorian.counterweight.components.RopeComponent.AnchorNode;

import org.jetbrains.annotations.Nullable;
import javax.annotation.Nonnull;
import java.util.Set;

public class RopeSystems {
    public static class RopeTickingSystem extends EntityTickingSystem<ChunkStore> {
        private final Query<ChunkStore> query;

        public RopeTickingSystem() {
            this.query = AnchorComponent.getComponentType();
        }

        @Override
        public void tick(float dt, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk, @Nonnull Store<ChunkStore> chunkStore, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
            AnchorComponent anchorComponent = archetypeChunk.getComponent(index, AnchorComponent.getComponentType());
            if (anchorComponent == null || anchorComponent.getLinkedRope() == null) return;

            EntityStore entityStore = chunkStore.getExternalData().getWorld().getEntityStore();
            Ref<EntityStore> ropeRef = entityStore.getRefFromUUID(anchorComponent.getLinkedRope());

            if (ropeRef == null || !ropeRef.isValid()) {
                anchorComponent.setLinkedRope(null);
                return;
            }

            RopeComponent ropeComponent = entityStore.getStore().getComponent(ropeRef, RopeComponent.getComponentType());
            if (ropeComponent == null) return;

            // TODO: rope functionality
        }

        @Override
        public @Nullable Query<ChunkStore> getQuery() { return this.query; }
    }

    public static class CheckRopeNodesValidSystem extends EntityTickingSystem<EntityStore> {
        private final Query<EntityStore> query;
        private final Set<Dependency<EntityStore>> dependencies;

        public CheckRopeNodesValidSystem() {
            this.query = RopeComponent.getComponentType();
            this.dependencies = Set.of(new SystemDependency<>(Order.AFTER, PlayerSystems.ProcessPlayerInput.class));
        }

        @Override
        public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            RopeComponent ropeComponent = archetypeChunk.getComponent(index, RopeComponent.getComponentType());
            assert ropeComponent != null;

            World world = store.getExternalData().getWorld();
            Store<ChunkStore> chunkStore = world.getChunkStore().getStore();

            for (AnchorNode node : ropeComponent.getAnchorNodes()) {
                Vector3i blockPosition = node.getBlockPosition();

                long chunkIndex = ChunkUtil.indexChunkFromBlock(blockPosition.getX(), blockPosition.getZ());
                WorldChunk worldChunk = world.getChunkIfLoaded(chunkIndex);
                if (worldChunk == null || worldChunk.getBlockComponentChunk() == null) continue;

                int blockIndex = ChunkUtil.indexBlockInColumn(ChunkUtil.localCoordinate(blockPosition.getX()), blockPosition.getY(), ChunkUtil.localCoordinate(blockPosition.getZ()));
                Ref<ChunkStore> blockRef = worldChunk.getBlockComponentChunk().getEntityReference(blockIndex);
                if (blockRef == null) {
                    commandBuffer.removeEntity(archetypeChunk.getReferenceTo(index), RemoveReason.REMOVE);
                    return;
                }

                AnchorComponent anchor = chunkStore.getComponent(blockRef, AnchorComponent.getComponentType());
                if (anchor == null || anchor.getLinkedRope() == null) {
                    commandBuffer.removeEntity(archetypeChunk.getReferenceTo(index), RemoveReason.REMOVE);
                    return;
                }
            }
        }

        @Override
        public @Nullable Query<EntityStore> getQuery() { return this.query; }

        @Override
        public @Nonnull Set<Dependency<EntityStore>> getDependencies() { return this.dependencies; }
    }
}
