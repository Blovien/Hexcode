package com.riprod.hexcode.builtin.glyphs.interfere;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.interfere.style.InterfereStyle;
import com.riprod.hexcode.builtin.utils.ConstructSplicer;
import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexEffectsComponent;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;
import com.riprod.hexcode.core.common.construct.registry.ConstructRegistry;
import com.riprod.hexcode.core.common.effect.HexEffectHandler;
import com.riprod.hexcode.core.common.effect.HexEffectRegistry;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.HexVarUtil;

public class InterfereGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "Interfere";

    @Override
    public String getId() { return ID; }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targetVar = glyph.readSlot(InterfereGlyphSlots.TARGET, hexContext);
        if (targetVar == null) {
            HexExecuter.fail(hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        EntityVar entityVar = HexVarUtil.resolveEntityVar(targetVar, hexContext);
        if (entityVar == null) return;

        Ref<EntityStore> ref = entityVar.getRef(accessor);
        if (ref == null || !ref.isValid()) {
            HexExecuter.fail(hexContext);
            return;
        }

        Vector3d pos = resolvePosition(ref, accessor);

        HexEffectsComponent construct = accessor.getComponent(ref, HexEffectsComponent.getComponentType());
        if (construct != null) {
            hijackConstruct(construct, ref, glyph, hexContext, accessor);
            if (pos != null) InterfereStyle.renderHijack(pos, hexContext.getColors(), accessor);
        } else {
            int stripped = stripEffects(ref, accessor);
            if (stripped > 0 && pos != null) {
                InterfereStyle.renderStrip(pos, hexContext.getColors(), accessor);
            }
            LOGGER.atInfo().log("interfere: stripped %d effects", stripped);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void hijackConstruct(HexEffectsComponent construct, Ref<EntityStore> ref,
            Glyph glyph, HexContext hexContext, CommandBuffer<EntityStore> accessor) {

        List<HexStatus<?>> targets = new ArrayList<>(construct.getEffects().values());
        if (targets.isEmpty()) return;

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float donation = tracker != null ? tracker.getRemainingBudget() : 0f;

        ConstructTickContext ctx = new ConstructTickContext(accessor, ref);

        for (HexStatus<?> target : targets) {
            ConstructHandler<?> handler = ConstructRegistry.get(target.getHandlerId());
            if (handler == null) continue;

            ConstructSplicer.splice(target, handler, hexContext, glyph,
                    ConstructSplicer.ChainMode.REPLACE,
                    ConstructSplicer.VariablePolicy.PREFER_CASTER,
                    donation);

            ConstructHandler raw = handler;
            HexStatus rawStatus = target;
            try {
                raw.onEnd(rawStatus, ctx);
            } catch (Exception e) {
                LOGGER.atWarning().log("interfere: onEnd failed for '%s': %s",
                        target.getHandlerId(), e.getMessage());
            }

            construct.removeEffect(target.getConstructId());
        }

        if (tracker != null && donation > 0f) {
            tracker.consumeVolatility(donation);
        }

        LOGGER.atInfo().log("interfere: hijacked %d active effects on target", targets.size());
    }

    private int stripEffects(Ref<EntityStore> ref, CommandBuffer<EntityStore> accessor) {
        int count = 0;
        for (HexEffectHandler handler : HexEffectRegistry.getAll().values()) {
            if (handler.isPresent(accessor, ref)) {
                handler.strip(accessor, ref);
                count++;
            }
        }
        return count;
    }

    private Vector3d resolvePosition(Ref<EntityStore> ref, CommandBuffer<EntityStore> accessor) {
        TransformComponent tc = accessor.getComponent(ref, TransformComponent.getComponentType());
        return tc != null ? tc.getPosition() : null;
    }
}
