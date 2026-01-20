package com.riprod.hexcode.glyph.selects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRole;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Base class for SELECT glyphs - outer shells that determine targeting/delivery.
 *
 * Select glyphs wrap one glyph OR a linked chain of siblings.
 * They determine how targets are selected for the wrapped effects.
 */
public abstract class SelectGlyph implements Glyph {
    private final String id;
    private final String displayName;
    private final boolean delayed;
    private final GlyphVisual visual;
    private final Set<String> compatibleModifiers;

    protected SelectGlyph(String id, String displayName, boolean delayed, Set<String> compatibleModifiers) {
        this.id = id;
        this.displayName = displayName;
        this.delayed = delayed;
        this.visual = GlyphVisual.select();
        this.compatibleModifiers = compatibleModifiers;
    }

    protected SelectGlyph(String id, String displayName, boolean delayed, GlyphVisual visual, Set<String> compatibleModifiers) {
        this.id = id;
        this.displayName = displayName;
        this.delayed = delayed;
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
        return GlyphRole.SELECT;
    }

    @Override
    public GlyphVisual getVisual() {
        return visual;
    }

    @Override
    public boolean isDelayed() {
        return delayed;
    }

    /**
     * @return Set of modifier IDs that are compatible with this select
     */
    public Set<String> getCompatibleModifiers() {
        return compatibleModifiers;
    }

    @Override
    public boolean isCompatibleWith(Glyph modifier) {
        if (modifier.getRole() != GlyphRole.MODIFIER) {
            return false;
        }
        // Check if this select accepts the modifier
        if (!compatibleModifiers.isEmpty() && !compatibleModifiers.contains(modifier.getId())) {
            return false;
        }
        // Also check modifier's compatibility with us
        return Glyph.super.isCompatibleWith(modifier);
    }

    /**
     * Select targets based on the current execution context.
     * Override in subclasses to implement specific targeting logic.
     */
    @Override
    public abstract TargetSet selectTargets(ExecutionContext ctx);

    /**
     * Get the effective range after modifiers.
     */
    protected float getModifiedRange(ExecutionContext ctx, float baseRange) {
        float rangeMod = ctx.getModifier("hexcode:range");
        return baseRange * rangeMod;
    }

    /**
     * Get the effective speed after modifiers.
     */
    protected float getModifiedSpeed(ExecutionContext ctx, float baseSpeed) {
        float speedMod = ctx.getModifier("hexcode:speed");
        return baseSpeed * speedMod;
    }

    /**
     * Get the split count after modifiers.
     */
    protected int getSplitCount(ExecutionContext ctx) {
        float splitMod = ctx.getModifier("hexcode:split");
        return splitMod > 1.0f ? 3 : 1;
    }
}
