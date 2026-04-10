package com.riprod.hexcode.builtin.glyphs.effect.scale;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.scale.component.ScaleComponent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class ScaleGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final double DEFAULT_MAGNITUDE = 2.0;
    private static final double MIN_MAGNITUDE = 0.25;
    private static final double MAX_MAGNITUDE = 4.0;
    private static final double DEFAULT_DURATION = 5.0;

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.readSlot("target", hexContext);
        EntityVar entityVar = SpellVarUtil.resolveEntityVar(targets, hexContext);
        if (entityVar == null) {
            Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        Ref<EntityStore> ref = entityVar.getRef(accessor);
        if (ref == null || !ref.isValid()) {
            Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        double magnitude = Math.max(MIN_MAGNITUDE, Math.min(MAX_MAGNITUDE,
                SpellVarUtil.resolveNumberOrDefault(
                        glyph.readSlot("magnitude", hexContext), DEFAULT_MAGNITUDE)));
        double duration = Math.max(0.1, SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("duration", hexContext), DEFAULT_DURATION));

        try {
            EntityScaleComponent scaleComp = accessor.getComponent(ref,
                    EntityScaleComponent.getComponentType());
            BoundingBox box = accessor.getComponent(ref, BoundingBox.getComponentType());

            ScaleComponent existing = accessor.getComponent(ref, ScaleComponent.getComponentType());
            float originalScale = existing != null
                    ? existing.getOriginalScale()
                    : (scaleComp != null ? scaleComp.getScale() : 1.0f);
            Box originalBox = existing != null
                    ? existing.getOriginalBoundingBox()
                    : (box != null ? new Box(box.getBoundingBox()) : null);

            if (scaleComp != null) {
                scaleComp.setScale(originalScale * (float) magnitude);
            }
            if (box != null && originalBox != null) {
                Box scaled = new Box(originalBox).scale((float) magnitude);
                box.setBoundingBox(scaled);
            }

            if (existing != null) {
                accessor.removeComponent(ref, ScaleComponent.getComponentType());
            }
            accessor.addComponent(ref, ScaleComponent.getComponentType(),
                    new ScaleComponent(originalScale, originalBox, (float) duration));
        } catch (Exception e) {
            LOGGER.atWarning().log("scale: could not apply: %s", e.getMessage());
        }

        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
