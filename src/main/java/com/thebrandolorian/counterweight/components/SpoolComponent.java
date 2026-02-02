package com.thebrandolorian.counterweight.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.thebrandolorian.counterweight.CounterweightPlugin;

import javax.annotation.Nullable;

public class SpoolComponent implements Component<ChunkStore> {
    public static final BuilderCodec<SpoolComponent> CODEC =
            BuilderCodec.builder(SpoolComponent.class, SpoolComponent::new)
                    .append(new KeyedCodec<>("Speed", Codec.FLOAT),
                            (c, v) -> c.speed = v,
                            c -> c.speed).add()
                    .build();

    public static ComponentType<ChunkStore, SpoolComponent> getComponentType() {
        return CounterweightPlugin.get().getSpoolComponentType();
    }

    // Data
    private float speed = 1f;

    // Getters & Setters
    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }

    @Override
    public @Nullable SpoolComponent clone() {
        SpoolComponent c = new SpoolComponent();
        c.speed = this.speed;
        return c;
    }
}
