package com.riprod.hexcode.event;

import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.asset.GlyphAssetLoader;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphFactory;
import com.riprod.hexcode.glyph.GlyphRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * Event fired during Hexcode initialization to allow external plugins to register glyphs.
 *
 * <p>This event is fired before the registry is frozen, giving external plugins
 * the opportunity to register their custom glyphs using the same system as
 * built-in glyphs.
 *
 * <h2>Usage</h2>
 * <p>External plugins subscribe to this event and register their glyphs:
 * <pre>{@code
 * @Subscribe
 * public void onGlyphRegistration(GlyphRegistrationEvent event) {
 *     // Register external asset path first
 *     event.registerExternalAssetPath("myplugin", myPlugin.getDataDirectory().resolve("glyphs"));
 *
 *     // Load and register custom glyph
 *     GlyphAssetDefinition asset = event.loadGlyphAsset("myplugin:custom_glyph");
 *     event.registerGlyphWithAsset(asset, CustomGlyph::new);
 *
 *     // Or load from arbitrary path
 *     GlyphAssetDefinition asset2 = event.loadFromPath(myPlugin.getDataDirectory().resolve("special/glyph.json"));
 *     event.registerGlyphWithAsset(asset2, AnotherGlyph::new);
 * }
 * }</pre>
 *
 * <h2>Asset Paths</h2>
 * <p>External plugins should:
 * <ol>
 *   <li>Register their namespace with {@link #registerExternalAssetPath(String, Path)}</li>
 *   <li>Place glyph assets in their registered path</li>
 *   <li>Use namespace:name format for glyph IDs</li>
 * </ol>
 *
 * @see GlyphRegistry
 * @see GlyphAssetLoader
 */
public class GlyphRegistrationEvent {

    private final GlyphRegistry registry;

    /**
     * Create a new GlyphRegistrationEvent.
     *
     * @param registry The glyph registry (before freeze)
     */
    public GlyphRegistrationEvent(@Nonnull GlyphRegistry registry) {
        this.registry = registry;
    }

    // ========== GLYPH REGISTRATION ==========

    /**
     * Register a glyph directly.
     *
     * <p>The glyph must have a valid asset definition.
     *
     * @param glyph The glyph to register
     * @throws IllegalArgumentException if validation fails
     */
    public void registerGlyph(@Nonnull Glyph glyph) {
        registry.registerGlyph(glyph);
    }

    /**
     * Register a glyph from an asset definition using a factory.
     *
     * @param asset The asset definition (can be loaded via {@link #loadGlyphAsset})
     * @param factory The factory to create the glyph instance
     */
    public void registerGlyphWithAsset(@Nonnull GlyphAssetDefinition asset, @Nonnull GlyphFactory factory) {
        registry.registerGlyphFromAsset(asset, factory);
    }

    // ========== EXTERNAL PLUGIN SUPPORT ==========

    /**
     * Register an external asset path for a plugin namespace.
     *
     * <p>This allows the plugin's glyphs to be loaded by glyph ID (namespace:name).
     *
     * @param namespace The plugin's namespace (e.g., "myplugin")
     * @param assetPath Path to the plugin's glyph assets directory
     */
    public void registerExternalAssetPath(@Nonnull String namespace, @Nonnull Path assetPath) {
        GlyphAssetLoader.registerExternalAssetPath(namespace, assetPath);
    }

    // ========== ASSET LOADING ==========

    /**
     * Load an asset by glyph ID.
     *
     * <p>For built-in glyphs, use "hexcode:name" format.
     * For external plugins, ensure the namespace is registered first via
     * {@link #registerExternalAssetPath(String, Path)}.
     *
     * @param glyphId The glyph ID (e.g., "hexcode:fire" or "myplugin:custom")
     * @return The loaded asset definition, or null if loading fails
     */
    @Nullable
    public GlyphAssetDefinition loadGlyphAsset(@Nonnull String glyphId) {
        return GlyphAssetLoader.loadGlyphAsset(glyphId);
    }

    /**
     * Load an asset from an arbitrary path.
     *
     * <p>Use this for loading assets from non-standard locations.
     *
     * @param assetPath Full path to the asset file
     * @return The loaded asset definition, or null if loading fails
     */
    @Nullable
    public GlyphAssetDefinition loadFromPath(@Nonnull Path assetPath) {
        return GlyphAssetLoader.loadFromExternalPath(assetPath);
    }

    // ========== REGISTRY ACCESS ==========

    /**
     * Get the glyph registry.
     *
     * <p>Use this for advanced registration scenarios or to check
     * if a glyph is already registered.
     *
     * @return The glyph registry
     */
    @Nonnull
    public GlyphRegistry getRegistry() {
        return registry;
    }

    /**
     * Check if a glyph ID is already registered.
     *
     * @param glyphId The glyph ID
     * @return true if the glyph is registered
     */
    public boolean isRegistered(@Nonnull String glyphId) {
        return registry.hasGlyph(glyphId);
    }

    /**
     * Check if an asset is already loaded.
     *
     * @param glyphId The glyph ID
     * @return true if the asset is loaded
     */
    public boolean hasLoadedAsset(@Nonnull String glyphId) {
        return GlyphAssetLoader.hasAsset(glyphId);
    }
}
