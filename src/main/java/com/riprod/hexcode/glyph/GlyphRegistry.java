package com.riprod.hexcode.glyph;

import com.riprod.hexcode.glyph.effects.*;
import com.riprod.hexcode.glyph.modifiers.*;
import com.riprod.hexcode.glyph.selects.*;

import java.util.*;

/**
 * Registry for all glyph definitions.
 *
 * Provides lookup of glyphs by ID and access to glyphs by role.
 */
public class GlyphRegistry {
    private static GlyphRegistry instance;

    private final Map<String, Glyph> glyphsById;
    private final List<Glyph> effectGlyphs;
    private final List<Glyph> modifierGlyphs;
    private final List<Glyph> selectGlyphs;

    private GlyphRegistry() {
        this.glyphsById = new HashMap<>();
        this.effectGlyphs = new ArrayList<>();
        this.modifierGlyphs = new ArrayList<>();
        this.selectGlyphs = new ArrayList<>();

        registerDefaultGlyphs();
    }

    /**
     * Get the singleton instance of the registry.
     */
    public static GlyphRegistry getInstance() {
        if (instance == null) {
            instance = new GlyphRegistry();
        }
        return instance;
    }

    /**
     * Register all default MVP glyphs.
     */
    private void registerDefaultGlyphs() {
        // Effect Glyphs (10)
        register(new FireGlyph());
        register(new IceGlyph());
        register(new LightningGlyph());
        register(new EarthGlyph());
        register(new VoidGlyph());
        register(new LightGlyph());
        register(new ShieldGlyph());
        register(new BlinkGlyph());
        register(new HealGlyph());
        register(new PushGlyph());

        // Modifier Glyphs (5)
        register(new PowerGlyph());
        register(new RangeGlyph());
        register(new DurationGlyph());
        register(new SpeedGlyph());
        register(new SplitGlyph());

        // Select Glyphs (7)
        register(new SelfGlyph());
        register(new TouchGlyph());
        register(new GazeGlyph());
        register(new BeamGlyph());
        register(new ProjectileGlyph());
        register(new BurstGlyph());
        register(new ConeGlyph());
    }

    /**
     * Register a glyph.
     */
    public void register(Glyph glyph) {
        glyphsById.put(glyph.getId(), glyph);

        switch (glyph.getRole()) {
            case EFFECT:
                effectGlyphs.add(glyph);
                break;
            case MODIFIER:
                modifierGlyphs.add(glyph);
                break;
            case SELECT:
                selectGlyphs.add(glyph);
                break;
        }
    }

    /**
     * Get a glyph by ID.
     *
     * @param id The glyph ID (e.g., "hexcode:fire")
     * @return The glyph, or null if not found
     */
    public Glyph getGlyph(String id) {
        return glyphsById.get(id);
    }

    /**
     * Get a glyph by ID, throwing if not found.
     */
    public Glyph getGlyphOrThrow(String id) {
        Glyph glyph = glyphsById.get(id);
        if (glyph == null) {
            throw new IllegalArgumentException("Unknown glyph: " + id);
        }
        return glyph;
    }

    /**
     * Check if a glyph is registered.
     */
    public boolean hasGlyph(String id) {
        return glyphsById.containsKey(id);
    }

    /**
     * Get all registered glyphs.
     */
    public Collection<Glyph> getAllGlyphs() {
        return Collections.unmodifiableCollection(glyphsById.values());
    }

    /**
     * Get all effect glyphs.
     */
    public List<Glyph> getEffectGlyphs() {
        return Collections.unmodifiableList(effectGlyphs);
    }

    /**
     * Get all modifier glyphs.
     */
    public List<Glyph> getModifierGlyphs() {
        return Collections.unmodifiableList(modifierGlyphs);
    }

    /**
     * Get all select glyphs.
     */
    public List<Glyph> getSelectGlyphs() {
        return Collections.unmodifiableList(selectGlyphs);
    }

    /**
     * Get glyphs by role.
     */
    public List<Glyph> getGlyphsByRole(GlyphRole role) {
        switch (role) {
            case EFFECT:
                return getEffectGlyphs();
            case MODIFIER:
                return getModifierGlyphs();
            case SELECT:
                return getSelectGlyphs();
            default:
                return Collections.emptyList();
        }
    }

    /**
     * Get the implicit SELF glyph for wrapping hexes without explicit select.
     */
    public Glyph getImplicitSelf() {
        return getGlyph(SelfGlyph.ID);
    }

    /**
     * Check if two glyphs are compatible (can modifier wrap target).
     */
    public boolean areCompatible(Glyph modifier, Glyph target) {
        if (modifier.getRole() != GlyphRole.MODIFIER) {
            return false;
        }
        return target.isCompatibleWith(modifier);
    }

    /**
     * Get count of registered glyphs.
     */
    public int getGlyphCount() {
        return glyphsById.size();
    }

    /**
     * Reset the registry (for testing).
     */
    public static void reset() {
        instance = null;
    }
}
