package com.riprod.hexcode.builtin.obelisks.seeker;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class GlyphInfoHud extends CustomUIHud {

    private final String glyphName;
    private final String description;

    public GlyphInfoHud(PlayerRef playerRef, String glyphName, String description) {
        super(playerRef);
        this.glyphName = glyphName;
        this.description = description;
    }

    @Override
    protected void build(UICommandBuilder cmd) {
        cmd.append("Hexcode/Obelisks/GlyphInfo.ui");
        cmd.set("#GlyphName.Text", glyphName);
        cmd.set("#Description.Text", description);
    }
}
