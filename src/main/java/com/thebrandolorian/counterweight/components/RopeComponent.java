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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RopeComponent implements Component<EntityStore> {
    public static final BuilderCodec<RopeComponent> CODEC =
            BuilderCodec.builder(RopeComponent.class, RopeComponent::new)
                    .append(new KeyedCodec<>("TotalLength", Codec.FLOAT), (c, v) -> c.totalLength = v, c -> c.totalLength).add()
                    .append(new KeyedCodec<>("DeployedLength", Codec.FLOAT), (c, v) -> c.deployedLength = v, c -> c.deployedLength).add()
                    .append(new KeyedCodec<>("IsLooped", Codec.BOOLEAN), (c, v) -> c.isLooped = v, c -> c.isLooped).add()
                    .append(new KeyedCodec<>("Nodes", new ArrayCodec<>(RopeNode.CODEC, RopeNode[]::new)),
                            (c, v) -> c.nodes = new ArrayList<>(Arrays.asList(v)),
                            c -> c.nodes.toArray(new RopeNode[0])).add()
                    .build();

    public static ComponentType<EntityStore, RopeComponent> getComponentType() { return CounterweightPlugin.get().getRopeComponentType(); }

    private float totalLength = 1f;
    private float deployedLength = 0f;
    private boolean isLooped = false;
    private List<RopeNode> nodes = new ArrayList<>();
    private List<Vector3d> cachedNodePositions = new ArrayList<>();

    public void addNode(RopeNode node) { this.nodes.add(node); }
    public float getRemainingLength() { return totalLength - deployedLength; }

    // Getters & Setters
    public float getTotalLength() { return totalLength; }
    public void setTotalLength(float totalLength) { this.totalLength = totalLength; }

    public float getDeployedLength() { return deployedLength; }
    public void setDeployedLength(float len) { this.deployedLength = Math.max(0f, Math.min(totalLength, len)); }

    public boolean isLooped() { return isLooped; }
    public void setLooped(boolean looped) { isLooped = looped; }

    public List<RopeNode> getNodes() { return nodes; }
    public void setNodes(List<RopeNode> nodes) { this.nodes = new ArrayList<>(nodes); }

    public List<Vector3d> getCachedNodePositions() { return cachedNodePositions; }
    public void setCachedNodePositions(List<Vector3d> positions) { this.cachedNodePositions = positions; }

    @Nullable
    @Override
    public RopeComponent clone() {
        RopeComponent rope = new RopeComponent();
        rope.totalLength = this.totalLength;
        rope.deployedLength = this.deployedLength;
        rope.isLooped = this.isLooped;
        for (RopeNode node : this.nodes) rope.addNode(node.clone());
        return rope;
    }

    public static class RopeNode {
        public enum AnchorType { BLOCK, ENTITY }
        private static final Codec<AnchorType> ANCHOR_TYPE_CODEC = new EnumCodec<>(AnchorType.class);

        public static final BuilderCodec<RopeNode> CODEC = BuilderCodec.builder(RopeNode.class, RopeNode::new)
                .append(new KeyedCodec<>("pos", Vector3d.CODEC), (n, v) -> n.position = v, n -> n.position).add()
                .append(new KeyedCodec<>("uuid", Codec.UUID_BINARY), (n, v) -> n.uuid = v, n -> n.uuid).add()
                .append(new KeyedCodec<>("type", ANCHOR_TYPE_CODEC), (n, v) -> n.type = v, n -> n.type).add()
                .append(new KeyedCodec<>("anchorName", Codec.STRING), (n, v) -> n.anchorName = v, n -> n.anchorName).add()
                .afterDecode((node, extraInfo) -> {
                    if (!node.isValid()) {
                        node.type = AnchorType.BLOCK;
                        if (node.position == null) node.position = Vector3d.ZERO;
                        CounterweightPlugin.get().getLogger().atSevere().log("RopeNode " + node.toString() + " does not have a valid anchor");
                    }
                })
                .build();

        @Nullable private Vector3d position;
        @Nullable private UUID uuid;
        private AnchorType type = AnchorType.BLOCK;
        @Nullable private String anchorName;

        public RopeNode() {}

        public boolean isValid() {
            if (type == AnchorType.BLOCK) return position != null;
            if (type == AnchorType.ENTITY) return uuid != null;
            return false;
        }

        public static RopeNode block(Vector3d pos) {
            RopeNode node = new RopeNode();
            node.position = pos;
            node.type = AnchorType.BLOCK;
            return node;
        }

        public static RopeNode entity(UUID id) {
            RopeNode node = new RopeNode();
            node.uuid = id;
            node.type = AnchorType.ENTITY;
            return node;
        }

        public AnchorType getType() { return type; }
        @Nullable public String getAnchorName() { return anchorName; }
        @Nullable public Vector3d getPosition() { return position; }
        @Nullable public UUID getUuid() { return uuid; }

        @Override
        public RopeNode clone() {
            RopeNode clone = new RopeNode();
            clone.position = this.position != null ? new Vector3d(this.position.getX(), this.position.getY(), this.position.getZ()) : null;
            clone.uuid = this.uuid;
            clone.type = this.type;
            clone.anchorName = this.anchorName;
            return clone;
        }
    }
}
