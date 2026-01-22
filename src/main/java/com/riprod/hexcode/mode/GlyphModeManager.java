package com.riprod.hexcode.mode;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.loadout.Loadout;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages glyph mode sessions for all players.
 *
 * Singleton that tracks which players are in glyph mode and their state.
 */
public class GlyphModeManager {
    private static GlyphModeManager instance;

    private final Map<UUID, GlyphMode> activeSessions;

    private GlyphModeManager() {
        this.activeSessions = new HashMap<>();
    }

    /**
     * Get the singleton instance.
     */
    public static GlyphModeManager getInstance() {
        if (instance == null) {
            instance = new GlyphModeManager();
        }
        return instance;
    }

    /**
     * Check if a player is in glyph mode.
     *
     * @param playerId The player's UUID
     * @return true if the player is in glyph mode
     */
    public boolean isInGlyphMode(UUID playerId) {
        GlyphMode mode = activeSessions.get(playerId);
        return mode != null && mode.isActive();
    }

    /**
     * Get a player's glyph mode session.
     *
     * @param playerId The player's UUID
     * @return The glyph mode session, or null if not in mode
     */
    public GlyphMode getSession(UUID playerId) {
        return activeSessions.get(playerId);
    }

    /**
     * Enter glyph mode for a player.
     *
     * @param playerId The player's UUID
     * @param playerRef The player entity reference
     * @param loadout The player's loadout (or null for default)
     * @param commandBuffer The command buffer for deferred entity operations
     * @return The new glyph mode session
     */
    public GlyphMode enterGlyphMode(UUID playerId, Ref<EntityStore> playerRef, Loadout loadout, CommandBuffer<EntityStore> commandBuffer) {
        // If already in mode, just return existing session
        GlyphMode existing = activeSessions.get(playerId);
        if (existing != null && existing.isActive()) {
            return existing;
        }

        // Create new session
        Loadout effectiveLoadout = loadout != null ? loadout : Loadout.createDefaultLoadout();
        GlyphMode mode = new GlyphMode(playerRef, effectiveLoadout);

        // Enter with command buffer to spawn orbital glyphs (deferred)
        mode.enter(commandBuffer);

        activeSessions.put(playerId, mode);
        return mode;
    }

    /**
     * Exit glyph mode for a player.
     *
     * @param playerId The player's UUID
     * @param commandBuffer The command buffer for deferred entity operations
     * @return true if the player was in glyph mode
     */
    public boolean exitGlyphMode(UUID playerId, CommandBuffer<EntityStore> commandBuffer) {
        GlyphMode mode = activeSessions.get(playerId);
        if (mode != null && mode.isActive()) {
            // Exit with command buffer to despawn orbital glyphs (deferred)
            if (commandBuffer != null) {
                mode.exit(commandBuffer);
            } else {
                mode.exit();
            }
            return true;
        }
        return false;
    }

    /**
     * Exit glyph mode for a player (without entity cleanup).
     * Use this only when CommandBuffer is not available (e.g., player disconnect).
     *
     * @param playerId The player's UUID
     * @return true if the player was in glyph mode
     */
    public boolean exitGlyphMode(UUID playerId) {
        GlyphMode mode = activeSessions.get(playerId);
        if (mode != null && mode.isActive()) {
            mode.exit();
            return true;
        }
        return false;
    }

    /**
     * Toggle glyph mode for a player.
     *
     * @param playerId The player's UUID
     * @param playerRef The player entity reference
     * @param loadout The player's loadout (or null for default)
     * @param commandBuffer The command buffer for deferred entity operations
     * @return true if now in glyph mode, false if exited
     */
    public boolean toggleGlyphMode(UUID playerId, Ref<EntityStore> playerRef, Loadout loadout, CommandBuffer<EntityStore> commandBuffer) {
        if (isInGlyphMode(playerId)) {
            exitGlyphMode(playerId, commandBuffer);
            return false;
        } else {
            enterGlyphMode(playerId, playerRef, loadout, commandBuffer);
            return true;
        }
    }

    /**
     * Remove a player's session entirely (e.g., on disconnect).
     *
     * @param playerId The player's UUID
     */
    public void removeSession(UUID playerId) {
        GlyphMode mode = activeSessions.remove(playerId);
        if (mode != null) {
            mode.exit();
        }
    }

    /**
     * @return Number of active glyph mode sessions
     */
    public int getActiveSessionCount() {
        return (int) activeSessions.values().stream()
                .filter(GlyphMode::isActive)
                .count();
    }

    /**
     * Get all active sessions.
     */
    public Map<UUID, GlyphMode> getActiveSessions() {
        return new HashMap<>(activeSessions);
    }

    /**
     * Clear all sessions (for testing or shutdown).
     */
    public void clearAllSessions() {
        for (GlyphMode mode : activeSessions.values()) {
            mode.exit();
        }
        activeSessions.clear();
    }

    /**
     * Reset the manager instance (for testing).
     */
    public static void reset() {
        if (instance != null) {
            instance.clearAllSessions();
        }
        instance = null;
    }
}
