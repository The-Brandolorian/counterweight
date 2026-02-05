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
import com.thebrandolorian.counterweight.utils.DebugUtils;
import lombok.Getter;

import javax.annotation.Nonnull;

public class CounterweightPlugin extends JavaPlugin {
    private static CounterweightPlugin INSTANCE;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private @Getter ComponentType<ChunkStore, AnchorComponent> anchorComponentType;
    private @Getter ComponentType<ChunkStore, RopeComponent> ropeComponentType;
    private @Getter ComponentType<ChunkStore, RopeSegmentComponent> ropeSegmentComponentType;
    private @Getter ComponentType<ChunkStore, PulleyComponent> pulleyComponentType;
    private @Getter ComponentType<ChunkStore, SpoolComponent> spoolComponentType;

    public static CounterweightPlugin get() { return CounterweightPlugin.INSTANCE; }
    public CounterweightPlugin(@Nonnull JavaPluginInit init) { super(init); }

    @Override
    protected void setup() {
        INSTANCE = this;

        registerComponents();
        registerCommands();
        registerEvents();
		
		DebugUtils.logInfo("Successfully loaded %s v%s", this.getName(), this.getManifest().getVersion().toString());
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
        ropeSegmentComponentType = getChunkStoreRegistry().registerComponent(RopeSegmentComponent.class, "RopeSegmentComponent", RopeSegmentComponent.CODEC);
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
