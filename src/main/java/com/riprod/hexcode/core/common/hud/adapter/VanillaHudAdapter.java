package com.riprod.hexcode.core.common.hud.adapter;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.riprod.hexcode.core.common.hud.api.HudAdapter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class VanillaHudAdapter implements HudAdapter {

    @Nullable
    @Override
    public CustomUIHud getCurrentHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        return player.getHudManager().getCustomHud();
    }

    @Override
    public void setHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull CustomUIHud hud) {
        player.getHudManager().setCustomHud(playerRef, hud);
    }

    @Override
    public void clearHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        player.getHudManager().setCustomHud(playerRef, null);
    }
}
