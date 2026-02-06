package com.riprod.hexcode.command.test;

import java.util.List;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.glyphs.utils.CreateGlyph;

import javax.annotation.Nonnull;

public class SpawnGlyphCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final OptionalArg<String> glyphIdArg;
    private final OptionalArg<Float> scaleArg;
    private final FlagArg mountedArg;

    public SpawnGlyphCommand() {
        super("spawnGlyph", "Spawn a glyph entity for testing");
        this.glyphIdArg = this.withOptionalArg("glyph_id", "Glyph asset ID (default: Circle)", ArgTypes.STRING);
        this.scaleArg = this.withOptionalArg("scale", "Model scale (default: 1.0)", ArgTypes.FLOAT);
        this.mountedArg = this.withFlagArg("mounted", "Mount to player instead of spawning at position");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        String glyphId = glyphIdArg.get(ctx);
        if (glyphId == null)
            glyphId = "Circle";

        Float scale = scaleArg.get(ctx);
        if (scale == null)
            scale = 1.0f;

        boolean mounted = mountedArg.get(ctx);

        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyphId);
        if (asset == null) {
            playerRef.sendMessage(Message.raw("unknown glyph: " + glyphId));
            return;
        }

        TransformComponent playerTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        Vector3d playerPos = playerTransform.getPosition();

        playerRef.sendMessage(Message.raw("spawning glyph '" + glyphId + "' (scale=" + scale
                + ", mounted=" + mounted + ") at " + playerPos));

        GlyphComponent glyph = new GlyphComponent(glyphId);
        glyph.setScale(scale);

        if (mounted) {
            // spawn mounted to player with offset in front
            glyph.setOwnerRef(playerEntityRef);
            glyph.setOffset(new Vector3f(0, 2, 2));
        }

        // use Store directly (not CommandBuffer) — immediate spawn
        try {
            List<Ref<EntityStore>> refs = CreateGlyph.createGlyphEntity(store, glyph, playerPos);
            playerRef.sendMessage(
                    Message.raw("spawned " + refs.size() + " entity(s), ref[0] valid=" + refs.get(0).isValid()));

            // log what components the entity has
            Ref<EntityStore> glyphRef = refs.get(0);
            boolean hasTransform = store.getComponent(glyphRef, TransformComponent.getComponentType()) != null;
            boolean hasModel = store.getComponent(glyphRef,
                    com.hypixel.hytale.server.core.modules.entity.component.ModelComponent.getComponentType()) != null;
            boolean hasMount = store.getComponent(glyphRef, MountedComponent.getComponentType()) != null;
            boolean hasBBox = store.getComponent(glyphRef,
                    com.hypixel.hytale.server.core.modules.entity.component.BoundingBox.getComponentType()) != null;
            boolean hasGlyph = store.getComponent(glyphRef, GlyphComponent.getComponentType()) != null;

            playerRef.sendMessage(Message.raw(" components:transform=" + hasTransform
                    + " model=" + hasModel + " mount=" + hasMount
                    + " bbox=" + hasBBox + " glyph=" + hasGlyph));

            if (hasTransform) {
                TransformComponent t = store.getComponent(glyphRef, TransformComponent.getComponentType());
                playerRef.sendMessage(Message.raw("entity position: " + t.getPosition()));
            }
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("test spawn failed");
            playerRef.sendMessage(Message.raw("spawn failed: " + e.getMessage()));
        }
    }
}