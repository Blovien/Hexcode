package com.riprod.hexcode.command.draw;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.player.component.HexcasterComponent;
import javax.annotation.Nonnull;

public class DrawEnterCrafting extends AbstractPlayerCommand {

    public DrawEnterCrafting() {
        super("crafting", "Toggle crafting mode");
        addAliases("c");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        HexcasterComponent casterComponent = store.getComponent(playerEntityRef, HexcasterComponent.getComponentType());

        if (casterComponent.getCurrentMode() == HexcasterComponent.HexcasterMode.CRAFTING) {
            casterComponent.setCurrentMode(HexcasterComponent.HexcasterMode.IDLE);
            playerRef.sendMessage(Message.raw("Exited crafting mode"));
        } else {
            casterComponent.setCurrentMode(HexcasterComponent.HexcasterMode.CRAFTING);
            playerRef.sendMessage(Message.raw("Entered crafting mode"));
        }

    }
}
