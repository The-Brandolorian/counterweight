package com.thebrandolorian.counterweight.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3d;
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
                .append(new KeyedCodec<>("pos", Vector3d.CODEC), (n, v) -> n.position = v, n -> n.position).add()
                .afterDecode((node, extraInfo) -> {
                    if (!node.isValid()) {
                        if (node.position == null) node.position = Vector3d.ZERO;
                        CounterweightPlugin.get().getLogger().atSevere().log("RopeNode " + node.toString() + " does not have a valid anchor");
                    }
                })
                .build();

        @Nullable private Vector3d position;

        public AnchorNode() {}

        public boolean isValid() {
            return position != null;
        }

        public static AnchorNode block(Vector3d pos) {
            AnchorNode node = new AnchorNode();
            node.position = pos;
            return node;
        }

        @Nullable public Vector3d getPosition() { return position; }

        @Override
        public AnchorNode clone() {
            AnchorNode clone = new AnchorNode();
            clone.position = this.position != null ? new Vector3d(this.position.getX(), this.position.getY(), this.position.getZ()) : null;
            return clone;
        }
    }
}
