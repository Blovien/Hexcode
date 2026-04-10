package com.riprod.hexcode.builtin.glyphs.effect.self;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class SelfGlyph implements GlyphHandler {
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
            glyph.writeSlot("result", result, hexContext);
        }

        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }

    @Override
    public boolean canReadValue() {
        return true;
    }

    @Override
    public HexVar readValue(Glyph glyph, HexContext hexContext) {
        return compute(glyph, hexContext);
    }
}
