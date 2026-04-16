package com.riprod.hexcode.core.state.crafting.session;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;

public class SessionLifecycleHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void onPlayerConnect(PlayerConnectEvent event) {
        try {
            Holder<EntityStore> holder = event.getHolder();
            holder.ensureComponent(HexcasterCraftingComponent.getComponentType());
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] onPlayerConnect session handler failed: %s", e.getMessage());
        }
    }
}
