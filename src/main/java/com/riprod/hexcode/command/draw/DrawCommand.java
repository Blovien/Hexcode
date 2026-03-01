package com.riprod.hexcode.command.draw;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class DrawCommand extends AbstractPlayerCommand {
    public DrawCommand() {
        super("draw", "Draw settings for drawing a glyph");
        addAliases("d");

        addSubCommand(new DrawEnterCrafting());
        addSubCommand(new DrawTrainCommand());
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        // stub: would add to book here
        playerRef.sendMessage(Message.raw("Runs draw subcommands"));
    }
}
