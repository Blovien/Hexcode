package com.riprod.hexcode.builtin.glyphs.effect.self;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.values.HexValInterface;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class SelfGlyph implements GlyphHandler, HexValInterface {
    public static final String ID = "Glyph_Self";

    private HexVar compute(Glyph glyph, HexContext hexContext) {
        Ref<EntityStore> playerRef = hexContext.getCasterRef();
        if (playerRef == null || !playerRef.isValid()) return null;

        UUIDComponent uuidComponent = hexContext.getAccessor().getComponent(playerRef, UUIDComponent.getComponentType());
        return new EntityVar(EntityVar.createRef(uuidComponent.getUuid(), playerRef));
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar result = compute(glyph, hexContext);

        if (result != null) {
            Integer outputSlot = glyph.resolveOutput("result", hexContext);
            if (outputSlot != null) hexContext.setVariable(outputSlot, result);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }

    @Override
    public HexVar getValue(Glyph glyph, HexContext hexContext) {
        return compute(glyph, hexContext);
    }
}
