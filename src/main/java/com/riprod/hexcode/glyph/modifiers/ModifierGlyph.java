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

    protected ModifierGlyph(String id, String displayName, float multiplier, Set<String> incompatibleGlyphs) {
        this.id = id;
        this.displayName = displayName;
        this.multiplier = multiplier;
        this.visual = GlyphVisual.modifier();
        this.incompatibleGlyphs = incompatibleGlyphs;
    }

    protected ModifierGlyph(String id, String displayName, float multiplier, GlyphVisual visual, Set<String> incompatibleGlyphs) {
        this.id = id;
        this.displayName = displayName;
        this.multiplier = multiplier;
        this.visual = visual;
        this.incompatibleGlyphs = incompatibleGlyphs;
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
