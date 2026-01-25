package com.riprod.hexcode.execution;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.hex.HexNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages delayed spell executions for glyphs like BEAM and PROJECTILE.
 *
 * <p>When a SELECT glyph with delayed execution (like BEAM or PROJECTILE) is cast,
 * it registers a pending execution with this manager. When the projectile/beam
 * hits a target, the manager resolves the execution and runs the child glyphs.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>BEAM/PROJECTILE glyph calls {@link #queueDelayedExecution} during cast</li>
 *   <li>Manager stores HexNode children + frozen SpellContext</li>
 *   <li>Projectile/beam entity calls {@link #resolveExecution} on hit</li>
 *   <li>Manager executes stored children with updated context (hit target added)</li>
 *   <li>Execution is cleaned up (or times out)</li>
 * </ol>
 *
 * <h2>Timeout</h2>
 * <p>Pending executions expire after a configurable timeout (default 30 seconds).
 * This prevents memory leaks from projectiles that miss or despawn.
 *
 * @see HexExecutor
 */
public class DelayedExecutionManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Default timeout for pending executions in milliseconds (30 seconds) */
    private static final long DEFAULT_TIMEOUT_MS = 30_000L;

    /** Singleton instance */
    private static DelayedExecutionManager instance;

    /** Map of execution ID to pending execution state */
    private final Map<UUID, PendingExecution> pendingExecutions = new ConcurrentHashMap<>();

    /** Timeout for pending executions */
    private long timeoutMs = DEFAULT_TIMEOUT_MS;

    private DelayedExecutionManager() {}

    /**
     * Get the singleton instance.
     *
     * @return The DelayedExecutionManager instance
     */
    @Nonnull
    public static synchronized DelayedExecutionManager getInstance() {
        if (instance == null) {
            instance = new DelayedExecutionManager();
        }
        return instance;
    }

    /**
     * Queue a delayed execution for later resolution.
     *
     * <p>Called by BEAM/PROJECTILE glyphs during their cast() method.
     * The children will be executed when {@link #resolveExecution} is called.
     *
     * @param parentNode The node whose children should be executed on hit
     * @param context The current spell context (will be copied and frozen)
     * @param origin The origin position of the projectile/beam
     * @param direction The direction of travel
     * @return Unique execution ID to use when resolving
     */
    @Nonnull
    public UUID queueDelayedExecution(
            @Nonnull HexNode parentNode,
            @Nonnull SpellContext context,
            @Nonnull Vector3d origin,
            @Nonnull Vector3d direction) {

        UUID executionId = UUID.randomUUID();

        // Copy context to freeze the state at queue time
        SpellContext frozenContext = context.copy();

        // Store pending execution
        PendingExecution pending = new PendingExecution(
                executionId,
                parentNode,
                frozenContext,
                origin,
                direction,
                System.currentTimeMillis()
        );

        pendingExecutions.put(executionId, pending);

        LOGGER.atInfo().log("Queued delayed execution %s with %d children",
                executionId, parentNode.getChildren().size());

        return executionId;
    }

    /**
     * Resolve a delayed execution when the projectile/beam hits.
     *
     * <p>Called by projectile/beam entities when they hit a target.
     * Executes all pending children with the hit target added to context.
     *
     * @param executionId The execution ID returned from {@link #queueDelayedExecution}
     * @param hitEntity The entity that was hit (may be null for position-only hits)
     * @param hitPosition The position of the hit
     * @return true if execution was found and resolved, false if not found or expired
     */
    public boolean resolveExecution(
            @Nonnull UUID executionId,
            @Nullable Ref<EntityStore> hitEntity,
            @Nonnull Vector3d hitPosition) {

        PendingExecution pending = pendingExecutions.remove(executionId);
        if (pending == null) {
            LOGGER.atWarning().log("Attempted to resolve unknown execution ID: %s", executionId);
            return false;
        }

        // Check timeout
        long elapsed = System.currentTimeMillis() - pending.queuedAt;
        if (elapsed > timeoutMs) {
            LOGGER.atInfo().log("Execution %s timed out after %d ms", executionId, elapsed);
            return false;
        }

        LOGGER.atInfo().log("Resolving delayed execution %s (hit at %s)",
                executionId, hitPosition);

        // Get context and add hit target
        SpellContext context = pending.context;

        if (hitEntity != null) {
            context.addTarget(hitEntity);
        }
        context.addTargetPosition(hitPosition);

        // Clear the pending delay marker
        context.removeMetadata("pendingDelayId");

        // Execute children
        HexExecutor executor = new HexExecutor();
        List<HexNode> children = pending.parentNode.getChildren();

        if (children.size() == 1) {
            // Single child - nested context flow
            executor.executeHexNode(children.get(0), context);
        } else {
            // Multiple children - chain siblings with isolated contexts
            for (HexNode child : children) {
                SpellContext siblingContext = context.copy();
                executor.executeHexNode(child, siblingContext);
            }
        }

        LOGGER.atInfo().log("Resolved execution %s - executed %d children",
                executionId, children.size());

        return true;
    }

    /**
     * Cancel a pending execution.
     *
     * <p>Called when a projectile despawns or is cancelled before hitting.
     *
     * @param executionId The execution ID to cancel
     * @return true if execution was found and cancelled
     */
    public boolean cancelExecution(@Nonnull UUID executionId) {
        PendingExecution removed = pendingExecutions.remove(executionId);
        if (removed != null) {
            LOGGER.atInfo().log("Cancelled delayed execution %s", executionId);
            return true;
        }
        return false;
    }

    /**
     * Check if an execution ID is pending.
     *
     * @param executionId The execution ID to check
     * @return true if execution is pending
     */
    public boolean isPending(@Nonnull UUID executionId) {
        return pendingExecutions.containsKey(executionId);
    }

    /**
     * Get the number of pending executions.
     *
     * @return Count of pending executions
     */
    public int getPendingCount() {
        return pendingExecutions.size();
    }

    /**
     * Clean up expired executions.
     *
     * <p>Should be called periodically (e.g., every tick or every few seconds)
     * to remove executions that have timed out.
     *
     * @return Number of executions cleaned up
     */
    public int cleanupExpired() {
        long now = System.currentTimeMillis();
        List<UUID> expired = new ArrayList<>();

        for (Map.Entry<UUID, PendingExecution> entry : pendingExecutions.entrySet()) {
            if (now - entry.getValue().queuedAt > timeoutMs) {
                expired.add(entry.getKey());
            }
        }

        for (UUID id : expired) {
            pendingExecutions.remove(id);
            LOGGER.atFine().log("Cleaned up expired execution: %s", id);
        }

        if (!expired.isEmpty()) {
            LOGGER.atInfo().log("Cleaned up %d expired delayed executions", expired.size());
        }

        return expired.size();
    }

    /**
     * Set the timeout for pending executions.
     *
     * @param timeoutMs Timeout in milliseconds
     */
    public void setTimeout(long timeoutMs) {
        this.timeoutMs = Math.max(1000L, timeoutMs); // Minimum 1 second
    }

    /**
     * Get the current timeout setting.
     *
     * @return Timeout in milliseconds
     */
    public long getTimeout() {
        return timeoutMs;
    }

    /**
     * Clear all pending executions.
     *
     * <p>Should be called on server shutdown or world unload.
     */
    public void clearAll() {
        int count = pendingExecutions.size();
        pendingExecutions.clear();
        if (count > 0) {
            LOGGER.atInfo().log("Cleared %d pending delayed executions", count);
        }
    }

    /**
     * Reset the singleton instance (for testing).
     */
    public static void reset() {
        if (instance != null) {
            instance.clearAll();
            instance = null;
        }
    }

    /**
     * Internal class representing a pending execution.
     */
    private static class PendingExecution {
        final UUID executionId;
        final HexNode parentNode;
        final SpellContext context;
        final Vector3d origin;
        final Vector3d direction;
        final long queuedAt;

        PendingExecution(UUID executionId, HexNode parentNode, SpellContext context,
                         Vector3d origin, Vector3d direction, long queuedAt) {
            this.executionId = executionId;
            this.parentNode = parentNode;
            this.context = context;
            this.origin = origin;
            this.direction = direction;
            this.queuedAt = queuedAt;
        }
    }
}
