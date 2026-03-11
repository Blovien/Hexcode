package com.riprod.hexcode.builtin.glyphs.effect.self;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class SelfGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Self";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        Integer setSlot = glyph.resolveOutput("result", hexContext);
        Ref<EntityStore> playerRef = hexContext.getCasterRef();

        if (setSlot != null && playerRef != null && playerRef.isValid()) {
            UUIDComponent uuidComponent = hexContext.getAccessor().getComponent(playerRef, UUIDComponent.getComponentType());
            EntityVar selfVar = new EntityVar(EntityVar.createRef(uuidComponent.getUuid(), playerRef));
            hexContext.setVariable(setSlot, selfVar);
        } else {
            LOGGER.atWarning().log("self glyph: invalid caster reference or no output slot");
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
