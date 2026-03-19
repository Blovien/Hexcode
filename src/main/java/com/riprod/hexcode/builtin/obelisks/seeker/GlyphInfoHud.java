package com.riprod.hexcode.builtin.obelisks.seeker;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class GlyphInfoHud extends CustomUIHud {

    private final String glyphName;

    public GlyphInfoHud(PlayerRef playerRef, String glyphName) {
        super(playerRef);
        this.glyphName = glyphName;
    }

    @Override
    protected void build(UICommandBuilder cmd) {
        cmd.append("Hexcode/Obelisks/GlyphInfo.ui");
        cmd.set("#GlyphName.Text", glyphName);
    }
}
