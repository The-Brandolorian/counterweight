package com.thebrandolorian.counterweight.systems;

import com.hypixel.hytale.builtin.mounts.NPCMountComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.systems.RoleBuilderSystem;
import com.thebrandolorian.counterweight.components.RopeComponent;
import com.thebrandolorian.counterweight.components.SpoolComponent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Set;

public class PulleySystems {
    public class PulleySystem extends EntityTickingSystem<EntityStore> {
        private final Query<EntityStore> query;
        private final Set<Dependency<EntityStore>> dependencies;

        public PulleySystem() {
            this.query = SpoolComponent.getComponentType();
            this.dependencies = Set.of(new SystemDependency<>(Order.AFTER, RoleBuilderSystem.class));
        }

        @Override
        public void tick(float dt, int index, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
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

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() { return this.query; }


    }
}
