package com.riprod.hexcode.builtin.glyphs.onCast;

import com.riprod.hexcode.builtin.triggers.AbstractTriggerGlyph;
import com.riprod.hexcode.builtin.triggers.TriggerKey;

public class OnCastGlyph extends AbstractTriggerGlyph {

    public static final String ID = "OnCast";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String triggerKey() {
        return TriggerKey.CAST;
    }
}
