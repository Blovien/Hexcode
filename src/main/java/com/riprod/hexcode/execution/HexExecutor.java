package com.riprod.hexcode.execution;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.data.GlyphInstanceData;
import com.riprod.hexcode.data.HexBookData;
import com.riprod.hexcode.data.HexBookDataManager;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRegistry;
import com.riprod.hexcode.glyph.GlyphRole;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.util.RaycastUtil;
import com.hypixel.hytale.math.vector.Vector3d;

import java.util.UUID;

/**
 * Executes spells with proper context passing for Hexes vs Chains.
 *
 * <h2>Execution Model</h2>
 * <p>
 * The executor handles both nesting and chaining:
 * <ul>
 * <li><b>Hex (nested)</b>: Same context flows through parent → child.
 * Modifications are visible to all nested children.</li>
 * <li><b>Chain (sequential)</b>: Each chain element gets a fresh COPY of
 * the original context. Siblings are isolated.</li>
 * </ul>
 *
 * <h2>Execution Order</h2>
 * <ol>
 * <li>Create base context from caster info</li>
 * <li>For each chain element, create a COPY of base context</li>
 * <li>Execute hex node depth-first (context flows to nested children)</li>
 * <li>Delayed SELECTs pause until resolution before continuing children</li>
 * </ol>
 *
 * <h2>Key Behaviors</h2>
 * <ul>
 * <li>Chain elements get COPY of original context (isolated)</li>
 * <li>Nested children get SAME context (flows through)</li>
 * <li>Depth-first: complete entire hex before moving to next chain element</li>
 * <li>If no SELECT wraps a hex, implicit SELF[] is assumed</li>
 * </ul>
 *
 * @see SpellContext
 * @see Spell
 */
public class HexExecutor {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public HexExecutor() {
    }

    // ========== SPELL EXECUTION (NEW API) ==========

    /**
     * Execute a spell (supports chains).
     *
     * @param spell      The spell to execute
     * @param caster     The entity casting the spell
     * @param store      The entity store
     * @param world      The world
     * @param direction  The cast direction
     * @param castNumber The cast number (1 for first cast, 2 for second, etc.)
     * @return Execution result
     */
    public ExecutionResult execute(Hex hex, Ref<EntityStore> caster, Store<EntityStore> store,
            World world, Vector3d direction) {
        if (hex == null || hex.isEmpty()) {
            return ExecutionResult.failure("Spell is empty");
        }

        // Get caster position
        TransformComponent transform = store.getComponent(caster, TransformComponent.getComponentType());
        Vector3d origin = RaycastUtil.getPlayerEyePosition(transform);

        // Create base context
        SpellContext baseContext = SpellContext.create(caster, store, world, origin, direction, 1);

        LOGGER.atInfo().log("Executing spell with %d chain element(s), castNumber=%d",
                hex.getMaxDepth(), 1);

        try {
            // Execute the hex tree with a SELECT root (implicit SELF if needed)
            HexNode rootToExecute = hex.getRoot();

            // Execute depth-first from the root
            executeHexNode(rootToExecute, baseContext);

            return ExecutionResult.success();
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error executing spell");
            return ExecutionResult.failure("Execution error: " + e.getMessage());
        }
    }

    // ========== INTERNAL EXECUTION ==========

    /**
     * Execute a single hex node and its subtree.
     *
     * <p>
     * This is the core recursive execution method:
     * <ol>
     * <li>Load per-player glyph data (accuracy, draw speed)</li>
     * <li>Execute the glyph at this node (modifies context)</li>
     * <li>Execute children depth-first (context flows down)</li>
     * </ol>
     *
     * @param node    The node to execute
     * @param context The current spell context
     * @return The (possibly modified) context
     */
    public void executeHexNode(HexNode node, SpellContext context) {
        GlyphInstance glyph = node.getValue();

        LOGGER.atFine().log("Executing glyph '%s' (quality=%s, accuracy=%.2f)",
                glyph.getGlyph().getDisplayName(), glyph.getQualityRating(), glyph.getAccuracy());

        // Execute this glyph (modifies context)
        context = glyph.cast(context);

        // Record usage in player data
        recordGlyphUsage(glyph, context);

        // Check if this is a delayed execution (BEAM, PROJECTILE)
        if (context.hasMetadata("pendingDelayId")) {
            // For delayed execution, children are executed when the delay resolves
            // The DelayedExecutionManager handles this
            LOGGER.atInfo().log("Glyph '%s' has pending delay - children will execute on resolution",
                    glyph.getGlyph().getDisplayName());

            // Store the remaining children for later execution
            storeChildrenForDelayedExecution(node, context);
        }

        // Execute children depth-first (context flows down)
        for (HexNode child : node.getChildren()) {
            // Send a copy to each chained child
            SpellContext childContext = context.copy();
            executeHexNode(child, childContext);
        }
    }

    /**
     * Store children for delayed execution.
     */
    private void storeChildrenForDelayedExecution(HexNode node, SpellContext context) {
        // The DelayedExecutionManager needs to know about the remaining children
        // This is handled through metadata
        if (!node.getChildren().isEmpty()) {
            context.setMetadata("pendingChildren", node.getChildren());
        }
    }

    // ========== PLAYER DATA INTEGRATION ==========

    /**
     * Record that a glyph was used (for player statistics).
     *
     * <p>
     * Records usage in both the Hex Book (if equipped) and player data (legacy).
     *
     * @param glyph   The glyph that was executed
     * @param context The spell context
     */
    private void recordGlyphUsage(GlyphInstance glyph, SpellContext context) {
        Store<EntityStore> store = context.getStore();
        Ref<EntityStore> casterRef = context.getCaster();
        glyph.incrementUsage();

        // Record in Hex Book if available
        if (store != null && casterRef != null) {
            HexBookDataManager.recordGlyphUsage(store, casterRef, glyph.getGlyph().getId());
        }
    }

    // ========== RESULT CLASS ==========

    /**
     * Result of spell execution.
     */
    public static class ExecutionResult {
        private final boolean success;
        private final String message;
        private final int manaCost;

        private ExecutionResult(boolean success, String message, int manaCost) {
            this.success = success;
            this.message = message;
            this.manaCost = manaCost;
        }

        public static ExecutionResult success() {
            return new ExecutionResult(true, "Success", 0);
        }

        public static ExecutionResult success(int manaCost) {
            return new ExecutionResult(true, "Success", manaCost);
        }

        public static ExecutionResult failure(String message) {
            return new ExecutionResult(false, message, 0);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getManaCost() {
            return manaCost;
        }
    }
}
