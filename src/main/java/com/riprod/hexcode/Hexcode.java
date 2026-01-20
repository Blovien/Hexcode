package com.riprod.hexcode;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.riprod.hexcode.command.HexcodeCommand;
import com.riprod.hexcode.event.EventHandlers;
import com.riprod.hexcode.glyph.GlyphRegistry;

public class Hexcode extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private EventHandlers eventHandlers;

    public Hexcode(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hexcode spell-crafting mod v%s initializing...", this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        // Initialize glyph registry
        GlyphRegistry registry = GlyphRegistry.getInstance();
        LOGGER.atInfo().log("Registered %d glyphs", registry.getGlyphCount());

        // Register commands
        this.getCommandRegistry().registerCommand(new HexcodeCommand());

        // Register event handlers
        this.eventHandlers = new EventHandlers();
        EventRegistry eventRegistry = this.getEventRegistry();
        this.eventHandlers.register(eventRegistry);

        LOGGER.atInfo().log("Hexcode setup complete!");
    }
}
