package com.riprod.hexcode.builtin.glyphs.onPrimary;

import com.riprod.hexcode.builtin.triggers.AbstractTriggerGlyph;
import com.riprod.hexcode.builtin.triggers.TriggerKey;

public class OnPrimaryGlyph extends AbstractTriggerGlyph {

    public static final String ID = "OnPrimary";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String triggerKey() {
        return TriggerKey.PRIMARY;
    }
}
