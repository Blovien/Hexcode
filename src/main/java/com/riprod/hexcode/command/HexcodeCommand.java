package com.riprod.hexcode.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.command.test.TestCommand;

import javax.annotation.Nonnull;

public class HexcodeCommand extends AbstractPlayerCommand {
    public HexcodeCommand() {
        super("hexcode", "Hexcode spell-crafting mod commands");
        this.setPermissionGroup(GameMode.Creative);
        addAliases("hc");

        addSubCommand(new LearnCommand());
        addSubCommand(new TestCommand());
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
            PlayerRef playerRef, World world) {
        showHelp(ctx);
    }

    private void showHelp(CommandContext ctx) {
        ctx.sendMessage(Message.raw("Hexcode Commands:"));
        ctx.sendMessage(Message.raw("  /hexcode learn <glyphId> [accuracy] [speed] - Learn a glyph into held hexbook"));
        ctx.sendMessage(Message.raw("  /hexcode spawnGlyph [glyph_id] [scale] [mounted] - Spawn a glyph entity for testing"));
    }
}
