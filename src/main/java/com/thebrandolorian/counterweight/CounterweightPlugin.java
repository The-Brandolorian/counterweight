package com.thebrandolorian.counterweight;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.thebrandolorian.counterweight.components.AnchorComponent;
import com.thebrandolorian.counterweight.components.PulleyComponent;
import com.thebrandolorian.counterweight.components.RopeComponent;
import com.thebrandolorian.counterweight.components.SpoolComponent;
import com.thebrandolorian.counterweight.interactions.LinkInteraction;

import javax.annotation.Nonnull;

public class CounterweightPlugin extends JavaPlugin {
    private static CounterweightPlugin INSTANCE;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ComponentType<EntityStore, RopeComponent> ropeComponentType;
    private ComponentType<ChunkStore, AnchorComponent> anchorComponentType;
    private ComponentType<ChunkStore, PulleyComponent> pulleyComponentType;
    private ComponentType<ChunkStore, SpoolComponent> spoolComponentType;

    public static CounterweightPlugin get() { return INSTANCE; }
    public CounterweightPlugin(@Nonnull JavaPluginInit init) { super(init); }

    public ComponentType<EntityStore, RopeComponent> getRopeComponentType() { return this.ropeComponentType; }
    public ComponentType<ChunkStore, AnchorComponent> getAnchorComponentType() { return this.anchorComponentType; }
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
        ropeComponentType = getEntityStoreRegistry().registerComponent(RopeComponent.class, RopeComponent::new);
        anchorComponentType = getChunkStoreRegistry().registerComponent(AnchorComponent.class, AnchorComponent::new);
        pulleyComponentType = getChunkStoreRegistry().registerComponent(PulleyComponent.class, PulleyComponent::new);
        spoolComponentType = getChunkStoreRegistry().registerComponent(SpoolComponent.class, SpoolComponent::new);
    }

    private void registerCommands() {

    }

    private void registerEvents() {

    }

    private void registerInteractions() {
        getCodecRegistry(Interaction.CODEC).register("Link", LinkInteraction.class, LinkInteraction.CODEC);
    }

    private void registerSystems() {

    }

    private void registerCoreComponents() {

    }
}
