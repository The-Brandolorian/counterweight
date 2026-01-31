package com.thebrandolorian.counterweight.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.thebrandolorian.counterweight.CounterweightPlugin;

import javax.annotation.Nullable;
import java.util.UUID;

public class SpoolComponent implements Component<EntityStore> {
    public static final BuilderCodec<SpoolComponent> CODEC =
            BuilderCodec.builder(SpoolComponent.class, SpoolComponent::new)
                    .append(new KeyedCodec<>("Speed", Codec.FLOAT),
                            (c, v) -> c.speed = v,
                            c -> c.speed).add()

                    .append(new KeyedCodec<>("LinkedRope", Codec.UUID_BINARY),
                            (c, v) -> c.linkedRope = v, c -> c.linkedRope).add()

                    .build();

    public static ComponentType<EntityStore, SpoolComponent> getComponentType() {
        return CounterweightPlugin.get().getSpoolComponentType();
    }

    // Data
    private float speed = 1f;
    private @Nullable UUID linkedRope = null;

    // Getters & Setters
    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }

    public @Nullable UUID getLinkedRope() { return linkedRope; }
    public void setLinkedRope(@Nullable UUID linkedRope) { this.linkedRope = linkedRope; }

    @Override
    public SpoolComponent clone() {
        SpoolComponent c = new SpoolComponent();
        c.speed = this.speed;
        c.linkedRope = this.linkedRope;
        return c;
    }
}
