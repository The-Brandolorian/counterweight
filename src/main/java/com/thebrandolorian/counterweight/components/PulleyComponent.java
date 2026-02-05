package com.thebrandolorian.counterweight.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.thebrandolorian.counterweight.CounterweightPlugin;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

public class PulleyComponent implements Component<ChunkStore> {
    public static final BuilderCodec<PulleyComponent> CODEC =
            BuilderCodec.builder(PulleyComponent.class, PulleyComponent::new)

                    .append(new KeyedCodec<>("Axis", Vector3f.CODEC),
                            (c,v) -> c.axis.assign(v.x, v.y, v.z),
                            c -> new Vector3f(c.axis.x, c.axis.y, c.axis.z)).add()

                    .append(new KeyedCodec<>("Angle", Codec.FLOAT),
                            (c,v) -> c.angle = v,
                            c -> c.angle).add()

                    .append(new KeyedCodec<>("Radius", Codec.FLOAT),
                            (c,v) -> c.radius = v,
                            c -> c.radius).add()

                    .build();

    public static ComponentType<ChunkStore, PulleyComponent> getComponentType() {
        return CounterweightPlugin.get().getPulleyComponentType();
    }
    
    public PulleyComponent() { }

    @Getter private Vector3f axis = new Vector3f(1,0,0);
    @Getter @Setter private float angle = 0f;
    @Getter @Setter private float radius = 1f;
    
    public void setAxis(Vector3f axis) {
        float x = Math.abs(axis.x);
        float y = Math.abs(axis.y);
        float z = Math.abs(axis.z);

        if (x >= y && x >= z) this.axis.assign(Math.signum(axis.x),0,0);
        else if (y >= x && y >= z) this.axis.assign(0,Math.signum(axis.y),0);
        else this.axis.assign(0,0,Math.signum(axis.z));
    }
	
	@Override
    public Component<ChunkStore> clone() {
        PulleyComponent c = new PulleyComponent();
        c.axis = new Vector3f(axis.x, axis.y, axis.z);
        c.angle = angle;
        c.radius = radius;
        return c;
    }
}
