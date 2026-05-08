package com.riprod.hexcode.builtin.glyphs.onSecondary;

import com.riprod.hexcode.builtin.triggers.AbstractTriggerGlyph;
import com.riprod.hexcode.builtin.triggers.TriggerKey;

public class OnSecondaryGlyph extends AbstractTriggerGlyph {

    public static final String ID = "OnSecondary";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String triggerKey() {
        return TriggerKey.SECONDARY;
    }
}
