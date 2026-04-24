package com.riprod.hexcode.builtin.glyphs.isHolding;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexcasterExecutionComponent;

public class IsHoldingValue implements GlyphHandler {

    public static final String ID = "IsHolding";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public HexVar readValue(Glyph glyph, HexContext hexContext) {
        Ref<EntityStore> casterRef = hexContext.getCasterRef();
        if (casterRef == null || !casterRef.isValid() || hexContext.getAccessor() == null) {
            return new NumberVar(0.0);
        }
        HexcasterExecutionComponent comp = hexContext.getAccessor().getComponent(
                casterRef, HexcasterExecutionComponent.getComponentType());
        boolean holding = comp != null && comp.isHoldingPrimary();
        return new NumberVar(holding ? 1.0 : 0.0);
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
