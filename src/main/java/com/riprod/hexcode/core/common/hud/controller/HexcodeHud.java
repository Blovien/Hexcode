package com.riprod.hexcode.core.common.hud.controller;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class HexcodeHud extends CustomUIHud {

    public static final String SHELL_DOC = "Hexcode/Hud/Shell.ui";

    public HexcodeHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder cmd) {
        cmd.append(SHELL_DOC);
    }

    public void apply(@Nonnull UICommandBuilder cmd) {
        update(false, cmd);
    }
}
