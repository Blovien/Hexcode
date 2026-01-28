package com.riprod.hexcode.mode;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.data.HexBookData;
import com.riprod.hexcode.data.HexBookDataManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
     * Enter glyph mode for a player with book context and inventory.
     * Uses CommandBuffer for deferred entity operations during system ticks.
     *
     * <p>
     * When a book stack and inventory are provided:
     * <ul>
     * <li>Book UUID is created if needed (inventory updated)</li>
     * <li>Existing queued hex is loaded from WorldHexDataStore</li>
     * </ul>
     *
     * @param playerId      The player's UUID
     * @param playerRef     The player entity reference
     * @param commandBuffer The command buffer for deferred entity operations
     * @param bookStack     The Hex Book item stack (for loading queued hex)
     * @param inventory     The player's inventory (for updating ItemStack if UUID
     *                      created)
     * @return The new glyph mode session
     */
    public GlyphMode enterGlyphMode(UUID playerId, Ref<EntityStore> playerRef,
            CommandBuffer<EntityStore> commandBuffer,
            ItemStack bookStack, Inventory inventory) {
        // If already in mode, just return existing session
        GlyphMode existing = activeSessions.get(playerId);
        if (existing != null && existing.isActive()) {
            return existing;
        }

        // Get book data from held Hex Book using per-world storage
        HexBookData bookData = null;
        if (commandBuffer != null && playerRef != null) {
            bookData = HexBookDataManager.getHeldBookData(commandBuffer.getStore(), playerRef);
        }

        // Create new session with book data and player ID
        GlyphMode mode = new GlyphMode(playerRef, playerId, bookData);

        mode.enter(commandBuffer, bookStack, inventory);

        activeSessions.put(playerId, mode);
        return mode;
    }

    /**
     * Exit glyph mode for a player.
     * Uses CommandBuffer for deferred entity operations during system ticks.
     *
     * @param playerId      The player's UUID
     * @param commandBuffer The command buffer for deferred entity operations
     * @return true if the player was in glyph mode
     */
    public boolean exitGlyphMode(UUID playerId, CommandBuffer<EntityStore> commandBuffer) {
        GlyphMode mode = activeSessions.get(playerId);
        if (mode != null && mode.isActive()) {
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
     * Uses CommandBuffer for deferred entity operations during system ticks.
     *
     * @param playerId      The player's UUID
     * @param playerRef     The player entity reference
     * @param commandBuffer The command buffer for deferred entity operations
     * @param world         The world context for per-world data storage
     * @return true if now in glyph mode, false if exited
     */
    public boolean toggleGlyphMode(@Nonnull UUID playerId, @Nullable Ref<EntityStore> playerRef,
            @Nullable CommandBuffer<EntityStore> commandBuffer, @Nonnull World world) {
        return toggleGlyphMode(playerId, playerRef, commandBuffer, world, null, null);
    }

    /**
     * Toggle glyph mode for a player with book context.
     * Uses CommandBuffer for deferred entity operations during system ticks.
     *
     * <p>
     * On enter, loads any queued hex from WorldHexDataStore into composition.
     * On exit, saves the current composition to WorldHexDataStore.
     *
     * @param playerId      The player's UUID
     * @param playerRef     The player entity reference
     * @param commandBuffer The command buffer for deferred entity operations
     * @param world         The world context for per-world data storage
     * @param bookStack     The Hex Book item stack for loading composition
     * @param inventory     The player's inventory (for updating ItemStack if UUID
     *                      created)
     * @return true if now in glyph mode, false if exited
     */
    public boolean toggleGlyphMode(@Nonnull UUID playerId, @Nullable Ref<EntityStore> playerRef,
            @Nullable CommandBuffer<EntityStore> commandBuffer, @Nonnull World world,
            @Nullable ItemStack bookStack, @Nullable Inventory inventory) {
        if (isInGlyphMode(playerId)) {
            exitGlyphMode(playerId, commandBuffer);
            return false;
        } else {
            enterGlyphMode(playerId, playerRef, commandBuffer, bookStack, inventory);
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

    public void tickAll(float dt, CommandBuffer<EntityStore> commandBuffer) {
        for (GlyphMode mode : activeSessions.values()) {
            if (mode.isActive()) {
                Store<EntityStore> store = commandBuffer.getStore();
                mode.updateOrbitalGlyphs(store, dt);
            }
        }
    }
}
