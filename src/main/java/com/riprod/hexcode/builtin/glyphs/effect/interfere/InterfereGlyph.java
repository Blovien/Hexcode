package com.riprod.hexcode.builtin.glyphs.effect.interfere;

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
import com.riprod.hexcode.builtin.glyphs.effect.interfere.style.InterfereStyle;
import com.riprod.hexcode.core.common.construct.component.HexConstruct;
import com.riprod.hexcode.core.common.effect.HexEffectHandler;
import com.riprod.hexcode.core.common.effect.HexEffectRegistry;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class InterfereGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Interfere";

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;
        float finalCost = baseCost * castMultiplier;

        return hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targetVar = glyph.readSlot("target", hexContext);
        if (targetVar == null) {
            LOGGER.atInfo().log("interfere: no targets");
            Executor.fail(hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        EntityVar entityVar = SpellVarUtil.resolveEntityVar(targetVar, hexContext);
        if (entityVar != null) {
            Ref<EntityStore> ref = entityVar.getRef(accessor);
            if (ref == null || !ref.isValid()) {
                LOGGER.atInfo().log("interfere: invalid target");
                Executor.fail(hexContext);
                return;
            }

            Vector3d pos = resolvePosition(ref, accessor);

            HexConstruct construct = accessor.getComponent(ref, HexConstruct.getComponentType());
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

        BlockVar blockVar = SpellVarUtil.resolveBlockVar(targetVar, hexContext);
        if (blockVar != null) {
            Vector3d blockCenter = SpellVarUtil.resolvePosition(blockVar, accessor);
            if (blockCenter != null) {
                InterfereStyle.renderBlockStrip(blockCenter, hexContext.getColors(), accessor);
            }
        }
    }

    private void hijackConstruct(HexConstruct construct, Glyph glyph, HexContext hexContext) {
        HexContext targetCtx = construct.getHexContext();
        Hex hex = targetCtx.gethex();

        List<String> displaced = new ArrayList<>(construct.getConditionalBranchIds());

        List<String> interfereChildren = glyph.getNextLinks();
        if (interfereChildren == null || interfereChildren.isEmpty()) return;

        Hex sourceHex = hexContext.gethex();
        for (Glyph g : sourceHex.getGlyphs()) {
            hex.put(g.getId(), g);
        }

        construct.setConditionalBranchIds(new ArrayList<>(interfereChildren));

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
            if (g != null && "Glyph_Output".equals(g.getGlyphId())) {
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
