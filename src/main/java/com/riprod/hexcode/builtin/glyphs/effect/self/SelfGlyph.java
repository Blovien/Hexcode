package com.riprod.hexcode.builtin.glyphs.effect.self;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;

public class SelfGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Self";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        int setSlot = glyph.getOutput(0, hexContext);
        Ref<EntityStore> playerRef = hexContext.getCasterRef();

        if (playerRef != null && playerRef.isValid()) {
            UUIDComponent uuidComponent = hexContext.getAccessor().getComponent(playerRef, UUIDComponent.getComponentType());
            EntityVar selfVar = new EntityVar(EntityVar.createRef(uuidComponent.getUuid(), playerRef));
            hexContext.setVariable(setSlot, selfVar);
        } else {
            LOGGER.atWarning().log("self glyph: invalid caster reference");
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
