package com.riprod.hexcode;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.asset.GlyphAssetLoader;
import com.riprod.hexcode.command.HexcodeCommand;
import com.riprod.hexcode.data.WorldBookDataStore;
import com.riprod.hexcode.data.WorldHexDataStore;
import com.riprod.hexcode.drawing.DrawingTemplate;
import com.riprod.hexcode.entity.GlyphComponent;
import com.riprod.hexcode.event.EventHandlers;
import com.riprod.hexcode.event.GlyphRegistrationEvent;
import com.riprod.hexcode.glyph.GlyphFactories;
import com.riprod.hexcode.glyph.GlyphFactory;
import com.riprod.hexcode.glyph.GlyphRegistry;
import com.riprod.hexcode.glyph.GlyphRole;
import com.riprod.hexcode.interaction.HexcodeGlyphAction;
import com.riprod.hexcode.interaction.HexcodeGlyphModeToggle;
import com.riprod.hexcode.mode.GlyphModeSystem;

import java.util.Map;

/**
 * Main plugin class for the Hexcode spell-crafting mod.
 *
 * <p>Hexcode allows players to enter Glyph Mode while wielding the Hex Staff
 * and Hex Book. Glyphs orbit around the player and can be composed into
 * Hexes (tree-structured spell constructs).
 *
 * <h2>Initialization Flow</h2>
 * <ol>
 *   <li>Load all glyph asset definitions via GlyphAssetLoader</li>
 *   <li>Fire GlyphRegistrationEvent (allows external plugins to register)</li>
 *   <li>Register built-in glyphs using factories + assets</li>
 *   <li>Freeze registry</li>
 *   <li>Register components, systems, commands, and events</li>
 * </ol>
 */
public class Hexcode extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private EventHandlers eventHandlers;
    private ComponentType<EntityStore, GlyphComponent> glyphComponentType;

    public Hexcode(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hexcode spell-crafting mod v%s initializing...", this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        // Step 1: Initialize data managers with plugin data directory
        initializeDataManagers();

        // Step 2: Initialize glyph system (asset-driven)
        initializeGlyphSystem();

        // Step 3: Register custom interactions (must be done before assets load)
        registerInteractions();

        // Step 4: Register custom components
        registerComponents();

        // Step 5: Register systems
        registerSystems();

        // Step 6: Register commands
        this.getCommandRegistry().registerCommand(new HexcodeCommand());

        // Step 7: Register event handlers
        this.eventHandlers = new EventHandlers();
        EventRegistry eventRegistry = this.getEventRegistry();
        this.eventHandlers.register(eventRegistry);

        LOGGER.atInfo().log("Hexcode setup complete!");
    }

    /**
     * Initialize data managers with the plugin's data directory.
     *
     * <p>This follows the Hytale pattern (similar to BarterShopState) where
     * data managers are initialized with the plugin's data directory for
     * proper file access and persistence.
     *
     * <p>Storage locations:
     * <ul>
     *   <li>WorldBookDataStore (legacy): {@code {world_save_path}/hexcode/{player_uuid}/{book_type}.json}</li>
     *   <li>WorldHexDataStore (new): {@code {world_save_path}/hexcode/books/{book_uuid}.json}</li>
     * </ul>
     */
    private void initializeDataManagers() {
        LOGGER.atInfo().log("Initializing data managers...");

        // Initialize GlyphAssetLoader for loading glyph asset definitions
        GlyphAssetLoader.initialize(this.getDataDirectory());
        LOGGER.atInfo().log("GlyphAssetLoader initialized");

        // Initialize WorldBookDataStore for per-world, per-player book data (legacy)
        WorldBookDataStore.initialize();
        LOGGER.atInfo().log("WorldBookDataStore initialized (legacy per-player storage)");

        // Initialize WorldHexDataStore for UUID-based book storage (new)
        WorldHexDataStore.initialize();
        LOGGER.atInfo().log("WorldHexDataStore initialized (UUID-based storage)");

        // Initialize DrawingTemplate for loading PNG templates
        DrawingTemplate.initialize(this.getDataDirectory());
        LOGGER.atInfo().log("DrawingTemplate system initialized");
    }

    /**
     * Initialize the glyph system with asset-driven registration.
     *
     * <p>This follows the new modular architecture where:
     * <ol>
     *   <li>Load asset definitions from JSON files</li>
     *   <li>Fire registration event for external plugins</li>
     *   <li>Create glyphs using factories + assets</li>
     *   <li>Freeze the registry</li>
     * </ol>
     */
    private void initializeGlyphSystem() {
        LOGGER.atInfo().log("Initializing glyph system...");

        // Get registry instance
        GlyphRegistry registry = GlyphRegistry.getInstance();

        // Step 1: Load all glyph asset definitions using static methods
        LOGGER.atInfo().log("Loading glyph assets...");
        Map<String, GlyphAssetDefinition> assets = GlyphAssetLoader.loadAll();
        LOGGER.atInfo().log("Loaded %d glyph asset definitions", assets.size());

        // Step 2: Fire GlyphRegistrationEvent (allows external plugins to register)
        LOGGER.atInfo().log("Firing GlyphRegistrationEvent...");
        GlyphRegistrationEvent registrationEvent = new GlyphRegistrationEvent(registry);
        // Note: In the actual implementation, this event would be fired through Hytale's event system
        // For now, external plugins would subscribe and be called here
        // EventSystem.fire(registrationEvent);

        // Step 3: Register built-in glyphs using factories + assets
        LOGGER.atInfo().log("Registering built-in glyphs...");

        for (GlyphAssetDefinition asset : assets.values()) {
            String glyphId = asset.getId();
            GlyphFactory factory = GlyphFactories.getFactory(glyphId);

            if (factory != null) {
                try {
                    registry.registerGlyphFromAsset(asset, factory);
                } catch (Exception e) {
                    LOGGER.atSevere().log("Failed to register glyph %s: %s", glyphId, e.getMessage());
                }
            } else {
                LOGGER.atWarning().log("No factory found for glyph: %s", glyphId);
            }
        }

        // Step 4: Freeze registry
        registry.freeze();
    }

    /**
     * Register custom interaction types with the interaction codec.
     * This allows our custom Operations to be referenced from RootInteraction JSON assets.
     */
    private void registerInteractions() {
        // Register HexcodeGlyphModeToggle (Secondary action - toggle glyph mode)
        // Type name must match "Type" field in JSON asset files
        Interaction.CODEC.register(
                "HexcodeGlyphModeToggle",
                HexcodeGlyphModeToggle.class,
                HexcodeGlyphModeToggle.CODEC
        );
        LOGGER.atInfo().log("Registered HexcodeGlyphModeToggle interaction");

        // Register HexcodeGlyphAction (Primary action - drag/drop/cast)
        // Type name must match "Type" field in JSON asset files
        Interaction.CODEC.register(
                "HexcodeGlyphAction",
                HexcodeGlyphAction.class,
                HexcodeGlyphAction.CODEC
        );
        LOGGER.atInfo().log("Registered HexcodeGlyphAction interaction");
    }

    /**
     * Register custom components for the Hexcode mod.
     */
    private void registerComponents() {
        // Register OrbitalGlyphComponent with CODEC for proper ECS serialization
        // Method signature: registerComponent(Class, String name, BuilderCodec)
        glyphComponentType = this.getEntityStoreRegistry().registerComponent(
                GlyphComponent.class,
                "OrbitalGlyphComponent",
                GlyphComponent.CODEC
        );
        GlyphComponent.setComponentType(glyphComponentType);
        LOGGER.atInfo().log("Registered OrbitalGlyphComponent with CODEC");
    }

    /**
     * Register systems for the Hexcode mod.
     */
    private void registerSystems() {
        // Register OrbitalGlyphSystem for tick updates
        // TransformComponent type is fetched lazily by the system to avoid null during setup
        GlyphModeSystem orbitalSystem = new GlyphModeSystem(glyphComponentType);
        this.getEntityStoreRegistry().registerSystem(orbitalSystem);
        LOGGER.atInfo().log("Registered OrbitalGlyphSystem");
    }
}
