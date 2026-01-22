package com.riprod.hexcode.glyph.modifiers;

import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRole;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Base class for MODIFIER glyphs - inner shells that amplify/alter behavior.
 *
 * Modifier glyphs wrap exactly one glyph and only affect that direct child.
 * A MODIFIER only modifies its direct child, not grandchildren.
 */
public abstract class ModifierGlyph implements Glyph {
    private final String id;
    private final String displayName;
    private final float multiplier;
    private final GlyphVisual visual;
    private final Set<String> incompatibleGlyphs;

    /**
     * Create a modifier glyph with a specific texture.
     *
     * @param id Unique identifier
     * @param displayName Display name
     * @param multiplier Modifier multiplier
     * @param textureName Texture name (e.g., "power")
     * @param incompatibleGlyphs Set of glyph IDs that cannot be modified
     */
    protected ModifierGlyph(String id, String displayName, float multiplier, String textureName, Set<String> incompatibleGlyphs) {
        this.id = id;
        this.displayName = displayName;
        this.multiplier = multiplier;
        this.visual = GlyphVisual.modifier(textureName);
        this.incompatibleGlyphs = incompatibleGlyphs;
    }

    /**
     * @deprecated Use constructor with textureName parameter instead.
     */
    protected ModifierGlyph(String id, String displayName, float multiplier, Set<String> incompatibleGlyphs) {
        this(id, displayName, multiplier, deriveTextureName(id), incompatibleGlyphs);
    }

    protected ModifierGlyph(String id, String displayName, float multiplier, GlyphVisual visual, Set<String> incompatibleGlyphs) {
        this.id = id;
        this.displayName = displayName;
        this.multiplier = multiplier;
        this.visual = visual;
        this.incompatibleGlyphs = incompatibleGlyphs;
    }

    /**
     * Derive texture name from glyph ID for backward compatibility.
     */
    private static String deriveTextureName(String id) {
        // Extract name from "hexcode:power" -> "power"
        int colonIndex = id.lastIndexOf(':');
        return colonIndex >= 0 ? id.substring(colonIndex + 1) : id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public GlyphRole getRole() {
        return GlyphRole.MODIFIER;
    }

    @Override
    public GlyphVisual getVisual() {
        return visual;
    }

    @Override
    public float getModifierMultiplier() {
        return multiplier;
    }

    @Override
    public Set<String> getIncompatibleGlyphs() {
        return incompatibleGlyphs;
    }

    /**
     * Check if this modifier can wrap the given glyph.
     */
    public boolean canWrap(Glyph target) {
        if (target == null) {
            return false;
        }
        // Check if target is in our incompatible list
        if (incompatibleGlyphs.contains(target.getId())) {
            return false;
        }
        // Check if target accepts us as a modifier
        return target.isCompatibleWith(this);
    }
}
