package com.riprod.hexcode.builtin.glyphs.drain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.drain.component.DrainComponent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.component.Slot;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.utils.SpellVarUtil;

public class DrainGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
public String getId() { return ID; };

public static final String ID = "Drain";

    private static final float HP_TO_MANA_RATE = 1.5f;
    private static final float STAMINA_TO_MANA_RATE = 0.6f;
    private static final float DEFAULT_DURATION = 1.0f;

    private static float conversionRate(int sourceStat) {
        // destination is always Mana — drain is always <other> -> mana, never the reverse
        if (sourceStat == DefaultEntityStatTypes.getHealth()) return HP_TO_MANA_RATE;
        if (sourceStat == DefaultEntityStatTypes.getStamina()) return STAMINA_TO_MANA_RATE;
        return 1.0f;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targetVar = glyph.readSlot(DrainGlyphSlots.TARGET, hexContext);
        EntityVar entityVar = SpellVarUtil.resolveEntityVar(targetVar, hexContext);
        if (entityVar == null) {
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        Ref<EntityStore> targetRef = entityVar.getRef(hexContext.getAccessor());
        if (targetRef == null || !targetRef.isValid()) {
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        HexVar hpInput = glyph.readSlot(DrainGlyphSlots.HP, hexContext);
        HexVar staminaInput = glyph.readSlot(DrainGlyphSlots.STAMINA, hexContext);

        int sourceStatIndex;
        double drainPercent;

        if (hpInput != null) {
            sourceStatIndex = DefaultEntityStatTypes.getHealth();
            drainPercent = SpellVarUtil.resolveNumberOrDefault(hpInput, 0.0);
        } else if (staminaInput != null) {
            sourceStatIndex = DefaultEntityStatTypes.getStamina();
            drainPercent = SpellVarUtil.resolveNumberOrDefault(staminaInput, 0.0);
        } else {
            sourceStatIndex = DefaultEntityStatTypes.getHealth();
            drainPercent = 15.0f;
        }

        // HP-source gate: only allow HP drain if the source IS the caster.
        // Prevents "drain enemy HP → my mana" from becoming the dominant PvP strategy.
        if (sourceStatIndex == DefaultEntityStatTypes.getHealth()) {
            UUIDComponent srcUuid = hexContext.getAccessor().getComponent(
                    targetRef, UUIDComponent.getComponentType());
            UUIDComponent casterUuid = hexContext.getAccessor().getComponent(
                    hexContext.getCasterRef(), UUIDComponent.getComponentType());
            if (srcUuid == null || casterUuid == null
                    || !srcUuid.getUuid().equals(casterUuid.getUuid())) {
                HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
                return;
            }
        }

        // destination entity (default = caster). Destination stat is always Mana.
        HexVar destVar = glyph.readSlot(DrainGlyphSlots.DESTINATION, hexContext);
        Ref<EntityStore> destRef = hexContext.getCasterRef();
        EntityVar destEntityVar = SpellVarUtil.resolveEntityVar(destVar, hexContext);
        if (destEntityVar != null) {
            Ref<EntityStore> resolved = destEntityVar.getRef(hexContext.getAccessor());
            if (resolved != null && resolved.isValid()) destRef = resolved;
        }

        float rate = conversionRate(sourceStatIndex);

        if (drainPercent <= 0) {
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        EntityStatMap statMap = hexContext.getAccessor().getComponent(targetRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        EntityStatValue sourceStat = statMap.get(sourceStatIndex);
        if (sourceStat == null) {
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        float totalDrainAmount = (float) (drainPercent / 100.0) * sourceStat.getMax();

        if (sourceStatIndex == DefaultEntityStatTypes.getHealth()) {
            float maxDrainable = sourceStat.get() - 1.0f;
            if (maxDrainable <= 0) {
                HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
                return;
            }
            totalDrainAmount = Math.min(totalDrainAmount, maxDrainable);
        } else {
            totalDrainAmount = Math.min(totalDrainAmount, sourceStat.get());
        }

        if (totalDrainAmount <= 0) {
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        HexVar durationVar = glyph.readSlot(DrainGlyphSlots.DURATION, hexContext);
        float duration = DEFAULT_DURATION;
        if (durationVar != null) {
            duration = Math.max(0.01f, SpellVarUtil.resolveNumberOrDefault(durationVar, (double) DEFAULT_DURATION).floatValue());
        }

        HexColors colors = hexContext.getColors();
        Slot nextSlot = glyph.getSlot(Glyph.NEXT_SLOT);
        List<String> nextGlyphIds = nextSlot != null
                ? new ArrayList<>(Arrays.asList(nextSlot.getLinks()))
                : new ArrayList<>();
        Ref<EntityStore> hexEntityRef = hexContext.getRoot().getRootEntityRef();

        DrainComponent.DrainEntry entry = new DrainComponent.DrainEntry(
                sourceStatIndex, destRef, rate, totalDrainAmount, duration,
                hexContext.branch(), nextGlyphIds, hexEntityRef, colors);

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
            rootGlyph.addDependent(targetRef);
        }
    }

    @Override
    public HexVar readValue(Glyph glyph, HexContext hexContext) {
        HexVar targetVar = glyph.readSlot(DrainGlyphSlots.TARGET, hexContext);
        if (!(targetVar instanceof EntityVar entityVar)) return new NumberVar(0);

        Ref<EntityStore> targetRef = entityVar.getRef(hexContext.getAccessor());
        if (targetRef == null || !targetRef.isValid()) return new NumberVar(0);

        EntityStatMap statMap = hexContext.getAccessor().getComponent(targetRef, EntityStatMap.getComponentType());
        if (statMap == null) return new NumberVar(0);

        HexVar hpInput = glyph.readSlot(DrainGlyphSlots.HP, hexContext);
        HexVar staminaInput = glyph.readSlot(DrainGlyphSlots.STAMINA, hexContext);

        int statIndex;
        if (hpInput != null) {
            statIndex = DefaultEntityStatTypes.getHealth();
        } else if (staminaInput != null) {
            statIndex = DefaultEntityStatTypes.getStamina();
        } else {
            statIndex = DefaultEntityStatTypes.getMana();
        }

        EntityStatValue stat = statMap.get(statIndex);
        if (stat == null || stat.getMax() == 0) return new NumberVar(0);

        double fillPercent = (stat.get() / stat.getMax()) * 100.0;
        return new NumberVar(fillPercent);
    }
}
