package com.riprod.hexcode.core.common.hud.adapter;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.riprod.hexcode.core.common.hud.api.HudAdapter;
import com.riprod.hexcode.core.common.hud.controller.HexcodeHud;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class VanillaHudAdapter implements HudAdapter {

    @Nullable
    @Override
    public HexcodeHud findOwnedHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        CustomUIHud current = player.getHudManager().getCustomHud();
        return current instanceof HexcodeHud h ? h : null;
    }

    @Override
    public boolean canInstall(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        CustomUIHud current = player.getHudManager().getCustomHud();
        return current == null || current instanceof HexcodeHud;
    }

    @Override
    public void setHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull HexcodeHud hud) {
        player.getHudManager().setCustomHud(playerRef, hud);
    }
}
