package com.riprod.hexcode.api.event;

import com.hypixel.hytale.event.IEvent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class GlyphFizzleEvent implements IEvent<Void> {

    public enum Reason {
        VOLATILITY_DEPLETED,
        INSUFFICIENT_MANA,
        HANDLER_FAILED,
        MANUALLY_CANCELLED,
        ERROR
    }

    private final Glyph glyph;
    private final Reason reason;
    private final HexContext ctx;

    public GlyphFizzleEvent(Glyph glyph, Reason reason, HexContext ctx) {
        this.glyph = glyph;
        this.reason = reason;
        this.ctx = ctx;
    }

    public Glyph getGlyph() {
        return glyph;
    }

    public Reason getReason() {
        return reason;
    }

    public HexContext getCtx() {
        return ctx;
    }
}
