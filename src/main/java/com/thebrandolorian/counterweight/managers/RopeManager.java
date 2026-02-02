package com.thebrandolorian.counterweight.managers;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.*;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.thebrandolorian.counterweight.CounterweightPlugin;
import com.thebrandolorian.counterweight.components.AnchorComponent;
import com.thebrandolorian.counterweight.components.RopeComponent;
import javax.annotation.Nonnull;

public class RopeManager {

    public static boolean trySpawnRopeBetweenBlocks(
            @Nonnull World world,
            @Nonnull Vector3i position1,
            @Nonnull Vector3i position2) {

        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        AnchorComponent anchorComponent1 = tryGetBlockAnchorComponentFromPosition(world, chunkStore, position1);
        AnchorComponent anchorComponent2 = tryGetBlockAnchorComponentFromPosition(world, chunkStore, position2);
        if (anchorComponent1 == null || anchorComponent2 == null) return false;

        Vector3d start = new Vector3d(position1.getX() + 0.5, position1.getY() + 0.5, position1.getZ() + 0.5);
        Vector3d end = new Vector3d(position2.getX() + 0.5, position2.getY() + 0.5, position2.getZ() + 0.5);
        float distance = (float)start.distanceTo(end);

        Vector3d position = Vector3d.lerp(start, end, 0.5);
        Vector3f rotation = new Vector3f(0, 0, 0);

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset("Deco_Rope");
        if (modelAsset == null) return false;

        Model model = Model.createScaledModel(modelAsset, 1.0f);
        if (model.getBoundingBox() == null) return false;

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        world.execute(() -> {
            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

            RopeComponent rope = new RopeComponent();
            rope.addNode(RopeComponent.AnchorNode.block(start, position1));
            rope.addNode(RopeComponent.AnchorNode.block(end, position2));
            rope.setTotalLength(distance);
            rope.setDeployedLength(distance);

            UUIDComponent uuidComponent = holder.ensureAndGetComponent(UUIDComponent.getComponentType());
            holder.ensureComponent(Interactable.getComponentType());
            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(position, rotation));
            holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(model.getBoundingBox()));
            holder.addComponent(NetworkId.getComponentType(), new NetworkId(entityStore.getExternalData().takeNextNetworkId()));
            holder.addComponent(Interactions.getComponentType(), new Interactions()); // future interactions?
            holder.addComponent(RopeComponent.getComponentType(), rope);

            AnchorComponent a1 = tryGetBlockAnchorComponentFromPosition(world, chunkStore, position1);
            AnchorComponent a2 = tryGetBlockAnchorComponentFromPosition(world, chunkStore, position2);

            if (a1 != null && a2 != null) {
                a1.setLinkedRope(uuidComponent.getUuid());
                a2.setLinkedRope(uuidComponent.getUuid());
                entityStore.addEntity(holder, AddReason.SPAWN);
            } else CounterweightPlugin.get().getLogger().atWarning().log("Rope spawn failed: Anchors were invalidated before the entity could be committed.");
        });

        return true;
    }

    public static AnchorComponent tryGetBlockAnchorComponentFromPosition(World world, Store<ChunkStore> chunkStore, Vector3i position) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(position.getX(), position.getZ());
        WorldChunk worldChunk = world.getChunkIfLoaded(chunkIndex);
        if (worldChunk == null || worldChunk.getBlockComponentChunk() == null) return null;

        int blockIndex = ChunkUtil.indexBlockInColumn(ChunkUtil.localCoordinate(position.getX()), position.getY(), ChunkUtil.localCoordinate(position.getZ()));
        Ref<ChunkStore> blockRef = worldChunk.getBlockComponentChunk().getEntityReference(blockIndex);
        if (blockRef == null) return null;

        return chunkStore.getComponent(blockRef, AnchorComponent.getComponentType());
    }
}