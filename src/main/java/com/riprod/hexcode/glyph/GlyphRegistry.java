package com.riprod.hexcode.glyph;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.asset.GlyphAssetDefinition;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all glyphs in the Hexcode system.
 *
 * <p>The GlyphRegistry serves as the single source of truth for all registered glyphs.
 * It provides:
 * <ul>
 *   <li>Glyph registration from asset definitions + factories</li>
 *   <li>Thread-safe glyph lookup by ID</li>
 *   <li>Role-based glyph queries</li>
 *   <li>Compatibility validation</li>
 *   <li>Registry freeze to prevent post-initialization registration</li>
 * </ul>
 *
 * <h2>Registration Flow</h2>
 * <ol>
 *   <li>Load asset definitions via GlyphAssetLoader</li>
 *   <li>Fire GlyphRegistrationEvent (allows external plugins to register)</li>
 *   <li>Register built-in glyphs using factories + assets</li>
 *   <li>Call {@link #freeze()} to prevent further registration</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>Once frozen, the registry is fully thread-safe for concurrent lookups.
 * Registration must be done before freezing (typically during plugin setup).
 *
 * @see GlyphAssetDefinition
 * @see GlyphFactory
 */
public class GlyphRegistry {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static GlyphRegistry instance;

    // Storage
    private final Map<String, Glyph> glyphsById;
    private final Map<String, GlyphAssetDefinition> assetDefinitions;

    // Registration state
    private boolean frozen = false;

    private GlyphRegistry() {
        this.glyphsById = new ConcurrentHashMap<>();
        this.assetDefinitions = new ConcurrentHashMap<>();
    }

    /**
     * Get the singleton instance of the registry.
     *
     * @return The GlyphRegistry instance
     */
    public static synchronized GlyphRegistry getInstance() {
        if (instance == null) {
            instance = new GlyphRegistry();
        }
        return instance;
    }

    // ========== REGISTRATION ==========

    /**
     * Register a glyph directly.
     *
     * <p>The glyph must have a valid asset definition.
     *
     * @param glyph The glyph to register
     * @throws IllegalStateException if the registry is frozen
     * @throws IllegalArgumentException if validation fails
     */
    public void registerGlyph(Glyph glyph) {
        checkNotFrozen();

        String id = glyph.getId();

        if (glyphsById.containsKey(id)) {
            throw new IllegalArgumentException("Glyph already registered: " + id);
        }

        // Store glyph
        glyphsById.put(id, glyph);

        // Store asset definition
        GlyphAssetDefinition asset = glyph.getAssetDefinition();
        if (asset != null) {
            assetDefinitions.put(id, asset);
        }

        // Call onRegister callback
        Glyph.RegisterResult result = glyph.onRegister(this);
        if (!result.isSuccess()) {
            LOGGER.atWarning().log("Glyph registration callback failed for %s: %s",
                    id, result.getMessage());
        } else {
            LOGGER.atInfo().log("Registered glyph: %s (%s)", id, glyph.getDisplayName());
        }
    }

    /**
     * Register a glyph from an asset definition using a factory.
     *
     * @param asset The asset definition
     * @param factory The factory to create the glyph
     * @throws IllegalStateException if the registry is frozen
     */
    public void registerGlyphFromAsset(GlyphAssetDefinition asset, GlyphFactory factory) {
        checkNotFrozen();

        if (asset == null) {
            throw new IllegalArgumentException("Asset definition cannot be null");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Factory cannot be null");
        }

        try {
            Glyph glyph = factory.create(asset);
            registerGlyph(glyph);
        } catch (Exception e) {
            LOGGER.atSevere().log("Failed to create glyph from asset %s: %s",
                    asset.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Freeze the registry to prevent further registration.
     *
     * <p>Called after all glyphs are registered during initialization.
     * After freezing, registration methods will throw IllegalStateException.
     */
    public void freeze() {
        if (frozen) {
            LOGGER.atWarning().log("Registry already frozen");
            return;
        }

        frozen = true;

        LOGGER.atInfo().log("Registry frozen with %d glyphs",
                glyphsById.size());
    }

    /**
     * @return true if the registry is frozen
     */
    public boolean isFrozen() {
        return frozen;
    }

    // ========== LOOKUP ==========

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
     * Find a glyph by ID, returning Optional.
     *
     * @param id The glyph ID
     * @return Optional containing the glyph if found
     */
    public Optional<Glyph> findGlyph(String id) {
        return Optional.ofNullable(glyphsById.get(id));
    }

    /**
     * Get a glyph by ID, throwing if not found.
     *
     * @param id The glyph ID
     * @return The glyph
     * @throws IllegalArgumentException if not found
     */
    public Glyph getGlyphOrThrow(String id) {
        Glyph glyph = glyphsById.get(id);
        if (glyph == null) {
            throw new IllegalArgumentException("Unknown glyph: " + id);
        }
        return glyph;
    }

    /**
     * Get all registered glyphs.
     *
     * @return Unmodifiable collection of all glyphs
     */
    public Collection<Glyph> getAllGlyphs() {
        return Collections.unmodifiableCollection(glyphsById.values());
    }

    /**
     * Check if a glyph is registered.
     *
     * @param id The glyph ID
     * @return true if registered
     */
    public boolean hasGlyph(String id) {
        return glyphsById.containsKey(id);
    }

    /**
     * Get the total number of registered glyphs.
     *
     * @return Glyph count
     */
    public int getGlyphCount() {
        return glyphsById.size();
    }

    // ========== ASSET DEFINITIONS ==========

    /**
     * Get an asset definition by glyph ID.
     *
     * @param id The glyph ID
     * @return The asset definition, or null if not found
     */
    public GlyphAssetDefinition getAssetDefinition(String id) {
        return assetDefinitions.get(id);
    }

    /**
     * Get all loaded asset definitions.
     *
     * @return Unmodifiable map of ID to asset definition
     */
    public Map<String, GlyphAssetDefinition> getAssetDefinitions() {
        return Collections.unmodifiableMap(assetDefinitions);
    }

    /**
     * Get the implicit SELF glyph for wrapping hexes without explicit select.
     *
     * @return The SELF select glyph
     */
    public Glyph getImplicitSelf() {
        return getGlyph("hexcode:self");
    }

    // ========== VALIDATION ==========

    private void checkNotFrozen() {
        if (frozen) {
            throw new IllegalStateException("Cannot modify registry after freeze()");
        }
    }

    // ========== RESET ==========

    /**
     * Reset the registry (for testing purposes).
     *
     * <p>Clears all glyphs and resets frozen state.
     */
    public static void reset() {
        if (instance != null) {
            instance.glyphsById.clear();
            
            instance.assetDefinitions.clear();
            instance.frozen = false;
        }
        instance = null;
    }
}
