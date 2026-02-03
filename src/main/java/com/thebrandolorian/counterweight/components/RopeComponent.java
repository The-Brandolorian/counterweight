package com.thebrandolorian.counterweight.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.thebrandolorian.counterweight.CounterweightPlugin;

import javax.annotation.Nullable;
import java.util.*;

public class RopeComponent implements Component<ChunkStore> {
    public static final BuilderCodec<RopeComponent> CODEC =
            BuilderCodec.builder(RopeComponent.class, RopeComponent::new)
                    .append(new KeyedCodec<>("Paths", new ArrayCodec<>(RopePath.CODEC, RopePath[]::new)),
                            (c, v) -> c.paths = new ArrayList<>(Arrays.asList(v)),
                            c -> c.paths.toArray(new RopePath[0])).add()
                    .build();

    public static ComponentType<ChunkStore, RopeComponent> getComponentType() { return CounterweightPlugin.get().getRopeComponentType(); }

    private List<RopePath> paths = new ArrayList<>();

    public RopeComponent() {}

    public void addPath(Vector3i targetAnchor, Set<Vector3i> segments) {
        this.paths.removeIf(p -> p.getTarget().equals(targetAnchor));
        this.paths.add(new RopePath(targetAnchor, segments));
    }

    public List<RopePath> getPaths() { return paths; }

    @Override
    public @Nullable RopeComponent clone() {
        RopeComponent clone = new RopeComponent();
        for (RopePath path : this.paths) clone.paths.add(path.clone());
        return clone;
    }

    public static class RopePath {
        public static final BuilderCodec<RopePath> CODEC = BuilderCodec.builder(RopePath.class, RopePath::new)
                .append(new KeyedCodec<>("Target", Vector3i.CODEC),
                        (p, v) -> p.target = v,
                        p -> p.target).add()
                .append(new KeyedCodec<>("Segments", new ArrayCodec<>(Vector3i.CODEC, Vector3i[]::new)),
                        (p, v) -> p.segments = new HashSet<>(Arrays.asList(v)),
                        p -> p.segments.toArray(new Vector3i[0])).add()
                .build();

        private Vector3i target;
        private Set<Vector3i> segments = new HashSet<>();

        public RopePath() {}

        public RopePath(Vector3i target, Set<Vector3i> segments) {
            this.target = target;
            this.segments = segments;
        }

        public Vector3i getTarget() { return target; }
        public Set<Vector3i> getSegments() { return segments; }

        public RopePath clone() {
            Vector3i targetClone = this.target != null ? new Vector3i(this.target) : null;
            return new RopePath(targetClone, new HashSet<>(this.segments));
        }
    }
}
