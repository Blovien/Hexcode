package com.riprod.hexcode.event;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.riprod.hexcode.mode.GlyphModeManager;

import java.util.UUID;

/**
 * Event handlers for the Hexcode mod.
 *
 * Note: Primary (left-click) and Secondary (right-click) interactions are now handled
 * via the Hytale interaction system through custom Operations:
 * - HexcodeGlyphModeToggle: Handles Secondary (right-click) to toggle glyph mode
 * - HexcodeGlyphAction: Handles Primary (left-click) for glyph drag/drop and hex casting
 *
 * See: com.riprod.hexcode.interaction package
 *
 * This class now only handles cleanup events (e.g., player disconnect).
 */
public class EventHandlers {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final GlyphModeManager modeManager;

    public EventHandlers() {
        this.modeManager = GlyphModeManager.getInstance();
    }

    /**
     * Register all event handlers.
     *
     * @param eventRegistry The event registry
     */
    public void register(EventRegistry eventRegistry) {
        registerDisconnectHandler(eventRegistry);
    }

    /**
     * Handle player disconnect to clean up glyph mode session.
     */
    private void registerDisconnectHandler(EventRegistry eventRegistry) {
        eventRegistry.register(PlayerDisconnectEvent.class, event -> {
            try {
                handleDisconnect(event);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Error handling disconnect event");
            }
        });
    }

    private void handleDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();

        UUID playerId = playerRef.getUuid();

        // Clean up glyph mode session
        modeManager.removeSession(playerId);
        LOGGER.atInfo().log("Cleaned up glyph mode session for disconnected player %s", playerId);
    }
}
