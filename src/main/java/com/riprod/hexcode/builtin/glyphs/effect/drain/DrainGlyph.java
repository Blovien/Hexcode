package com.riprod.hexcode.builtin.glyphs.effect.drain;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.drain.component.DrainComponent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.values.HexValInterface;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.utils.SpellVarUtil;

public class DrainGlyph implements GlyphHandler, HexValInterface {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Drain";

    private static final float HP_TO_MANA_RATE = 1.5f;
    private static final float STAMINA_TO_MANA_RATE = 0.6f;
    private static final float DEFAULT_DURATION = 1.0f;

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targetVar = glyph.resolveInput("target", hexContext);
        if (!(targetVar instanceof EntityVar entityVar)) {
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        Ref<EntityStore> targetRef = entityVar.getRef(hexContext.getAccessor());
        if (targetRef == null || !targetRef.isValid()) {
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        HexVar hpInput = glyph.resolveInput("hp", hexContext);
        HexVar staminaInput = glyph.resolveInput("stamina", hexContext);

        int sourceStatIndex;
        float conversionRate;
        double drainPercent;

        if (hpInput != null) {
            sourceStatIndex = DefaultEntityStatTypes.getHealth();
            conversionRate = HP_TO_MANA_RATE;
            drainPercent = SpellVarUtil.resolveNumberOrDefault(hpInput, 0.0);
        } else if (staminaInput != null) {
            sourceStatIndex = DefaultEntityStatTypes.getStamina();
            conversionRate = STAMINA_TO_MANA_RATE;
            drainPercent = SpellVarUtil.resolveNumberOrDefault(staminaInput, 0.0);
        } else {
            // default to using health if no specific stat is provided
            sourceStatIndex = DefaultEntityStatTypes.getHealth();
            conversionRate = HP_TO_MANA_RATE;
            drainPercent = 15.0f;
        }

        if (drainPercent <= 0) {
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        EntityStatMap statMap = hexContext.getAccessor().getComponent(targetRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        EntityStatValue sourceStat = statMap.get(sourceStatIndex);
        if (sourceStat == null) {
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        float totalDrainAmount = (float) (drainPercent / 100.0) * sourceStat.getMax();

        // clamp so we don't drain below 1 hp
        if (sourceStatIndex == DefaultEntityStatTypes.getHealth()) {
            float maxDrainable = sourceStat.get() - 1.0f;
            if (maxDrainable <= 0) {
                Executor.continueExecution(glyph.getNext(), hexContext);
                return;
            }
            totalDrainAmount = Math.min(totalDrainAmount, maxDrainable);
        } else {
            totalDrainAmount = Math.min(totalDrainAmount, sourceStat.get());
        }

        if (totalDrainAmount <= 0) {
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        HexVar durationVar = glyph.resolveInput("duration", hexContext);
        float duration = DEFAULT_DURATION;
        if (durationVar != null) {
            duration = Math.max(0.01f, SpellVarUtil.resolveNumberOrDefault(durationVar, (double) DEFAULT_DURATION).floatValue());
        }

        HexColors colors = hexContext.getColors();
        List<String> nextGlyphIds = new ArrayList<>(glyph.getNext());
        Ref<EntityStore> hexEntityRef = hexContext.getRoot().getRootEntityRef();

        DrainComponent.DrainEntry entry = new DrainComponent.DrainEntry(
                sourceStatIndex, conversionRate, totalDrainAmount, duration,
                hexContext.copy(), nextGlyphIds, hexEntityRef, colors);

        DrainComponent existing = hexContext.getAccessor().getComponent(
                targetRef, DrainComponent.getComponentType());
        if (existing != null) {
            existing.addEntry(entry);
        } else {
            DrainComponent component = new DrainComponent();
            component.addEntry(entry);
            hexContext.getAccessor().addComponent(
                    targetRef, DrainComponent.getComponentType(), component);
        }

        RootGlyph rootGlyph = hexContext.getAccessor().getComponent(
                hexEntityRef, RootGlyph.getComponentType());
        if (rootGlyph != null) {
            rootGlyph.incrementExternalWaiters();
        }
    }

    @Override
    public HexVar getValue(Glyph glyph, HexContext hexContext) {
        HexVar targetVar = glyph.resolveInput("target", hexContext);
        if (!(targetVar instanceof EntityVar entityVar)) {
            return new NumberVar(0);
        }

        Ref<EntityStore> targetRef = entityVar.getRef(hexContext.getAccessor());
        if (targetRef == null || !targetRef.isValid()) {
            return new NumberVar(0);
        }

        EntityStatMap statMap = hexContext.getAccessor().getComponent(targetRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return new NumberVar(0);
        }

        HexVar hpInput = glyph.resolveInput("hp", hexContext);
        HexVar staminaInput = glyph.resolveInput("stamina", hexContext);

        int statIndex;
        if (hpInput != null) {
            statIndex = DefaultEntityStatTypes.getHealth();
        } else if (staminaInput != null) {
            statIndex = DefaultEntityStatTypes.getStamina();
        } else {
            statIndex = DefaultEntityStatTypes.getMana();
        }

        EntityStatValue stat = statMap.get(statIndex);
        if (stat == null || stat.getMax() == 0) {
            return new NumberVar(0);
        }

        double fillPercent = (stat.get() / stat.getMax()) * 100.0;
        return new NumberVar(fillPercent);
    }
}
