package com.riprod.hexcode.glyph.effects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRole;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Base class for EFFECT glyphs - leaf nodes that perform actions.
 *
 * Effect glyphs are always leaves in the Hex tree. They cannot contain
 * other glyphs and are where actual spell effects are applied.
 */
public abstract class EffectGlyph implements Glyph {
    private final String id;
    private final String displayName;
    private final int baseCost;
    private final GlyphVisual visual;
    private final Set<String> compatibleModifiers;

    protected EffectGlyph(String id, String displayName, int baseCost, GlyphVisual visual, Set<String> compatibleModifiers) {
        this.id = id;
        this.displayName = displayName;
        this.baseCost = baseCost;
        this.visual = visual;
        this.compatibleModifiers = compatibleModifiers;
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
        return GlyphRole.EFFECT;
    }

    @Override
    public GlyphVisual getVisual() {
        return visual;
    }

    @Override
    public int getBaseCost() {
        return baseCost;
    }

    /**
     * @return Set of modifier IDs that are compatible with this effect
     */
    public Set<String> getCompatibleModifiers() {
        return compatibleModifiers;
    }

    @Override
    public boolean isCompatibleWith(Glyph modifier) {
        if (modifier.getRole() != GlyphRole.MODIFIER) {
            return false;
        }
        // Check if this effect accepts the modifier
        if (!compatibleModifiers.isEmpty() && !compatibleModifiers.contains(modifier.getId())) {
            return false;
        }
        // Also check modifier's compatibility with us
        return Glyph.super.isCompatibleWith(modifier);
    }

    /**
     * Apply the effect to targets. Override in subclasses.
     */
    @Override
    public abstract void applyEffect(ExecutionContext ctx, TargetSet targets);

    /**
     * Get the effective damage/healing amount after modifiers.
     */
    protected float getModifiedAmount(ExecutionContext ctx, float baseAmount) {
        return baseAmount * ctx.getTotalModifierMultiplier();
    }

    /**
     * Get the effective duration after modifiers.
     */
    protected float getModifiedDuration(ExecutionContext ctx, float baseDuration) {
        float durationMod = ctx.getModifier("hexcode:duration");
        return baseDuration * durationMod;
    }
}
