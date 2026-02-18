package com.riprod.hexcode.builtin.glyphs.self;

import com.riprod.hexcode.builtin.glyphs.seek.SeekGlyphStyle;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.utils.SpellVarUtil;
import com.riprod.hexcode.core.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

import java.util.List;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.components.Glyph;

public class SelfGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Self";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        int setSlot = glyph.getVariable(1);
        Ref<EntityStore> playerRef = hexContext.casterRef;

        if (playerRef != null && playerRef.isValid()) {
            UUIDComponent uuidComponent = hexContext.accessor.getComponent(playerRef, UUIDComponent.getComponentType());
            EntityVar selfVar = new EntityVar(uuidComponent.getUuid(), playerRef);
            // set the self variable to the caster's entity reference
            executionContext.setVariable(setSlot, List.of(selfVar));
        } else {
            LOGGER.atWarning().log("self glyph: invalid caster reference");
        }

        Executor.continueExecution(hexContext, executionContext);
    }
}
