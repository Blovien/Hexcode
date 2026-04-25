package com.riprod.hexcode.builtin.glyphs.interfere;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.interfere.style.InterfereStyle;
import com.riprod.hexcode.core.common.construct.component.HexEffectsComponent;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.effect.HexEffectHandler;
import com.riprod.hexcode.core.common.effect.HexEffectRegistry;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.HexDirectionUtil;
import com.riprod.hexcode.utils.HexVarUtil;

public class InterfereGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
public String getId() { return ID; };

public static final String ID = "Interfere";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targetVar = glyph.readSlot(InterfereGlyphSlots.TARGET, hexContext);
        if (targetVar == null) {
            LOGGER.atInfo().log("interfere: no targets");
            HexExecuter.fail(hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        EntityVar entityVar = HexVarUtil.resolveEntityVar(targetVar, hexContext);
        if (entityVar != null) {
            Ref<EntityStore> ref = entityVar.getRef(accessor);
            if (ref == null || !ref.isValid()) {
                LOGGER.atInfo().log("interfere: invalid target");
                HexExecuter.fail(hexContext);
                return;
            }

            Vector3d pos = resolvePosition(ref, accessor);

            HexEffectsComponent construct = accessor.getComponent(ref, HexEffectsComponent.getComponentType());
            if (construct != null) {
                hijackConstruct(construct, glyph, hexContext);
                if (pos != null) InterfereStyle.renderHijack(pos, hexContext.getColors(), accessor);
                LOGGER.atInfo().log("interfere: hijacked construct");
            } else {
                int stripped = stripEffects(ref, accessor);
                if (stripped > 0 && pos != null) {
                    InterfereStyle.renderStrip(pos, hexContext.getColors(), accessor);
                }
                LOGGER.atInfo().log("interfere: stripped %d effects", stripped);
            }
        }

        BlockVar blockVar = HexVarUtil.resolveBlockVar(targetVar, hexContext);
        if (blockVar != null) {
            Vector3d blockCenter = HexVarUtil.position(blockVar, accessor);
            if (blockCenter != null) {
                InterfereStyle.renderBlockStrip(blockCenter, hexContext.getColors(), accessor);
            }
        }
    }

    private void hijackConstruct(HexEffectsComponent construct, Glyph glyph, HexContext hexContext) {
        // TODO: PHASE_2 - interfere should target a specific HexStatus; currently uses first
        HexStatus<?> status = construct.getEffects().values().stream().findFirst().orElse(null);
        if (status == null) return;

        HexContext targetCtx = status.getHexContext();
        Hex hex = targetCtx.gethex();

        // TODO: PHASE_2 - conditional branch ids no longer exist; use triggering glyph's next-links as displaced set
        Glyph trigger = status.getTriggeringGlyph();
        List<String> displaced = trigger != null && trigger.getNextLinks() != null
                ? new ArrayList<>(trigger.getNextLinks())
                : new ArrayList<>();

        List<String> interfereChildren = glyph.getNextLinks();
        if (interfereChildren == null || interfereChildren.isEmpty()) return;

        Hex sourceHex = hexContext.gethex();
        for (Glyph g : sourceHex.getGlyphs()) {
            hex.put(g.getId(), g);
        }

        // TODO: PHASE_2 - no setConditionalBranchIds equivalent; hijack semantics disabled

        if (!displaced.isEmpty()) {
            Glyph outputGlyph = findOutputGlyph(interfereChildren, hexContext);
            if (outputGlyph != null) {
                for (String id : displaced) {
                    outputGlyph.addSlotLink(Glyph.NEXT_SLOT, id);
                }
            }
        }

        // for (Map.Entry<String, HexVar> entry : hexContext.getVariables().entrySet()) {
        //     targetCtx.setVariable(entry.getKey(), entry.getValue());
        // }
    }

    private Glyph findOutputGlyph(List<String> childIds, HexContext hexContext) {
        for (String id : childIds) {
            Glyph g = hexContext.getGlyph(id);
            if (g != null && "Output".equals(g.getGlyphId())) {
                return g;
            }
        }
        return null;
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
