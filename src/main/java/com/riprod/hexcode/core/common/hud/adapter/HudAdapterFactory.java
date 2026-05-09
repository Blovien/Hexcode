package com.riprod.hexcode.core.common.hud.adapter;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.riprod.hexcode.core.common.hud.api.HudAdapter;

import javax.annotation.Nonnull;

public final class HudAdapterFactory {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final PluginIdentifier MULTIPLE_HUD_ID = PluginIdentifier.fromString("Buuz135:MultipleHUD");

    private HudAdapterFactory() {
    }

    @Nonnull
    public static HudAdapter create() {
        if (PluginManager.get() != null && PluginManager.get().getPlugin(MULTIPLE_HUD_ID) != null) {
            MultipleHudAdapter adapter = new MultipleHudAdapter();
            if (adapter.isReady()) {
                LOGGER.atInfo().log("Hexcode HUD bound to MultipleHUD adapter");
                return adapter;
            }
            LOGGER.atWarning().log("MultipleHUD detected but bind failed; using vanilla adapter");
        }
        LOGGER.atInfo().log("Hexcode HUD bound to vanilla adapter");
        return new VanillaHudAdapter();
    }
}
