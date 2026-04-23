package com.riprod.hexcode.builtin.glyphs.resonate;

import java.util.List;
import java.util.Map;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.resonate.style.ResonateStyle;
import com.riprod.hexcode.core.common.construct.component.HexEffectsComponent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class ResonateGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
public String getId() { return ID; };

public static final String ID = "Resonate";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targetVar = glyph.readSlot(ResonateGlyphSlots.TARGET, hexContext);
        EntityVar entityVar = SpellVarUtil.resolveEntityVar(targetVar, hexContext);
        if (entityVar == null) {
            LOGGER.atInfo().log("resonate: targets must be entities");
            HexExecuter.fail(hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        Ref<EntityStore> ref = entityVar.getRef(accessor);
        if (ref == null || !ref.isValid()) {
            HexExecuter.fail(hexContext);
            return;
        }

        HexEffectsComponent construct = accessor.getComponent(ref, HexEffectsComponent.getComponentType());
        if (construct != null) {
            resonateConstruct(glyph, hexContext, construct, ref, accessor);
        } else {
            resonateEntity(glyph, hexContext, ref, accessor);
        }
    }

    private void resonateConstruct(Glyph glyph, HexContext hexContext,
            HexEffectsComponent construct, Ref<EntityStore> ref,
            CommandBuffer<EntityStore> accessor) {

        List<String> children = glyph.getNextLinks();
        if (children != null && !children.isEmpty()) {
            HexContext targetCtx = construct.getHexContext();
            Hex hex = targetCtx.gethex();

            Hex sourceHex = hexContext.gethex();
            for (Glyph g : sourceHex.getGlyphs()) {
                hex.put(g.getId(), g);
            }

            construct.getConditionalBranchIds().addAll(children);

            for (Map.Entry<String, HexVar> entry : hexContext.getVariables().entrySet()) {
                targetCtx.getVariables().putIfAbsent(entry.getKey(), entry.getValue());
            }

            VolatilityTracker targetTracker = targetCtx.getVolatilityTracker();
            if (targetTracker != null) {
                targetTracker.consumeVolatility(children.size());
            }

            LOGGER.atInfo().log("resonate: injected %d glyphs into construct", children.size());
        }

        transferManaToRoot(glyph, hexContext, construct.getHexContext().getRoot(), accessor);

        Vector3d pos = resolvePosition(ref, accessor);
        if (pos != null) ResonateStyle.renderResonate(pos, hexContext.getColors(), accessor);
    }

    private void resonateEntity(Glyph glyph, HexContext hexContext,
            Ref<EntityStore> ref, CommandBuffer<EntityStore> accessor) {

        EntityStatMap targetStats = accessor.getComponent(ref, EntityStatMap.getComponentType());
        if (targetStats == null) {
            Vector3d pos = resolvePosition(ref, accessor);
            if (pos != null) ResonateStyle.renderNoSignal(pos, hexContext.getColors(), accessor);
            LOGGER.atInfo().log("resonate: target has no mana pool");
            HexExecuter.fail(hexContext);
            return;
        }

        HexVar manaVar = glyph.readSlot(ResonateGlyphSlots.MANA, hexContext);
        double percentage = SpellVarUtil.resolveNumberOrDefault(manaVar, 0.0);
        if (percentage <= 0) {
            LOGGER.atInfo().log("resonate: no mana percentage specified for entity transfer");
            HexExecuter.fail(hexContext);
            return;
        }
        percentage = Math.min(percentage, 100.0);

        HexRoot casterRoot = hexContext.getRoot();
        float casterMana = casterRoot.getCurrentMana(accessor);
        float transferAmount = (float) (casterMana * (percentage / 100.0));

        int manaIndex = DefaultEntityStatTypes.getMana();
        float targetCurrent = targetStats.get(manaIndex).get();
        float targetMax = targetStats.get(manaIndex).getMax();
        float room = targetMax - targetCurrent;

        transferAmount = Math.min(transferAmount, room);
        if (transferAmount <= 0) {
            LOGGER.atInfo().log("resonate: target mana is full");
            return;
        }

        if (!casterRoot.tryConsumeMana(transferAmount, accessor)) return;
        targetStats.addStatValue(manaIndex, transferAmount);

        Vector3d pos = resolvePosition(ref, accessor);
        if (pos != null) ResonateStyle.renderResonate(pos, hexContext.getColors(), accessor);
        LOGGER.atInfo().log("resonate: transferred %.1f mana to entity", transferAmount);
    }

    private void transferManaToRoot(Glyph glyph, HexContext hexContext,
            HexRoot targetRoot, CommandBuffer<EntityStore> accessor) {

        HexVar manaVar = glyph.readSlot(ResonateGlyphSlots.MANA, hexContext);
        double percentage = SpellVarUtil.resolveNumberOrDefault(manaVar, 0.0);
        if (percentage <= 0) return;
        percentage = Math.min(percentage, 100.0);

        HexRoot casterRoot = hexContext.getRoot();
        float casterMana = casterRoot.getCurrentMana(accessor);
        float transferAmount = (float) (casterMana * (percentage / 100.0));
        if (transferAmount <= 0) return;

        if (!casterRoot.tryConsumeMana(transferAmount, accessor)) return;
        targetRoot.addMana(transferAmount, accessor);

        LOGGER.atInfo().log("resonate: transferred %.1f mana to construct owner", transferAmount);
    }

    private Vector3d resolvePosition(Ref<EntityStore> ref, CommandBuffer<EntityStore> accessor) {
        TransformComponent tc = accessor.getComponent(ref, TransformComponent.getComponentType());
        return tc != null ? tc.getPosition() : null;
    }
}
