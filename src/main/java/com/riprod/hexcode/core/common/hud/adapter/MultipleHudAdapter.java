package com.riprod.hexcode.core.common.hud.adapter;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.riprod.hexcode.core.common.hud.api.HudAdapter;
import com.riprod.hexcode.core.common.hud.controller.HexcodeHud;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;

public final class MultipleHudAdapter implements HudAdapter {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HUD_ID = "HexcodeHud";
    private static final String MHUD_CLASS = "com.buuz135.mhud.MultipleHUD";
    private static final String MCHUD_CLASS = "com.buuz135.mhud.MultipleCustomUIHud";

    @Nullable
    private final Object mhudInstance;
    @Nullable
    private final Method setCustomHud;
    @Nullable
    private final Class<?> mchudClass;
    @Nullable
    private final Method mchudGet;

    public MultipleHudAdapter() {
        Object instance = null;
        Method setMethod = null;
        Class<?> mchud = null;
        Method getMethod = null;
        try {
            Class<?> mhudClass = Class.forName(MHUD_CLASS);
            instance = mhudClass.getMethod("getInstance").invoke(null);
            setMethod = mhudClass.getMethod("setCustomHud", Player.class, PlayerRef.class, String.class, CustomUIHud.class);
            mchud = Class.forName(MCHUD_CLASS);
            getMethod = mchud.getMethod("get", String.class);
        } catch (ReflectiveOperationException e) {
            LOGGER.atSevere().withCause(e).log("Failed to bind MultipleHUD adapter; falling back to inert state");
        }
        this.mhudInstance = instance;
        this.setCustomHud = setMethod;
        this.mchudClass = mchud;
        this.mchudGet = getMethod;
    }

    public boolean isReady() {
        return mhudInstance != null && setCustomHud != null && mchudClass != null && mchudGet != null;
    }

    @Nullable
    @Override
    public HexcodeHud findOwnedHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        if (!isReady()) return null;
        CustomUIHud current = player.getHudManager().getCustomHud();
        if (current instanceof HexcodeHud h) return h;
        if (!mchudClass.isInstance(current)) return null;
        try {
            Object child = mchudGet.invoke(current, HUD_ID);
            return child instanceof HexcodeHud h ? h : null;
        } catch (ReflectiveOperationException e) {
            LOGGER.atSevere().withCause(e).log("MultipleCustomUIHud.get failed");
            return null;
        }
    }

    @Override
    public boolean canInstall(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        return isReady();
    }

    @Override
    public void setHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull HexcodeHud hud) {
        if (!isReady()) return;
        try {
            setCustomHud.invoke(mhudInstance, player, playerRef, HUD_ID, hud);
        } catch (ReflectiveOperationException e) {
            LOGGER.atSevere().withCause(e).log("MultipleHUD setCustomHud failed");
        }
    }
}
