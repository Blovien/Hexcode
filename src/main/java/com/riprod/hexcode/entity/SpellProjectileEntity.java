package com.riprod.hexcode.entity;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.HexExecutor;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.selects.SelectGlyph;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.util.HexMathUtil;
import com.hypixel.hytale.math.vector.Vector3d;

/**
 * Represents a spell projectile entity in the world.
 *
 * Created by PROJECTILE select glyphs. Travels in the cast direction
 * and executes children on hit.
 */
public class SpellProjectileEntity {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final HexNode pendingNode;
    private final ExecutionContext context;
    private final SelectGlyph selectGlyph;
    private final Ref<EntityStore> caster;

    private Ref<EntityStore> entityRef;
    private Vector3d position;
    private Vector3d direction;
    private float speed;
    private float maxDistance;
    private float traveledDistance;
    private boolean resolved;

    public SpellProjectileEntity(HexNode pendingNode, ExecutionContext context, SelectGlyph selectGlyph,
                                  Vector3d startPosition, Vector3d direction, float speed, float maxDistance) {
        this.pendingNode = pendingNode;
        this.context = context;
        this.selectGlyph = selectGlyph;
        this.caster = context.getCaster();
        this.position = new Vector3d(startPosition);
        this.direction = new Vector3d(direction).normalize();
        this.speed = speed;
        this.maxDistance = maxDistance;
        this.traveledDistance = 0;
        this.resolved = false;
    }

    /**
     * @return The hex node containing children to execute on hit
     */
    public HexNode getPendingNode() {
        return pendingNode;
    }

    /**
     * @return The execution context
     */
    public ExecutionContext getContext() {
        return context;
    }

    /**
     * @return Current position
     */
    public Vector3d getPosition() {
        return new Vector3d(position);
    }

    /**
     * @return Movement direction
     */
    public Vector3d getDirection() {
        return new Vector3d(direction);
    }

    /**
     * @return Projectile speed
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * @return true if the projectile has hit or expired
     */
    public boolean isResolved() {
        return resolved;
    }

    /**
     * @return true if the projectile has traveled beyond max distance
     */
    public boolean isExpired() {
        return traveledDistance >= maxDistance;
    }

    /**
     * Update projectile position.
     *
     * @param dt Delta time in seconds
     */
    public void update(float dt) {
        if (resolved) {
            return;
        }

        float distance = speed * dt;
        position.add(HexMathUtil.mul(new Vector3d(direction), distance));
        traveledDistance += distance;

        if (traveledDistance >= maxDistance) {
            // Expired without hitting anything
            LOGGER.atInfo().log("Projectile expired after traveling %.1f blocks", traveledDistance);
            resolved = true;
        }
    }

    /**
     * Called when the projectile hits an entity.
     *
     * @param hitEntity The entity that was hit
     * @param hitPosition The position of impact
     * @param executor The hex executor
     */
    public void onHitEntity(Ref<EntityStore> hitEntity, Vector3d hitPosition, HexExecutor executor) {
        if (resolved) {
            return;
        }

        LOGGER.atInfo().log("Projectile hit entity at (%.1f, %.1f, %.1f)",
                hitPosition.x, hitPosition.y, hitPosition.z);

        // Set the hit target and execute children
        TargetSet targets = TargetSet.of(hitEntity).withOrigin(hitPosition);
        context.pushTargets(targets);

        // Execute all children of the select glyph
        for (HexNode child : pendingNode.getChildren()) {
            executor.executeNode(child, context);
        }

        context.popTargets();
        resolved = true;
    }

    /**
     * Called when the projectile hits a block.
     *
     * @param hitPosition The position of impact
     * @param executor The hex executor
     */
    public void onHitBlock(Vector3d hitPosition, HexExecutor executor) {
        if (resolved) {
            return;
        }

        LOGGER.atInfo().log("Projectile hit block at (%.1f, %.1f, %.1f)",
                hitPosition.x, hitPosition.y, hitPosition.z);

        // For block hits, execute with position target only
        TargetSet targets = TargetSet.ofPosition(hitPosition).withOrigin(hitPosition);
        context.pushTargets(targets);

        // Execute all children
        for (HexNode child : pendingNode.getChildren()) {
            executor.executeNode(child, context);
        }

        context.popTargets();
        resolved = true;
    }

    /**
     * Spawn this projectile entity in the world.
     *
     * @param store The entity store
     */
    public void spawn(Store<EntityStore> store) {
        // TODO: Implement actual projectile entity spawning
        // This would create an entity with:
        // - PredictedProjectile component
        // - Velocity component
        // - ModelComponent (projectile visual)
        // - TransformComponent
        // - BoundingBox for collision

        LOGGER.atInfo().log("Spawning spell projectile at (%.1f, %.1f, %.1f) with speed %.1f",
                position.x, position.y, position.z, speed);
    }

    /**
     * Despawn this projectile entity.
     *
     * @param store The entity store
     */
    public void despawn(Store<EntityStore> store) {
        if (entityRef != null) {
            // TODO: Implement actual entity despawning
            LOGGER.atInfo().log("Despawning spell projectile");
            entityRef = null;
        }
    }
}
