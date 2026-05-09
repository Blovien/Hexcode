package com.riprod.hexcode.builtin.glyphs.onUse;

import com.riprod.hexcode.builtin.triggers.AbstractTriggerGlyph;
import com.riprod.hexcode.builtin.triggers.TriggerKey;

public class OnUseGlyph extends AbstractTriggerGlyph {

    public static final String ID = "OnUse";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String triggerKey() {
        return TriggerKey.USE;
    }
}
