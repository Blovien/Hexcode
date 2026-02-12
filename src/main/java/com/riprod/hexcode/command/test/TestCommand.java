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
        super("test", "Laser beam test commands");
        addAliases("t");

        addSubCommand(new TestLaserPointCommand());
        addSubCommand(new TestLaserDistanceCommand());
        addSubCommand(new TestLaserDurationCommand());
        addSubCommand(new TestLaserGridCommand());
        addSubCommand(new TestLaserSwapCommand());
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
            PlayerRef playerRef, World world) {
        ctx.sendMessage(Message.raw("Test Commands:"));
        ctx.sendMessage(Message.raw("  /hc test point - laser at exact eye pos, 0.1 block forward"));
        ctx.sendMessage(Message.raw("  /hc test distance <blocks> - laser from eye, N blocks forward"));
        ctx.sendMessage(Message.raw("  /hc test duration <ms> - laser 2 blocks forward, variable duration"));
        ctx.sendMessage(Message.raw("  /hc test grid - 5 lasers at increasing lengths (0.1, 0.5, 1, 2, 5)"));
        ctx.sendMessage(Message.raw("  /hc test swap - 4 lasers testing start/end swap + playerNetworkId=0"));
    }
}
