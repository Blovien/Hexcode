package com.riprod.hexcode.glyph.selects;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.execution.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;

/**
 * Self select glyph - targets the caster only.
 *
 * <p>Instant - no travel time. Always targets only the caster.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>No configurable properties - SELF always targets caster</li>
 * </ul>
 */
public class SelfGlyph extends SelectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Create a self glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public SelfGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.select("Self"), false);
    }

    @Override
    protected void selectTargets(SpellContext context) {
        LOGGER.atInfo().log("Self selecting caster as target");

        // Target the caster
        context.addTarget(context.getCaster());

        // Set cast origin as target position as well
        context.addTargetPosition(context.getCastOrigin());
    }
}
