package com.thebrandolorian.counterweight.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.thebrandolorian.counterweight.CounterweightPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class AnchorComponent implements Component<ChunkStore> {
    public static final BuilderCodec<AnchorComponent> CODEC = BuilderCodec.builder(AnchorComponent.class, AnchorComponent::new)
            .append(new KeyedCodec<>("Position", Vector3i.CODEC),
                    (c, v) -> c.position = v,
                    c -> c.position).add()

            .append(new KeyedCodec<>("Links", new SetCodec<>(Vector3i.CODEC, HashSet::new, false)),
                    (c, v) -> c.linkedAnchorPositions = v,
                    c -> c.linkedAnchorPositions).add()

            .build();

    public static ComponentType<ChunkStore, AnchorComponent> getComponentType() { return CounterweightPlugin.get().getAnchorComponentType(); }

    private Vector3i position = null;
    private Set<Vector3i> linkedAnchorPositions = new HashSet<>();

    public void setPosition(Vector3i pos) { this.position = pos; }
    public @Nullable Vector3i getPosition() { return this.position; }

    public void addAnchor(Vector3i pos) { this.linkedAnchorPositions.add(pos); }
    public void removeAnchor(Vector3i pos) { this.linkedAnchorPositions.remove(pos); }
    public @Nullable Set<Vector3i> getLinkedAnchorPositions() { return this.linkedAnchorPositions; }

    @Override
    public @Nonnull AnchorComponent clone() {
        AnchorComponent clone = new AnchorComponent();
        clone.position = this.position;
        clone.linkedAnchorPositions = new HashSet<>(this.linkedAnchorPositions);
        return clone;
    }
}
