package com.riprod.hexcode.core.common.hud.adapter;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.riprod.hexcode.core.common.hud.api.HudAdapter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;

public final class MultipleHudAdapter implements HudAdapter {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HUD_ID = "HexcodeHud";
    private static final String MHUD_CLASS = "com.buuz135.mhud.MultipleHUD";

    @Nullable
    private final Object mhudInstance;
    @Nullable
    private final Method setCustomHud;
    @Nullable
    private final Method hideCustomHud;

    public MultipleHudAdapter() {
        Object instance = null;
        Method setMethod = null;
        Method hideMethod = null;
        try {
            Class<?> mhudClass = Class.forName(MHUD_CLASS);
            instance = mhudClass.getMethod("getInstance").invoke(null);
            setMethod = mhudClass.getMethod("setCustomHud", Player.class, PlayerRef.class, String.class, CustomUIHud.class);
            hideMethod = mhudClass.getMethod("hideCustomHud", Player.class, PlayerRef.class, String.class);
        } catch (ReflectiveOperationException e) {
            LOGGER.atSevere().withCause(e).log("Failed to bind MultipleHUD adapter; falling back to inert state");
        }
        this.mhudInstance = instance;
        this.setCustomHud = setMethod;
        this.hideCustomHud = hideMethod;
    }

    public boolean isReady() {
        return mhudInstance != null && setCustomHud != null && hideCustomHud != null;
    }

    @Nullable
    @Override
    public CustomUIHud getCurrentHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        return player.getHudManager().getCustomHud();
    }

    @Override
    public void setHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull CustomUIHud hud) {
        if (!isReady()) return;
        try {
            setCustomHud.invoke(mhudInstance, player, playerRef, HUD_ID, hud);
        } catch (ReflectiveOperationException e) {
            LOGGER.atSevere().withCause(e).log("MultipleHUD setCustomHud failed");
        }
    }

    @Override
    public void clearHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        if (!isReady()) return;
        try {
            hideCustomHud.invoke(mhudInstance, player, playerRef, HUD_ID);
        } catch (ReflectiveOperationException e) {
            LOGGER.atSevere().withCause(e).log("MultipleHUD hideCustomHud failed");
        }
    }
}
