package com.riprod.hexcode.command.glyph;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public class GlyphsCommand extends AbstractPlayerCommand {

    public GlyphsCommand() {
        super("glyphs", "Learn a glyph into held hexbook");
        addAliases("g");

        addSubCommand(new GlyphsLearnCommand());
        addSubCommand(new GlyphsListCommand());

    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        // stub: would add to book here
        playerRef.sendMessage(Message.raw("Runs glyph subcommands"));
    }
}
