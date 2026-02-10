package com.riprod.hexcode.command.glyph;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.hexbook.component.HexBookComponent;
import com.riprod.hexcode.player.system.CasterInventory;

import javax.annotation.Nonnull;

public class GlyphsForgetCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> glyphIdArg;

    public GlyphsForgetCommand() {
        super("forget", "Forget a glyph from held hexbook");
        addAliases("f");

        this.glyphIdArg = this.withRequiredArg("glyphId", "The glyph ID to forget", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        String glyphId = glyphIdArg.get(context);

        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyphId);
        if (asset == null) {
            playerRef.sendMessage(Message.raw("Unknown glyph: " + glyphId));
            return;
        }

        HexBookComponent bookComponent = CasterInventory.getHexBookComponent(store, playerEntityRef);

        bookComponent.removeGlyph(glyphId);

        CasterInventory.saveHexBookComponent(store, playerEntityRef, bookComponent);

        // stub: would add to book here
        playerRef.sendMessage(Message
                .raw("(debug) Forgot glyph '" + glyphId + "' from your hexbook! (if it existed)"));
    }
}
