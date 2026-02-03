package com.thebrandolorian.counterweight.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.thebrandolorian.counterweight.CounterweightPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RopeComponent implements Component<ChunkStore> {
    public static final BuilderCodec<RopeComponent> CODEC =
            BuilderCodec.builder(RopeComponent.class, RopeComponent::new)
                    .append(new KeyedCodec<>("TotalLength", Codec.FLOAT),
                            (c, v) -> c.totalLength = v,
                            c -> c.totalLength).add()

                    .append(new KeyedCodec<>("DeployedLength", Codec.FLOAT),
                            (c, v) -> c.deployedLength = v,
                            c -> c.deployedLength).add()

                    .append(new KeyedCodec<>("Nodes", new ArrayCodec<>(AnchorNode.CODEC, AnchorNode[]::new)),
                            (c, v) -> c.anchorNodes = new ArrayList<>(Arrays.asList(v)),
                            c -> c.anchorNodes.toArray(new AnchorNode[0])).add()

                    .append(new KeyedCodec<>("Segments", new ArrayCodec<>(Vector3i.CODEC, Vector3i[]::new)),
                            (c, v) -> c.segmentPositions = new ArrayList<>(Arrays.asList(v)),
                            c -> c.segmentPositions.toArray(new Vector3i[0])).add()

                    .build();

    public static ComponentType<ChunkStore, RopeComponent> getComponentType() { return CounterweightPlugin.get().getRopeComponentType(); }

    public RopeComponent() {}

    private float totalLength = 1f;
    private float deployedLength = 0f;
    private List<AnchorNode> anchorNodes = new ArrayList<>();
    private List<Vector3i> segmentPositions = new ArrayList<>();

    public void addNode(AnchorNode node) { this.anchorNodes.add(node); }
    public void addSegment(Vector3i segment) { this.segmentPositions.add(segment); }
    public float getRemainingLength() { return totalLength - deployedLength; }

    // Getters & Setters
    public float getTotalLength() { return totalLength; }
    public void setTotalLength(float totalLength) { this.totalLength = totalLength; }

    public float getDeployedLength() { return deployedLength; }
    public void setDeployedLength(float len) { this.deployedLength = Math.max(0f, Math.min(totalLength, len)); }

    public List<AnchorNode> getAnchorNodes() { return anchorNodes; }
    public List<Vector3i> getSegmentPositions() { return segmentPositions; }

    @Override
    public @Nullable RopeComponent clone() {
        RopeComponent rope = new RopeComponent();
        rope.totalLength = this.totalLength;
        rope.deployedLength = this.deployedLength;
        for (AnchorNode node : this.anchorNodes) rope.addNode(node.clone());
        for (Vector3i segment : this.segmentPositions) rope.addSegment(new Vector3i(segment.x, segment.y, segment.z));
        return rope;
    }

    public static class AnchorNode {
        public enum AnchorType { BLOCK }
        private static final Codec<AnchorType> ANCHOR_TYPE_CODEC = new EnumCodec<>(AnchorType.class);

        public static final BuilderCodec<AnchorNode> CODEC = BuilderCodec.builder(AnchorNode.class, AnchorNode::new)
                .append(new KeyedCodec<>("AnchorPosition", Vector3i.CODEC),
                        (n, v) -> n.anchorPosition = v,
                        n -> n.anchorPosition).add()

                .build();

        private Vector3i anchorPosition = null;

        public AnchorNode() {}

        public boolean isValid() { return anchorPosition != null; }

        public static AnchorNode of(@Nonnull Vector3i anchorPosition) {
            AnchorNode node = new AnchorNode();
            node.anchorPosition = anchorPosition;
            return node;
        }

        @Nonnull public Vector3i getAnchorPosition() { return anchorPosition; }

        @Override
        public AnchorNode clone() {
            AnchorNode clone = new AnchorNode();
            if (this.anchorPosition != null) clone.anchorPosition = new Vector3i(this.anchorPosition);
            return clone;
        }
    }
}
