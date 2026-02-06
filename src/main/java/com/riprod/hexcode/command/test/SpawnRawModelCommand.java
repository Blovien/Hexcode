package com.riprod.hexcode.command.test;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class SpawnRawModelCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final OptionalArg<String> modelIdArg;

    public SpawnRawModelCommand() {
        super("spawnRawModel", "Spawn a raw model entity (transform + model + bbox only)");
        this.modelIdArg = this.withOptionalArg("model_id", "ModelAsset ID (default: Circle)", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        String modelId = modelIdArg.get(ctx);
        if (modelId == null) modelId = "Circle";

        playerRef.sendMessage(Message.raw("[spawnRawModel] looking up ModelAsset: " + modelId));
        LOGGER.atInfo().log("[spawnRawModel] looking up ModelAsset: %s", modelId);

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelId);
        if (modelAsset == null) {
            playerRef.sendMessage(Message.raw("[spawnRawModel] FAILED: ModelAsset not found: " + modelId));
            return;
        }
        playerRef.sendMessage(Message.raw("[spawnRawModel] ModelAsset found: id=" + modelAsset.getId()));

        Model model = Model.createScaledModel(modelAsset, 1.0f);
        playerRef.sendMessage(Message.raw("[spawnRawModel] Model created, assetId=" + model.getModelAssetId()
                + ", bbox=" + model.getBoundingBox()));

        TransformComponent playerTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        Vector3d playerPos = playerTransform.getPosition();
        playerRef.sendMessage(Message.raw("[spawnRawModel] player pos=" + playerPos));

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(playerPos, new Vector3f(0, 0, 0)));
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(model.getBoundingBox()));

        playerRef.sendMessage(Message.raw("[spawnRawModel] holder ready, calling store.addEntity..."));

        try {
            Ref<EntityStore> ref = store.addEntity(holder, AddReason.SPAWN);
            playerRef.sendMessage(Message.raw("[spawnRawModel] entity spawned, ref valid=" + ref.isValid()));

            TransformComponent t = store.getComponent(ref, TransformComponent.getComponentType());
            ModelComponent m = store.getComponent(ref, ModelComponent.getComponentType());
            BoundingBox b = store.getComponent(ref, BoundingBox.getComponentType());
            playerRef.sendMessage(Message.raw("[spawnRawModel] components: transform=" + (t != null)
                    + " model=" + (m != null) + " bbox=" + (b != null)));
            if (t != null) {
                playerRef.sendMessage(Message.raw("[spawnRawModel] entity pos=" + t.getPosition()));
            }
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("[spawnRawModel] failed");
            playerRef.sendMessage(Message.raw("[spawnRawModel] FAILED: " + e.getMessage()));
        }
    }
}
