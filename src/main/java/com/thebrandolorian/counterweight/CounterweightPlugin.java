package com.thebrandolorian.counterweight;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.thebrandolorian.counterweight.components.*;
import com.thebrandolorian.counterweight.interactions.LinkInteraction;
import com.thebrandolorian.counterweight.systems.RopeSystems;

import javax.annotation.Nonnull;

public class CounterweightPlugin extends JavaPlugin {
    private static CounterweightPlugin INSTANCE;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ComponentType<ChunkStore, AnchorComponent> anchorComponentType;
    private ComponentType<ChunkStore, RopeComponent> ropeComponentType;
    private ComponentType<ChunkStore, RopeSegmentComponent> ropeSegmentType;
    private ComponentType<ChunkStore, PulleyComponent> pulleyComponentType;
    private ComponentType<ChunkStore, SpoolComponent> spoolComponentType;

    public static CounterweightPlugin get() { return INSTANCE; }
    public CounterweightPlugin(@Nonnull JavaPluginInit init) { super(init); }

    public ComponentType<ChunkStore, AnchorComponent> getAnchorComponentType() { return this.anchorComponentType; }
    public ComponentType<ChunkStore, RopeComponent> getRopeComponentType() { return this.ropeComponentType; }
    public ComponentType<ChunkStore, RopeSegmentComponent> getRopeSegmentComponentType() { return this.ropeSegmentType; }
    public ComponentType<ChunkStore, PulleyComponent> getPulleyComponentType() { return this.pulleyComponentType; }
    public ComponentType<ChunkStore, SpoolComponent> getSpoolComponentType() { return this.spoolComponentType; }

    @Override
    protected void setup() {
        INSTANCE = this;

        registerComponents();
        registerCommands();
        registerEvents();

        LOGGER.atInfo().log("Successfully loaded %s v%s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void start() {
        registerSystems();
        registerCoreComponents();
        registerInteractions();
    }

    private void registerComponents() {
        anchorComponentType = getChunkStoreRegistry().registerComponent(AnchorComponent.class, "AnchorComponent", AnchorComponent.CODEC);
        ropeComponentType = getChunkStoreRegistry().registerComponent(RopeComponent.class, "RopeComponent", RopeComponent.CODEC);
        ropeSegmentType = getChunkStoreRegistry().registerComponent(RopeSegmentComponent.class, "RopeSegmentComponent", RopeSegmentComponent.CODEC);
        pulleyComponentType = getChunkStoreRegistry().registerComponent(PulleyComponent.class, "PulleyComponent", PulleyComponent.CODEC);
        spoolComponentType = getChunkStoreRegistry().registerComponent(SpoolComponent.class, "SpoolComponent", SpoolComponent.CODEC);
    }

    private void registerCommands() {

    }

    private void registerEvents() {

    }

    private void registerInteractions() {
        getCodecRegistry(Interaction.CODEC).register("Link", LinkInteraction.class, LinkInteraction.CODEC);
    }

    private void registerSystems() {
        this.getChunkStoreRegistry().registerSystem(new RopeSystems.AnchorListener());
        this.getChunkStoreRegistry().registerSystem(new RopeSystems.RopeSegmentListener());
        this.getChunkStoreRegistry().registerSystem(new RopeSystems.RopeListener());
        this.getChunkStoreRegistry().registerSystem(new RopeSystems.RopeTickingSystem());
    }

    private void registerCoreComponents() {

    }

}
