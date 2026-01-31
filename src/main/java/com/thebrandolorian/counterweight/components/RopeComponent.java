package com.thebrandolorian.counterweight.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
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
                    .append(new KeyedCodec<>("Nodes", new ArrayCodec<>(Codec.UUID_BINARY, UUID[]::new)), (c, v) -> c.nodes = new ArrayList<>(Arrays.asList(v)), c -> c.nodes.toArray(new UUID[0])).add()
                    .build();

    public static ComponentType<EntityStore, RopeComponent> getComponentType() {
        return CounterweightPlugin.get().getRopeComponentType();
    }

    private float totalLength = 1f;
    private float deployedLength = 0f;
    private boolean isLooped = false;
    private List<UUID> nodes = new ArrayList<>();

    // Getters & Setters
    public float getTotalLength() { return totalLength; }
    public void setTotalLength(float totalLength) { this.totalLength = totalLength; }

    public float getDeployedLength() { return deployedLength; }
    public void setDeployedLength(float len) { this.deployedLength = Math.max(0f, Math.min(totalLength, len)); }

    public boolean isLooped() { return isLooped; }
    public void setLooped(boolean looped) { isLooped = looped; }

    public List<UUID> getNodes() { return nodes; }
    public void setNodes(List<UUID> nodes) { this.nodes = new ArrayList<>(nodes); }

    // Util
    public void addNode(UUID entityId) { nodes.add(entityId); }
    public float getRemainingLength() { return totalLength - deployedLength; }
    public boolean isFullyDeployed() { return deployedLength >= totalLength; }
    public boolean isFullyRetracted() { return deployedLength <= 0f; }

    @Nullable
    @Override
    public RopeComponent clone() {
        RopeComponent rope = new RopeComponent();
        rope.totalLength = this.totalLength;
        rope.deployedLength = this.deployedLength;
        rope.isLooped = this.isLooped;
        rope.nodes = new ArrayList<>(this.nodes);
        return rope;
    }
}
