package com.riprod.hexcode.builtin.obelisks.seeker;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class GlyphInfoHud extends CustomUIHud {

    static final String ANCHOR_SELECTOR = "#HexcodeSeekerInfo";
    private static final String ANCHOR_DOC = "Hexcode/Obelisks/SeekerAnchor.ui";
    private static final String PANEL_DOC = "Hexcode/Obelisks/GlyphInfo.ui";

    public GlyphInfoHud(PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(UICommandBuilder cmd) {
        cmd.append(ANCHOR_DOC);
    }

    public void showContent(String title, String description) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.clear(ANCHOR_SELECTOR);
        cmd.append(ANCHOR_SELECTOR, PANEL_DOC);
        cmd.set("#GlyphName.Text", title == null ? "" : title);
        cmd.set("#Description.Text", description == null ? "" : description);
        update(false, cmd);
    }

    public void clearContent() {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.clear(ANCHOR_SELECTOR);
        update(false, cmd);
    }
}
