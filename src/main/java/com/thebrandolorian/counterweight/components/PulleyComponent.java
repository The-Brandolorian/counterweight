package com.thebrandolorian.counterweight.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.codec.ProtocolCodecs;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.thebrandolorian.counterweight.CounterweightPlugin;
import org.jetbrains.annotations.Nullable;

public class PulleyComponent implements Component<EntityStore> {
    public static final BuilderCodec<PulleyComponent> CODEC =
            BuilderCodec.builder(PulleyComponent.class, PulleyComponent::new)

                    .append(new KeyedCodec<>("Axis", ProtocolCodecs.VECTOR3F),
                            (c,v) -> c.axis.assign(v.x, v.y, v.z),
                            c -> new com.hypixel.hytale.protocol.Vector3f(c.axis.x, c.axis.y, c.axis.z)).add()

                    .append(new KeyedCodec<>("Angle", Codec.FLOAT),
                            (c,v) -> c.angle = v,
                            c -> c.angle).add()

                    .append(new KeyedCodec<>("Radius", Codec.FLOAT),
                            (c,v) -> c.radius = v,
                            c -> c.radius).add()

                    .build();

    public static ComponentType<EntityStore, PulleyComponent> getComponentType() {
        return CounterweightPlugin.get().getPulleyComponentType();
    }

    // Data
    private Vector3f axis = new Vector3f(1,0,0);
    private float angle = 0f;
    private float radius = 1f;


    // Getters
    public Vector3f getAxis(){ return axis; }
    public float getAngle(){ return angle; }
    public float getRadius(){ return radius; }

    // Setters
    public void setAxis(Vector3f axis) {
        float x = Math.abs(axis.x);
        float y = Math.abs(axis.y);
        float z = Math.abs(axis.z);

        if (x >= y && x >= z) this.axis.assign(Math.signum(axis.x),0,0);
        else if (y >= x && y >= z) this.axis.assign(0,Math.signum(axis.y),0);
        else this.axis.assign(0,0,Math.signum(axis.z));
    }
    public void setAngle(float angle) { this.angle = angle; }
    public void setRadius(float radius) { this.radius = radius; }

    @Override
    public @Nullable Component<EntityStore> clone() {
        PulleyComponent c = new PulleyComponent();
        c.axis = new Vector3f(axis.x, axis.y, axis.z);
        c.angle = angle;
        c.radius = radius;
        return c;
    }
}
