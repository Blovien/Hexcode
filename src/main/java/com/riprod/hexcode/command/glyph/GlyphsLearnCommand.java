package com.riprod.hexcode.command.glyph;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.hexbook.HexBookComponent;
import com.riprod.hexcode.player.system.CasterInventory;

import javax.annotation.Nonnull;

public class GlyphsLearnCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> glyphIdArg;
    private final OptionalArg<Float> accuracyArg;
    private final OptionalArg<Float> speedArg;

    public GlyphsLearnCommand() {
        super("learn", "Learn a glyph into held hexbook");
        addAliases("l");

        this.glyphIdArg = this.withRequiredArg("glyphId", "The glyph ID to learn", ArgTypes.STRING);
        this.accuracyArg = this.withOptionalArg("accuracy", "Accuracy modifier (default 1.0)", ArgTypes.FLOAT);
        this.speedArg = this.withOptionalArg("speed", "Speed modifier (default 1.0)", ArgTypes.FLOAT);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        String glyphId = glyphIdArg.get(context);
        Float accuracy = accuracyArg.get(context);
        Float speed = speedArg.get(context);

        if (accuracy == null) {
            accuracy = 1.0f;
        }
        if (speed == null) {
            speed = 1.0f;
        }

        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyphId);
        if (asset == null) {
            playerRef.sendMessage(Message.raw("Unknown glyph: " + glyphId));
            return;
        }

        GlyphComponent glyph = new GlyphComponent(glyphId, accuracy, speed);
        
        HexBookComponent bookComponent = CasterInventory.getHexBookComponent(store, playerEntityRef);

        bookComponent.addGlyph(glyph);

        CasterInventory.saveHexBookComponent(store, playerEntityRef, bookComponent);

        // stub: would add to book here
        playerRef.sendMessage(Message.raw("(debug) Learned glyph '" + glyphId + "' (accuracy=" + accuracy + ", speed=" + speed + ")"));
    }
}
