package com.riprod.hexcode.command.test;

import java.util.List;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.glyphs.utils.CreateGlyph;

import javax.annotation.Nonnull;

public class SpawnFromGlyphAssetCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final OptionalArg<String> glyphIdArg;
    private final OptionalArg<Float> scaleArg;

    public SpawnFromGlyphAssetCommand() {
        super("spawnFromGlyphAsset", "Spawn entity via GlyphAsset -> ModelAsset -> CreateGlyph pipeline");
        this.glyphIdArg = this.withOptionalArg("glyph_id", "GlyphAsset ID (default: Circle)", ArgTypes.STRING);
        this.scaleArg = this.withOptionalArg("scale", "Model scale (default: 1.0)", ArgTypes.FLOAT);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        String glyphId = glyphIdArg.get(ctx);
        if (glyphId == null) glyphId = "Circle";

        Float scale = scaleArg.get(ctx);
        if (scale == null) scale = 1.0f;

        playerRef.sendMessage(Message.raw("[spawnFromGlyphAsset] glyphId=" + glyphId + ", scale=" + scale));
        LOGGER.atInfo().log("[spawnFromGlyphAsset] glyphId=%s, scale=%.2f", glyphId, scale);

        GlyphAsset glyphAsset = GlyphAsset.getAssetMap().getAsset(glyphId);
        if (glyphAsset == null) {
            playerRef.sendMessage(Message.raw("[spawnFromGlyphAsset] FAILED: GlyphAsset not found: " + glyphId));
            return;
        }
        playerRef.sendMessage(Message.raw("[spawnFromGlyphAsset] GlyphAsset found: id=" + glyphAsset.getId()
                + ", modelPath=" + glyphAsset.getModelPath()));

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(glyphAsset.getModelPath());
        if (modelAsset == null) {
            playerRef.sendMessage(Message.raw("[spawnFromGlyphAsset] FAILED: ModelAsset not found for path: "
                    + glyphAsset.getModelPath()));
            return;
        }
        playerRef.sendMessage(Message.raw("[spawnFromGlyphAsset] ModelAsset resolved: " + modelAsset.getId()));

        TransformComponent playerTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        Vector3d playerPos = playerTransform.getPosition();

        GlyphComponent glyph = new GlyphComponent(glyphId);
        glyph.setScale(scale);
        playerRef.sendMessage(Message.raw("[spawnFromGlyphAsset] calling CreateGlyph.createGlyphEntity via Store..."));

        try {
            List<Ref<EntityStore>> refs = CreateGlyph.createGlyphEntity(store, glyph, playerPos);
            playerRef.sendMessage(Message.raw("[spawnFromGlyphAsset] spawned " + refs.size() + " entity(s)"));

            Ref<EntityStore> ref = refs.get(0);
            playerRef.sendMessage(Message.raw("[spawnFromGlyphAsset] ref valid=" + ref.isValid()));

            TransformComponent t = store.getComponent(ref, TransformComponent.getComponentType());
            ModelComponent m = store.getComponent(ref, ModelComponent.getComponentType());
            GlyphComponent g = store.getComponent(ref, GlyphComponent.getComponentType());
            playerRef.sendMessage(Message.raw("[spawnFromGlyphAsset] components: transform=" + (t != null)
                    + " model=" + (m != null) + " glyph=" + (g != null)));
            if (t != null) {
                playerRef.sendMessage(Message.raw("[spawnFromGlyphAsset] entity pos=" + t.getPosition()));
            }
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("[spawnFromGlyphAsset] failed");
            playerRef.sendMessage(Message.raw("[spawnFromGlyphAsset] FAILED: " + e.getMessage()));
        }
    }
}
