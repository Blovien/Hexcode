package com.riprod.hexcode.glyph;

import com.riprod.hexcode.asset.GlyphAssetDefinition;

/**
 * Factory interface for creating Glyph instances from asset definitions.
 *
 * <p>GlyphFactory is used by the GlyphRegistry to create glyph instances
 * from their asset definitions. Each glyph type has a corresponding factory
 * registered in GlyphFactories.
 *
 * <p>Example usage:
 * <pre>{@code
 * GlyphFactory fireFactory = asset -> new FireGlyph(asset);
 * Glyph fireGlyph = fireFactory.create(fireAssetDefinition);
 * }</pre>
 *
 * @see GlyphFactories
 * @see GlyphRegistry
 */
@FunctionalInterface
public interface GlyphFactory {

    /**
     * Create a new glyph instance from an asset definition.
     *
     * @param asset The asset definition containing glyph properties
     * @return A new Glyph instance
     * @throws IllegalArgumentException if the asset is incompatible with this factory
     */
    Glyph create(GlyphAssetDefinition asset);
}
