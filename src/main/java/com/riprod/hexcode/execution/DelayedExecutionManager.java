package com.riprod.hexcode.execution;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.entity.SpellBeamEntity;
import com.riprod.hexcode.entity.SpellProjectileEntity;
import com.riprod.hexcode.glyph.selects.SelectGlyph;
import com.riprod.hexcode.hex.HexNode;
import com.hypixel.hytale.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages delayed spell execution for BEAM and PROJECTILE selects.
 *
 * When a delayed SELECT is encountered during hex execution:
 * 1. The projectile/beam is spawned
 * 2. Sibling nodes are queued for execution after delays resolve
 * 3. When the projectile hits, it notifies this manager
 * 4. Once all delays resolve, remaining siblings execute
 */
public class DelayedExecutionManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static DelayedExecutionManager instance;

    private final Map<UUID, DelayedExecutionState> activeExecutions;
    private final Map<UUID, SpellProjectileEntity> activeProjectiles;
    private final Map<UUID, SpellBeamEntity> activeBeams;

    private DelayedExecutionManager() {
        this.activeExecutions = new ConcurrentHashMap<>();
        this.activeProjectiles = new ConcurrentHashMap<>();
        this.activeBeams = new ConcurrentHashMap<>();
    }

    public static synchronized DelayedExecutionManager getInstance() {
        if (instance == null) {
            instance = new DelayedExecutionManager();
        }
        return instance;
    }

    /**
     * Begin tracking a delayed execution.
     *
     * @param context The execution context
     * @param parentNode The parent node containing delayed children
     * @param executor The hex executor
     * @return The execution state for tracking
     */
    public DelayedExecutionState beginDelayedExecution(ExecutionContext context,
                                                        HexNode parentNode, HexExecutor executor) {
        UUID executionId = UUID.randomUUID();
        DelayedExecutionState state = new DelayedExecutionState(executionId, context, parentNode, executor);
        activeExecutions.put(executionId, state);
        LOGGER.atInfo().log("Started delayed execution %s", executionId);
        return state;
    }

    /**
     * Spawn a projectile for a delayed select.
     *
     * @param state The execution state
     * @param node The hex node with children to execute on hit
     * @param select The select glyph
     * @param store The entity store
     * @return The spawned projectile entity
     */
    public SpellProjectileEntity spawnProjectile(DelayedExecutionState state, HexNode node,
                                                  SelectGlyph select, Store<EntityStore> store) {
        ExecutionContext ctx = state.getContext();
        Vector3d origin = ctx.getCurrentOrigin();
        Vector3d direction = ctx.getCastDirection();
        float speed = ctx.calculateModifiedSpeed(20.0f);
        float range = ctx.calculateModifiedRange(50.0f);

        SpellProjectileEntity projectile = new SpellProjectileEntity(
                node, ctx, select, origin, direction, speed, range);

        UUID projectileId = UUID.randomUUID();
        projectile.spawn(store);
        activeProjectiles.put(projectileId, projectile);

        // Register this delay with the state
        state.addPendingDelay(projectileId);

        LOGGER.atInfo().log("Spawned projectile %s for execution %s", projectileId, state.getExecutionId());
        return projectile;
    }

    /**
     * Spawn a beam for a delayed select.
     *
     * @param state The execution state
     * @param node The hex node with children to execute on hit
     * @param select The select glyph
     * @param store The entity store
     * @return The spawned beam entity
     */
    public SpellBeamEntity spawnBeam(DelayedExecutionState state, HexNode node,
                                      SelectGlyph select, Store<EntityStore> store) {
        ExecutionContext ctx = state.getContext();
        Vector3d origin = ctx.getCurrentOrigin();
        Vector3d direction = ctx.getCastDirection();
        float speed = ctx.calculateModifiedSpeed(50.0f);
        float range = ctx.calculateModifiedRange(100.0f);

        SpellBeamEntity beam = new SpellBeamEntity(
                node, ctx, select, origin, direction, speed, range);

        UUID beamId = UUID.randomUUID();
        beam.spawn(store);
        activeBeams.put(beamId, beam);

        // Register this delay with the state
        state.addPendingDelay(beamId);

        LOGGER.atInfo().log("Spawned beam %s for execution %s", beamId, state.getExecutionId());
        return beam;
    }

    /**
     * Called when a delayed select (projectile/beam) resolves.
     *
     * @param executionId The parent execution ID
     * @param delayId The specific delay that resolved
     */
    public void onDelayResolved(UUID executionId, UUID delayId) {
        DelayedExecutionState state = activeExecutions.get(executionId);
        if (state == null) {
            LOGGER.atWarning().log("No execution state found for %s", executionId);
            return;
        }

        state.resolveDelay(delayId);
        LOGGER.atInfo().log("Delay %s resolved for execution %s", delayId, executionId);

        // Check if all delays are resolved
        if (state.allDelaysResolved()) {
            LOGGER.atInfo().log("All delays resolved for execution %s, continuing siblings", executionId);
            executePendingSiblings(state);
            cleanupExecution(executionId);
        }
    }

    /**
     * Execute pending sibling nodes after all delays resolve.
     */
    private void executePendingSiblings(DelayedExecutionState state) {
        HexExecutor executor = state.getExecutor();
        ExecutionContext context = state.getContext();

        for (HexNode sibling : state.getPendingSiblings()) {
            LOGGER.atInfo().log("Executing pending sibling: %s", sibling.getGlyph().getDisplayName());
            executor.executeNode(sibling, context);
        }
    }

    /**
     * Clean up a completed execution.
     */
    private void cleanupExecution(UUID executionId) {
        activeExecutions.remove(executionId);
        LOGGER.atInfo().log("Cleaned up execution %s", executionId);
    }

    /**
     * Update all active projectiles and beams.
     *
     * @param dt Delta time in seconds
     * @param store The entity store
     * @param executor The hex executor
     */
    public void update(float dt, Store<EntityStore> store, HexExecutor executor) {
        updateProjectiles(dt, store, executor);
        updateBeams(dt, store, executor);
        checkCollisions(store, executor);
    }

    /**
     * Update all active projectiles.
     */
    private void updateProjectiles(float dt, Store<EntityStore> store, HexExecutor executor) {
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, SpellProjectileEntity> entry : activeProjectiles.entrySet()) {
            SpellProjectileEntity projectile = entry.getValue();
            projectile.update(dt);

            if (projectile.isResolved()) {
                toRemove.add(entry.getKey());
            }
        }

        for (UUID id : toRemove) {
            SpellProjectileEntity projectile = activeProjectiles.remove(id);
            if (projectile != null) {
                projectile.despawn(store);
            }
        }
    }

    /**
     * Update all active beams.
     */
    private void updateBeams(float dt, Store<EntityStore> store, HexExecutor executor) {
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, SpellBeamEntity> entry : activeBeams.entrySet()) {
            SpellBeamEntity beam = entry.getValue();
            beam.update(dt);

            if (beam.isResolved()) {
                toRemove.add(entry.getKey());
            }
        }

        for (UUID id : toRemove) {
            SpellBeamEntity beam = activeBeams.remove(id);
            if (beam != null) {
                beam.despawn(store);
            }
        }
    }

    /**
     * Check for projectile and beam collisions.
     */
    private void checkCollisions(Store<EntityStore> store, HexExecutor executor) {
        checkProjectileCollisions(store, executor);
        checkBeamCollisions(store, executor);
    }

    /**
     * Check for projectile collisions.
     *
     * @param store The entity store
     * @param executor The hex executor
     */
    private void checkProjectileCollisions(Store<EntityStore> store, HexExecutor executor) {
        for (Map.Entry<UUID, SpellProjectileEntity> entry : activeProjectiles.entrySet()) {
            SpellProjectileEntity projectile = entry.getValue();
            if (projectile.isResolved()) {
                continue;
            }

            // Perform collision check
            CollisionResult collision = performProjectileRaycast(projectile, store);
            if (collision != null) {
                if (collision.hitEntity != null) {
                    projectile.onHitEntity(collision.hitEntity, collision.hitPosition, executor);
                } else {
                    projectile.onHitBlock(collision.hitPosition, executor);
                }
            }
        }
    }

    /**
     * Check for beam collisions.
     */
    private void checkBeamCollisions(Store<EntityStore> store, HexExecutor executor) {
        for (Map.Entry<UUID, SpellBeamEntity> entry : activeBeams.entrySet()) {
            SpellBeamEntity beam = entry.getValue();
            if (beam.isResolved()) {
                continue;
            }

            // Beams are instant raycasts
            CollisionResult collision = performBeamRaycast(beam, store);
            if (collision != null) {
                if (collision.hitEntity != null) {
                    beam.onHitEntity(collision.hitEntity, collision.hitPosition, executor);
                } else {
                    beam.onHitBlock(collision.hitPosition, executor);
                }
            }
        }
    }

    /**
     * Perform raycast collision detection for a projectile.
     *
     * @param projectile The projectile to check
     * @param store The entity store
     * @return Collision result or null if no collision
     */
    private CollisionResult performProjectileRaycast(SpellProjectileEntity projectile, Store<EntityStore> store) {
        // TODO: Implement actual raycast using Hytale's physics/collision APIs
        // This would involve:
        // 1. Get projectile position and direction
        // 2. Cast a ray from previous position to current position
        // 3. Check for entity intersections
        // 4. Check for block intersections
        // 5. Return the closest hit

        // For now, just log that we would check
        LOGGER.atInfo().log("Would perform projectile raycast at (%.1f, %.1f, %.1f)",
                projectile.getPosition().x, projectile.getPosition().y, projectile.getPosition().z);
        return null;
    }

    /**
     * Perform raycast collision detection for a beam.
     */
    private CollisionResult performBeamRaycast(SpellBeamEntity beam, Store<EntityStore> store) {
        // TODO: Implement actual raycast using Hytale's physics/collision APIs
        // Beams raycast once from origin in direction

        LOGGER.atInfo().log("Would perform beam raycast from (%.1f, %.1f, %.1f)");
        return null;
    }

    /**
     * Result of a collision check.
     */
    private static class CollisionResult {
        final Ref<EntityStore> hitEntity;
        final Vector3d hitPosition;

        CollisionResult(Ref<EntityStore> hitEntity, Vector3d hitPosition) {
            this.hitEntity = hitEntity;
            this.hitPosition = hitPosition;
        }
    }

    /**
     * State for a single delayed hex execution.
     */
    public static class DelayedExecutionState {
        private final UUID executionId;
        private final ExecutionContext context;
        private final HexNode parentNode;
        private final HexExecutor executor;
        private final Map<UUID, Boolean> pendingDelays;
        private final List<HexNode> pendingSiblings;

        public DelayedExecutionState(UUID executionId, ExecutionContext context,
                                      HexNode parentNode, HexExecutor executor) {
            this.executionId = executionId;
            this.context = context;
            this.parentNode = parentNode;
            this.executor = executor;
            this.pendingDelays = new HashMap<>();
            this.pendingSiblings = new ArrayList<>();
        }

        public UUID getExecutionId() {
            return executionId;
        }

        public ExecutionContext getContext() {
            return context;
        }

        public HexNode getParentNode() {
            return parentNode;
        }

        public HexExecutor getExecutor() {
            return executor;
        }

        /**
         * Register a delay to wait for.
         */
        public void addPendingDelay(UUID delayId) {
            pendingDelays.put(delayId, false);
        }

        /**
         * Mark a delay as resolved.
         */
        public void resolveDelay(UUID delayId) {
            pendingDelays.put(delayId, true);
        }

        /**
         * Check if all delays have resolved.
         */
        public boolean allDelaysResolved() {
            if (pendingDelays.isEmpty()) {
                return true;
            }
            for (Boolean resolved : pendingDelays.values()) {
                if (!resolved) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Add a sibling to execute after delays resolve.
         */
        public void addPendingSibling(HexNode sibling) {
            pendingSiblings.add(sibling);
        }

        /**
         * Get all pending siblings.
         */
        public List<HexNode> getPendingSiblings() {
            return pendingSiblings;
        }

        /**
         * Get count of pending delays.
         */
        public int getPendingDelayCount() {
            int count = 0;
            for (Boolean resolved : pendingDelays.values()) {
                if (!resolved) {
                    count++;
                }
            }
            return count;
        }
    }
}
