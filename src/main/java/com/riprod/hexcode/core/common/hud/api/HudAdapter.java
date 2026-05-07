package com.riprod.hexcode.core.common.hud.api;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface HudAdapter {

    @Nullable
    CustomUIHud getCurrentHud(@Nonnull Player player, @Nonnull PlayerRef playerRef);

    void setHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull CustomUIHud hud);

    void clearHud(@Nonnull Player player, @Nonnull PlayerRef playerRef);
}
