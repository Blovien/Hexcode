package com.riprod.hexcode.core.common.hud.api;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.riprod.hexcode.core.common.hud.controller.HexcodeHud;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface HudAdapter {

    @Nullable
    HexcodeHud findOwnedHud(@Nonnull Player player, @Nonnull PlayerRef playerRef);

    boolean canInstall(@Nonnull Player player, @Nonnull PlayerRef playerRef);

    void setHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull HexcodeHud hud);
}
