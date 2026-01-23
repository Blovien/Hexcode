package com.riprod.hexcode.glyph.modifiers;

import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.execution.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;

/**
 * Speed modifier glyph - increases projectile/beam velocity.
 *
 * <p>Only compatible with SELECT glyphs that have travel time (BEAM, PROJECTILE).
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>basePower - the multiplier (default: 1.5 = 50% increase)</li>
 * </ul>
 */
public class SpeedGlyph extends ModifierGlyph {

    /**
     * Create a speed glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public SpeedGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.modifier("speed"));
    }

    @Override
    protected void applyModifier(SpellContext context) {
        float multiplier = getMultiplier();
        context.multiplySpeed(multiplier);
    }
}
