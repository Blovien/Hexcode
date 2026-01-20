package com.riprod.hexcode.loadout;

import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A player's equipped glyph loadout.
 *
 * Players don't have access to all glyphs at once. Like a deckbuilder:
 * - Player pre-selects which glyphs to bring (loadout)
 * - Only loadout glyphs appear in orbital ring
 * - Loadout management happens outside of combat
 */
public class Loadout {
    public static final int MAX_GLYPHS = 12;

    private final List<String> glyphIds;

    public Loadout() {
        this.glyphIds = new ArrayList<>();
    }

    public Loadout(List<String> glyphIds) {
        this.glyphIds = new ArrayList<>(glyphIds);
    }

    /**
     * @return Unmodifiable list of glyph IDs in this loadout
     */
    public List<String> getGlyphIds() {
        return Collections.unmodifiableList(glyphIds);
    }

    /**
     * @return List of glyphs in this loadout (resolved from registry)
     */
    public List<Glyph> getGlyphs() {
        GlyphRegistry registry = GlyphRegistry.getInstance();
        List<Glyph> glyphs = new ArrayList<>();
        for (String id : glyphIds) {
            Glyph glyph = registry.getGlyph(id);
            if (glyph != null) {
                glyphs.add(glyph);
            }
        }
        return glyphs;
    }

    /**
     * Add a glyph to the loadout.
     *
     * @param glyphId The glyph ID to add
     * @return true if added successfully
     */
    public boolean addGlyph(String glyphId) {
        if (glyphIds.size() >= MAX_GLYPHS) {
            return false;
        }
        if (glyphIds.contains(glyphId)) {
            return false; // No duplicates
        }
        GlyphRegistry registry = GlyphRegistry.getInstance();
        if (!registry.hasGlyph(glyphId)) {
            return false;
        }
        glyphIds.add(glyphId);
        return true;
    }

    /**
     * Remove a glyph from the loadout.
     *
     * @param glyphId The glyph ID to remove
     * @return true if removed successfully
     */
    public boolean removeGlyph(String glyphId) {
        return glyphIds.remove(glyphId);
    }

    /**
     * Check if the loadout contains a glyph.
     */
    public boolean hasGlyph(String glyphId) {
        return glyphIds.contains(glyphId);
    }

    /**
     * @return Number of glyphs in this loadout
     */
    public int size() {
        return glyphIds.size();
    }

    /**
     * @return true if the loadout is empty
     */
    public boolean isEmpty() {
        return glyphIds.isEmpty();
    }

    /**
     * @return true if the loadout is at max capacity
     */
    public boolean isFull() {
        return glyphIds.size() >= MAX_GLYPHS;
    }

    /**
     * Clear all glyphs from the loadout.
     */
    public void clear() {
        glyphIds.clear();
    }

    /**
     * Create a default loadout for MVP testing.
     * Includes a variety of glyphs from each role.
     */
    public static Loadout createDefaultLoadout() {
        Loadout loadout = new Loadout();
        // Effects
        loadout.addGlyph("hexcode:fire");
        loadout.addGlyph("hexcode:ice");
        loadout.addGlyph("hexcode:lightning");
        loadout.addGlyph("hexcode:heal");
        loadout.addGlyph("hexcode:shield");
        // Modifiers
        loadout.addGlyph("hexcode:power");
        loadout.addGlyph("hexcode:range");
        loadout.addGlyph("hexcode:duration");
        // Selects
        loadout.addGlyph("hexcode:self");
        loadout.addGlyph("hexcode:beam");
        loadout.addGlyph("hexcode:burst");
        loadout.addGlyph("hexcode:projectile");
        return loadout;
    }
}
