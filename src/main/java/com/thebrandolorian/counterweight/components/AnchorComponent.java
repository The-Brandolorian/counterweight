package com.thebrandolorian.counterweight.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.thebrandolorian.counterweight.CounterweightPlugin;

import javax.annotation.Nonnull;
import java.util.UUID;

public class AnchorComponent implements Component<ChunkStore> {
    private UUID linkedRope;

    public void setLinkedRope(UUID id) { this.linkedRope = id; }
    public UUID getLinkedRope() { return this.linkedRope; }

    public static ComponentType<ChunkStore, AnchorComponent> getComponentType() {
        return CounterweightPlugin.get().getAnchorComponentType();
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() { return new AnchorComponent(); }
}
