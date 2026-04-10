package com.riprod.hexcode.core.common.glyphs.component;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;

public interface GlyphHandler {
    HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    void execute(Glyph glyph, HexContext hexContext);

    default boolean canReadValue() {
        return false;
    }

    default HexVar readValue(Glyph glyph, HexContext hexContext) {
        return null;
    }

    /**
     * Stores `value` somewhere in the hex context. Called when something writes
     * through this glyph as a destination. The handler does the setVariable
     * itself; no key is exposed across the boundary.
     *
     * Default rule: if this handler produces a NumberVar via readValue, the
     * destination key IS that number (as a string). Otherwise it's the glyph's
     * own UUID. Variable overrides this — its destination is its dereference
     * target, not the value at that target.
     */
    default void writeValue(Glyph glyph, HexContext hexContext, HexVar value) {
        String key = glyph.getId();
        if (canReadValue()) {
            HexVar v = readValue(glyph, hexContext);
            if (v instanceof NumberVar nv) {
                key = String.valueOf((int) nv.getValue());
            }
        }
        hexContext.setVariable(key, value);
    }

    default boolean canExecute(Glyph glyph, HexContext hexContext) {
        if (canReadValue()) return true;
        return resolveVolatility(glyph, hexContext)
            && resolveMana(glyph, hexContext);
    }

    default boolean resolveVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;
        boolean passed = tracker.rollAndIncrement(glyph);
        if (!passed) {
            tracker.setFizzled(true);
            String title = resolveGlyphTitle(glyph.getGlyphId());
            LOGGER.atInfo().log("glyph %s fizzled: rolled %.3f against %.3f chance (cast #%d, type count %d)",
                    glyph.getGlyphId(), tracker.getLastRoll(), tracker.getLastChance(),
                    tracker.getCastCount(),
                    tracker.getGlyphTypeCount(glyph.getGlyphId()));
            sendCasterMessage(hexContext, title + " fizzled!");
        }
        return passed;
    }

    default boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        float baseCost = asset.getManaConsumption()
            * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;
        float finalCost = baseCost * castMultiplier;

        boolean consumed = hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
        if (!consumed) {
            float currentMana = hexContext.getRoot().getCurrentMana(hexContext.getAccessor());
            String title = resolveGlyphTitle(glyph.getGlyphId());
            LOGGER.atInfo().log("glyph %s insufficient mana: needs %.1f, has %.1f (base %d, efficiency %.2f, multiplier %.2f)",
                    glyph.getGlyphId(), finalCost, currentMana,
                    asset.getManaConsumption(), glyph.getEfficiency(), castMultiplier);
            sendCasterMessage(hexContext,
                    title + " requires " + String.format("%.0f", finalCost) + " mana (have " + String.format("%.0f", currentMana) + ")");
        }
        return consumed;
    }

    private static String resolveGlyphTitle(String glyphId) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyphId);
        return (asset != null && asset.getTitle() != null) ? asset.getTitle() : glyphId;
    }

    private static void sendCasterMessage(HexContext hexContext, String message) {
        Ref<EntityStore> casterRef = hexContext.getCasterRef();
        if (casterRef == null || !casterRef.isValid()) return;
        if (hexContext.getAccessor() == null) return;
        PlayerRef pr = hexContext.getAccessor().getComponent(casterRef, PlayerRef.getComponentType());
        if (pr != null) {
            pr.sendMessage(Message.raw(message));
        }
    }
}
