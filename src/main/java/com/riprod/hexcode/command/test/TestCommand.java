package com.riprod.hexcode.command.test;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public class TestCommand extends AbstractPlayerCommand {
    public TestCommand() {
        super("test", "Test Commands");
        addSubCommand(new SpawnRawModelCommand());
        addSubCommand(new SpawnFromModelAssetCommand());
        addSubCommand(new SpawnFromGlyphAssetCommand());
        addSubCommand(new SpawnGlyphCommand());
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        showHelp(ctx);
    }

    private void showHelp(CommandContext ctx) {
        ctx.sendMessage(Message.raw("Test Commands:"));
        ctx.sendMessage(
                Message.raw("  /test spawnGlyph [glyph_id] [scale] [mounted] - Spawn a glyph entity for testing"));
    }
}