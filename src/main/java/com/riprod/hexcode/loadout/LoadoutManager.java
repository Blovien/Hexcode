package com.riprod.hexcode.loadout;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player loadouts.
 *
 * Handles:
 * - Storing loadouts per player
 * - Creating default loadouts
 * - Loadout validation
 */
public class LoadoutManager {
    private static LoadoutManager instance;

    private final Map<UUID, Loadout> playerLoadouts;

    private LoadoutManager() {
        this.playerLoadouts = new HashMap<>();
    }

    /**
     * Get the singleton instance.
     */
    public static LoadoutManager getInstance() {
        if (instance == null) {
            instance = new LoadoutManager();
        }
        return instance;
    }

    /**
     * Get a player's loadout.
     *
     * @param playerId The player's UUID
     * @return The player's loadout (creates default if none exists)
     */
    public Loadout getLoadout(UUID playerId) {
        return playerLoadouts.computeIfAbsent(playerId, id -> Loadout.createDefaultLoadout());
    }

    /**
     * Set a player's loadout.
     *
     * @param playerId The player's UUID
     * @param loadout The loadout to set
     */
    public void setLoadout(UUID playerId, Loadout loadout) {
        playerLoadouts.put(playerId, loadout);
    }

    /**
     * Check if a player has a custom loadout.
     *
     * @param playerId The player's UUID
     * @return true if the player has a loadout set
     */
    public boolean hasLoadout(UUID playerId) {
        return playerLoadouts.containsKey(playerId);
    }

    /**
     * Remove a player's loadout (resets to default on next access).
     *
     * @param playerId The player's UUID
     */
    public void removeLoadout(UUID playerId) {
        playerLoadouts.remove(playerId);
    }

    /**
     * Reset a player's loadout to default.
     *
     * @param playerId The player's UUID
     */
    public void resetToDefault(UUID playerId) {
        playerLoadouts.put(playerId, Loadout.createDefaultLoadout());
    }

    /**
     * Get the number of stored loadouts.
     */
    public int getLoadoutCount() {
        return playerLoadouts.size();
    }

    /**
     * Clear all stored loadouts.
     */
    public void clearAll() {
        playerLoadouts.clear();
    }

    /**
     * Reset the manager instance.
     */
    public static void reset() {
        if (instance != null) {
            instance.clearAll();
        }
        instance = null;
    }
}
