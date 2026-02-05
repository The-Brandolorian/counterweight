package com.thebrandolorian.counterweight.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.thebrandolorian.counterweight.CounterweightPlugin;
import lombok.Getter;

import java.util.*;

public class RopeComponent implements Component<ChunkStore> {
    public static final BuilderCodec<RopeComponent> CODEC =
            BuilderCodec.builder(RopeComponent.class, RopeComponent::new)
                    .append(new KeyedCodec<>("Paths", new ArrayCodec<>(RopePath.CODEC, RopePath[]::new)),
                            (c, v) -> c.paths = new ArrayList<>(Arrays.asList(v)),
                            c -> c.paths.toArray(new RopePath[0])).add()
                    .build();

    public static ComponentType<ChunkStore, RopeComponent> getComponentType() { return CounterweightPlugin.get().getRopeComponentType(); }
    
    public RopeComponent() {}
    
    @Getter private List<RopePath> paths = new ArrayList<>();

    public void addPath(Vector3i targetAnchor, Set<Vector3i> segments) {
        for (RopePath path : paths) {
            if (path.getTarget().equals(targetAnchor)) return;
        }
        paths.add(new RopePath(targetAnchor, segments));
    }

    @Override
    public RopeComponent clone() {
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
        
        @Getter private Vector3i target;
        @Getter private Set<Vector3i> segments = new HashSet<>();

        public RopePath() {}

        public RopePath(Vector3i target, Set<Vector3i> segments) {
            this.target = target;
            this.segments = segments;
        }
        
        @Override
        public RopePath clone() {
            Vector3i targetClone = this.target != null ? new Vector3i(this.target) : null;
            return new RopePath(targetClone, new HashSet<>(this.segments));
        }
    }
}
