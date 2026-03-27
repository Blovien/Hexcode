package com.riprod.hexcode.builtin.glyphs.effect.interfere;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.erode.component.ErodeComponent;
import com.riprod.hexcode.builtin.glyphs.effect.fortify.component.FortifyComponent;
import com.riprod.hexcode.builtin.glyphs.effect.interfere.style.InterfereStyle;
import com.riprod.hexcode.builtin.glyphs.effect.levitate.component.LevitateComponent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class InterfereGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Interfere";

    private static final String FORTIFY_EFFECT_ID = "Hexcode_Fortify";
    private static final String ERODE_EFFECT_ID = "Hexcode_Erode";
    private static final String LEVITATE_EFFECT_ID = "Hexcode_Levitate";
    private static final float HIJACK_COST_MULTIPLIER = 2.0f;

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        HexVar targets = glyph.resolveInput("target", hexContext);
        int targetCount = (targets != null) ? Math.max(1, targets.size()) : 1;

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;
        float finalCost = baseCost * castMultiplier * targetCount;

        return hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targetVar = glyph.resolveInput("target", hexContext);
        if (targetVar == null || targetVar.size() == 0) {
            LOGGER.atInfo().log("interfere: no targets");
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        if (targetVar instanceof EntityVar entityVar) {
            for (int i = 0; i < entityVar.size(); i++) {
                Ref<EntityStore> ref = entityVar.getRef(i, accessor);
                if (ref == null || !ref.isValid()) continue;

                Vector3d pos = resolvePosition(ref, accessor);

                HexSignal signal = accessor.getComponent(ref, HexSignal.getComponentType());
                if (signal != null && signal.getPrimary() != null) {
                    hijackSignal(signal, glyph, hexContext);
                    if (pos != null) InterfereStyle.renderHijack(pos, hexContext.getColors(), accessor);
                    LOGGER.atInfo().log("interfere: hijacked hex signal");
                    continue;
                }

                int stripped = stripBuffs(ref, accessor);
                if (stripped > 0 && pos != null) {
                    InterfereStyle.renderStrip(pos, hexContext.getColors(), accessor);
                }
                LOGGER.atInfo().log("interfere: stripped %d effects", stripped);
            }
        }

        if (targetVar instanceof BlockVar blockVar) {
            for (int i = 0; i < blockVar.size(); i++) {
                Vector3d blockCenter = SpellVarUtil.resolvePositionAt(targetVar, i, accessor);
                if (blockCenter != null) {
                    InterfereStyle.renderBlockStrip(blockCenter, hexContext.getColors(), accessor);
                }
            }
        }

        // interfere does not call continueExecution — children are the injected payload
    }

    private void hijackSignal(HexSignal signal, Glyph glyph, HexContext hexContext) {
        // replace all entries' glyph chains with the interfere caster's children
        // hexEntityRef stays pointing to the original caster → they pay mana (sabotage)
        signal.replaceAll(hexContext.copy(), glyph.getNext());
    }

    private int stripBuffs(Ref<EntityStore> ref, CommandBuffer<EntityStore> accessor) {
        int count = 0;

        if (accessor.getComponent(ref, FortifyComponent.getComponentType()) != null) {
            removeEntityEffect(ref, FORTIFY_EFFECT_ID, accessor);
            accessor.removeComponent(ref, FortifyComponent.getComponentType());
            count++;
        }

        if (accessor.getComponent(ref, ErodeComponent.getComponentType()) != null) {
            removeEntityEffect(ref, ERODE_EFFECT_ID, accessor);
            accessor.removeComponent(ref, ErodeComponent.getComponentType());
            count++;
        }

        if (accessor.getComponent(ref, LevitateComponent.getComponentType()) != null) {
            removeEntityEffect(ref, LEVITATE_EFFECT_ID, accessor);
            accessor.removeComponent(ref, LevitateComponent.getComponentType());
            count++;
        }

        return count;
    }

    private void removeEntityEffect(Ref<EntityStore> ref, String effectId,
            CommandBuffer<EntityStore> accessor) {
        EffectControllerComponent controller = accessor.getComponent(
                ref, EffectControllerComponent.getComponentType());
        if (controller == null) return;

        int effectIndex = EntityEffect.getAssetMap().getIndex(effectId);
        if (effectIndex != Integer.MIN_VALUE) {
            controller.removeEffect(ref, effectIndex, accessor);
        }
    }

    private Vector3d resolvePosition(Ref<EntityStore> ref, CommandBuffer<EntityStore> accessor) {
        TransformComponent tc = accessor.getComponent(ref, TransformComponent.getComponentType());
        return tc != null ? tc.getPosition() : null;
    }
}
