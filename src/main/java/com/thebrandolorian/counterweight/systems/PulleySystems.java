package com.thebrandolorian.counterweight.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSystems;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.thebrandolorian.counterweight.components.RopeComponent;
import com.thebrandolorian.counterweight.components.SpoolComponent;

import org.jetbrains.annotations.Nullable;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PulleySystems {
    public static class PulleySystem extends EntityTickingSystem<EntityStore> {
        private final Query<EntityStore> query;
        private final Set<Dependency<EntityStore>> dependencies;

        public PulleySystem() {
            this.query = SpoolComponent.getComponentType();
            this.dependencies = Set.of(new SystemDependency<>(Order.AFTER, CheckRopeNodesSystem.class), new SystemGroupDependency<>(Order.BEFORE, EntityTrackerSystems.QUEUE_UPDATE_GROUP));
        }

        @Override
        public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            SpoolComponent spool = archetypeChunk.getComponent(index, SpoolComponent.getComponentType());
            if (spool == null || spool.getLinkedRope() == null) return;

            EntityStore entityStore = store.getExternalData();
            Ref<EntityStore> ropeRef = entityStore.getRefFromUUID(spool.getLinkedRope());

            if (ropeRef == null || !ropeRef.isValid()) {
                spool.setLinkedRope(null);
                return;
            }

            RopeComponent ropeComponent = commandBuffer.getComponent(ropeRef, RopeComponent.getComponentType());
            if (ropeComponent == null) return;

            // TODO: pulley functionality
        }

        @Override
        public @Nullable Query<EntityStore> getQuery() { return this.query; }

        @Override
        public @Nonnull Set<Dependency<EntityStore>> getDependencies() { return this.dependencies; }
    }

    public static class CheckRopeNodesSystem extends EntityTickingSystem<EntityStore> {
        private final Query<EntityStore> query;
        private final Set<Dependency<EntityStore>> dependencies;

        public CheckRopeNodesSystem() {
            this.query = RopeComponent.getComponentType();
            this.dependencies = Set.of(new SystemDependency<>(Order.AFTER, PlayerSystems.ProcessPlayerInput.class));
        }

        @Override
        public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            EntityStore entityStore = store.getExternalData();

            RopeComponent ropeComponent = archetypeChunk.getComponent(index, RopeComponent.getComponentType());
            assert ropeComponent != null;

            List<Vector3d> positions = new ArrayList<>();

            boolean nodesValid = true;
            for (UUID nodeUuid : ropeComponent.getNodes()) {
                Ref<EntityStore> nodeRef = entityStore.getRefFromUUID(nodeUuid);

                if (nodeRef != null && nodeRef.isValid()) {
                    TransformComponent transform = commandBuffer.getComponent(nodeRef, TransformComponent.getComponentType());
                    if (transform != null) {
                        positions.add(new Vector3d(transform.getPosition()));
                        continue;
                    }
                }

                nodesValid = false;
                break;
            }

            if (!nodesValid) commandBuffer.removeEntity(archetypeChunk.getReferenceTo(index), RemoveReason.REMOVE);
            else ropeComponent.setCachedNodePositions(positions);
        }

        @Override
        public @Nullable Query<EntityStore> getQuery() { return this.query; }

        @Override
        public @Nonnull Set<Dependency<EntityStore>> getDependencies() { return this.dependencies; }
    }
}
