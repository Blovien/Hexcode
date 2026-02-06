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
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;

import javax.annotation.Nonnull;

public class SpawnFromModelAssetCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final OptionalArg<String> modelIdArg;
    private final OptionalArg<Float> scaleArg;

    public SpawnFromModelAssetCommand() {
        super("spawnFromModelAsset", "Spawn entity from ModelAsset + GlyphComponent (no GlyphAsset lookup)");
        this.modelIdArg = this.withOptionalArg("model_id", "ModelAsset ID (default: Circle)", ArgTypes.STRING);
        this.scaleArg = this.withOptionalArg("scale", "Model scale (default: 1.0)", ArgTypes.FLOAT);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        String modelId = modelIdArg.get(ctx);
        if (modelId == null) modelId = "Circle";

        Float scale = scaleArg.get(ctx);
        if (scale == null) scale = 1.0f;

        playerRef.sendMessage(Message.raw("[spawnFromModelAsset] modelId=" + modelId + ", scale=" + scale));
        LOGGER.atInfo().log("[spawnFromModelAsset] modelId=%s, scale=%.2f", modelId, scale);

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelId);
        if (modelAsset == null) {
            playerRef.sendMessage(Message.raw("[spawnFromModelAsset] FAILED: ModelAsset not found: " + modelId));
            return;
        }
        playerRef.sendMessage(Message.raw("[spawnFromModelAsset] ModelAsset found: " + modelAsset.getId()));

        Model model = Model.createScaledModel(modelAsset, scale);
        playerRef.sendMessage(Message.raw("[spawnFromModelAsset] Model created, bbox=" + model.getBoundingBox()));

        TransformComponent playerTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        Vector3d playerPos = playerTransform.getPosition();

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(playerPos, new Vector3f(0, 0, 0)));
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(model.getBoundingBox()));
        holder.addComponent(GlyphComponent.getComponentType(), new GlyphComponent(modelId));
        playerRef.sendMessage(Message.raw("[spawnFromModelAsset] holder has: transform, model, bbox, glyphComp"));

        try {
            Ref<EntityStore> ref = store.addEntity(holder, AddReason.SPAWN);
            playerRef.sendMessage(Message.raw("[spawnFromModelAsset] spawned, ref valid=" + ref.isValid()));

            TransformComponent t = store.getComponent(ref, TransformComponent.getComponentType());
            if (t != null) {
                playerRef.sendMessage(Message.raw("[spawnFromModelAsset] entity pos=" + t.getPosition()));
            }
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("[spawnFromModelAsset] failed");
            playerRef.sendMessage(Message.raw("[spawnFromModelAsset] FAILED: " + e.getMessage()));
        }
    }
}
