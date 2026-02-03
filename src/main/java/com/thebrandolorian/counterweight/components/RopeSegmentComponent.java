package com.thebrandolorian.counterweight.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.thebrandolorian.counterweight.CounterweightPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RopeSegmentComponent implements Component<ChunkStore> {
    public static final BuilderCodec<RopeSegmentComponent> CODEC = BuilderCodec.builder(RopeSegmentComponent.class, RopeSegmentComponent::new)

            .append(new KeyedCodec<>("StartAnchor", Vector3i.CODEC),
                    (c, v) -> c.startAnchor = v,
                    c -> c.startAnchor).add()

            .append(new KeyedCodec<>("EndAnchor", Vector3i.CODEC),
                    (c, v) -> c.endAnchor = v,
                    c -> c.endAnchor).add()

            .build();

    public static ComponentType<ChunkStore, RopeSegmentComponent> getComponentType() { return CounterweightPlugin.get().getRopeSegmentComponentType(); }

    private Vector3i startAnchor = null;
    private Vector3i endAnchor = null;

    public RopeSegmentComponent() {}

    public void setAnchors(@Nonnull Vector3i start, @Nonnull Vector3i end) {
        this.startAnchor = start;
        this.endAnchor = end;
    }

    // Getters and Setters
    @Nullable public Vector3i getStartAnchor() { return startAnchor; }
    @Nullable public Vector3i getEndAnchor() { return endAnchor; }

    @Override
    public @Nullable RopeSegmentComponent clone() {
        RopeSegmentComponent clone = new RopeSegmentComponent();
        if (this.startAnchor != null) clone.startAnchor = new Vector3i(this.startAnchor.getX(), this.startAnchor.getY(), this.startAnchor.getZ());
        if (this.endAnchor != null) clone.endAnchor = new Vector3i(this.endAnchor.getX(), this.endAnchor.getY(), this.endAnchor.getZ());
        return clone;
    }
}
