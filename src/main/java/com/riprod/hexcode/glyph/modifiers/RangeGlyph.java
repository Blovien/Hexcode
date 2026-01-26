package com.riprod.hexcode.glyph.modifiers;

import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.executing.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;

/**
 * Range modifier glyph - extends distance/radius.
 *
 * <p>Incompatible with most EFFECT glyphs (they don't have range).
 * Compatible with SELECT glyphs that have range (BEAM, PROJECTILE, BURST, etc.)
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>basePower - the multiplier (default: 1.5 = 50% increase)</li>
 * </ul>
 */
public class RangeGlyph extends ModifierGlyph {

    /**
     * Create a range glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public RangeGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.modifier("Range"));
    }

    @Override
    protected void applyModifier(SpellContext context) {
        float multiplier = getMultiplier();
        context.multiplyRange(multiplier);
    }
}
