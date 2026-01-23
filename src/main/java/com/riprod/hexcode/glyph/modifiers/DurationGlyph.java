package com.riprod.hexcode.glyph.modifiers;

import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.execution.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;

/**
 * Duration modifier glyph - extends duration effects.
 *
 * <p>Affects DOT/buff duration on effects and travel time on selects.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>basePower - the multiplier (default: 1.5 = 50% increase)</li>
 * </ul>
 */
public class DurationGlyph extends ModifierGlyph {

    /**
     * Create a duration glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public DurationGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.modifier("duration"));
    }

    @Override
    protected void applyModifier(SpellContext context) {
        float multiplier = getMultiplier();
        context.multiplyDuration(multiplier);
    }
}
