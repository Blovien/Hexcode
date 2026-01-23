package com.riprod.hexcode.glyph.modifiers;

import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.execution.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;

/**
 * Power modifier glyph - amplifies damage/healing intensity.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>basePower - the multiplier (default: 1.5 = 50% increase)</li>
 * </ul>
 */
public class PowerGlyph extends ModifierGlyph {

    /**
     * Create a power glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public PowerGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.modifier("power"));
    }

    @Override
    protected void applyModifier(SpellContext context) {
        float multiplier = getMultiplier();
        context.multiplyPower(multiplier);
    }
}
