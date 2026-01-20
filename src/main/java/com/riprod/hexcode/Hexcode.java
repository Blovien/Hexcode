package com.riprod.hexcode;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.command.HexcodeCommand;
import com.riprod.hexcode.entity.OrbitalGlyphComponent;
import com.riprod.hexcode.entity.OrbitalGlyphSystem;
import com.riprod.hexcode.event.EventHandlers;
import com.riprod.hexcode.glyph.GlyphRegistry;

public class Hexcode extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private EventHandlers eventHandlers;
    private ComponentType<EntityStore, OrbitalGlyphComponent> orbitalGlyphComponentType;

    public Hexcode(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hexcode spell-crafting mod v%s initializing...", this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        // Initialize glyph registry
        GlyphRegistry registry = GlyphRegistry.getInstance();
        LOGGER.atInfo().log("Registered %d glyphs", registry.getGlyphCount());

        // Register custom components
        registerComponents();

        // Register systems
        registerSystems();

        // Register commands
        this.getCommandRegistry().registerCommand(new HexcodeCommand());

        // Register event handlers
        this.eventHandlers = new EventHandlers();
        EventRegistry eventRegistry = this.getEventRegistry();
        this.eventHandlers.register(eventRegistry);

        LOGGER.atInfo().log("Hexcode setup complete!");
    }

    /**
     * Register custom components for the Hexcode mod.
     */
    private void registerComponents() {
        // Register OrbitalGlyphComponent
        orbitalGlyphComponentType = this.getEntityStoreRegistry().registerComponent(
                OrbitalGlyphComponent.class,
                OrbitalGlyphComponent::new
        );
        OrbitalGlyphComponent.setComponentType(orbitalGlyphComponentType);
        LOGGER.atInfo().log("Registered OrbitalGlyphComponent");
    }

    /**
     * Register systems for the Hexcode mod.
     */
    private void registerSystems() {
        // Register OrbitalGlyphSystem for tick updates
        ComponentType<EntityStore, TransformComponent> transformType = TransformComponent.getComponentType();

        OrbitalGlyphSystem orbitalSystem = new OrbitalGlyphSystem(
                orbitalGlyphComponentType,
                transformType
        );
        this.getEntityStoreRegistry().registerSystem(orbitalSystem);
        LOGGER.atInfo().log("Registered OrbitalGlyphSystem");
    }
}
