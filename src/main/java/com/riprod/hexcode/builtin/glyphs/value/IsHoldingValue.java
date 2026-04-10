package com.riprod.hexcode.builtin.glyphs.value;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class IsHoldingValue implements GlyphHandler {

    @Override
    public boolean canReadValue() {
        return true;
    }

    @Override
    public HexVar readValue(Glyph glyph, HexContext hexContext) {
        Ref<EntityStore> casterRef = hexContext.getCasterRef();
        if (casterRef == null || !casterRef.isValid() || hexContext.getAccessor() == null) {
            return new NumberVar(0.0);
        }
        HexcasterComponent comp = hexContext.getAccessor().getComponent(
                casterRef, HexcasterComponent.getComponentType());
        boolean holding = comp != null && comp.isHoldingPrimary();
        return new NumberVar(holding ? 1.0 : 0.0);
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
