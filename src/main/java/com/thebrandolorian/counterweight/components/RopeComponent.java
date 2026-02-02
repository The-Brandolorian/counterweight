package com.thebrandolorian.counterweight.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.thebrandolorian.counterweight.CounterweightPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RopeComponent implements Component<EntityStore> {
    public static final BuilderCodec<RopeComponent> CODEC =
            BuilderCodec.builder(RopeComponent.class, RopeComponent::new)
                    .append(new KeyedCodec<>("TotalLength", Codec.FLOAT), (c, v) -> c.totalLength = v, c -> c.totalLength).add()
                    .append(new KeyedCodec<>("DeployedLength", Codec.FLOAT), (c, v) -> c.deployedLength = v, c -> c.deployedLength).add()
                    .append(new KeyedCodec<>("Nodes", new ArrayCodec<>(AnchorNode.CODEC, AnchorNode[]::new)),
                            (c, v) -> c.anchorNodes = new ArrayList<>(Arrays.asList(v)),
                            c -> c.anchorNodes.toArray(new AnchorNode[0])).add()
                    .build();

    public static ComponentType<EntityStore, RopeComponent> getComponentType() { return CounterweightPlugin.get().getRopeComponentType(); }

    public RopeComponent() {}
    public RopeComponent(@Nonnull Vector3d position) {}

    private float totalLength = 1f;
    private float deployedLength = 0f;
    private List<AnchorNode> anchorNodes = new ArrayList<>();

    public void addNode(AnchorNode node) { this.anchorNodes.add(node); }
    public float getRemainingLength() { return totalLength - deployedLength; }

    // Getters & Setters
    public float getTotalLength() { return totalLength; }
    public void setTotalLength(float totalLength) { this.totalLength = totalLength; }

    public float getDeployedLength() { return deployedLength; }
    public void setDeployedLength(float len) { this.deployedLength = Math.max(0f, Math.min(totalLength, len)); }

    public List<AnchorNode> getAnchorNodes() { return anchorNodes; }
    public void setAnchorNodes(List<AnchorNode> anchorNodes) { this.anchorNodes = new ArrayList<>(anchorNodes); }

    @Override
    public @Nullable RopeComponent clone() {
        RopeComponent rope = new RopeComponent();
        rope.totalLength = this.totalLength;
        rope.deployedLength = this.deployedLength;
        for (AnchorNode node : this.anchorNodes) rope.addNode(node.clone());
        return rope;
    }

    public static class AnchorNode {
        public enum AnchorType { BLOCK }
        private static final Codec<AnchorType> ANCHOR_TYPE_CODEC = new EnumCodec<>(AnchorType.class);

        public static final BuilderCodec<AnchorNode> CODEC = BuilderCodec.builder(AnchorNode.class, AnchorNode::new)
                .append(new KeyedCodec<>("anchorPosition", Vector3d.CODEC), (n, v) -> n.anchorPosition = v, n -> n.anchorPosition).add()
                .append(new KeyedCodec<>("blockPosition", Vector3i.CODEC), (n, v) -> n.blockPosition = v, n -> n.blockPosition).add()
                .afterDecode((node, extraInfo) -> {
                    if (!node.isValid()) {
                        if (node.blockPosition == null && node.anchorPosition != null) {
                            node.blockPosition = new Vector3i((int)node.anchorPosition.getX(), (int)node.anchorPosition.getY(), (int)node.anchorPosition.getZ());
                        }

                        if (node.anchorPosition == null) {
                            node.anchorPosition = Vector3d.ZERO;
                            CounterweightPlugin.get().getLogger().atSevere().log("RopeNode " + node.toString() + " does not have a valid anchor");
                        }
                    }
                })
                .build();

        private Vector3d anchorPosition = Vector3d.ZERO;
        private Vector3i blockPosition = Vector3i.ZERO;

        public AnchorNode() {}

        public boolean isValid() {
            return anchorPosition != null;
        }

        public static AnchorNode block(@Nonnull Vector3d anchorPosition, @Nonnull Vector3i blockPosition) {
            AnchorNode node = new AnchorNode();
            node.anchorPosition = anchorPosition;
            node.blockPosition = blockPosition;
            return node;
        }

        @Nonnull public Vector3d getAnchorPosition() { return anchorPosition; }
        @Nonnull public Vector3i getBlockPosition() { return blockPosition; }

        @Override
        public AnchorNode clone() {
            AnchorNode clone = new AnchorNode();
            clone.anchorPosition = new Vector3d(anchorPosition.getX(), anchorPosition.getY(), anchorPosition.getZ());
            clone.blockPosition = new Vector3i(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
            return clone;
        }
    }
}
