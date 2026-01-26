package com.riprod.hexcode.executing;

import com.riprod.hexcode.glyph.selects.SelectGlyph;
import com.riprod.hexcode.hex.HexNode;
import com.hypixel.hytale.math.vector.Vector3d;

/**
 * Represents a delayed execution waiting for a projectile/beam to hit.
 *
 * When a BEAM or PROJECTILE glyph is executed, it creates a DelayedExecution
 * that holds the pending subtree until the projectile hits something.
 */
public class DelayedExecution {
    private final HexNode pendingNode;
    private final ExecutionContext context;
    private final SelectGlyph selectGlyph;
    private final Vector3d startPosition;
    private final Vector3d direction;
    private final float maxRange;
    private final float speed;

    private boolean resolved;
    private long createdAt;

    public DelayedExecution(HexNode pendingNode, ExecutionContext context, SelectGlyph selectGlyph,
                            Vector3d startPosition, Vector3d direction, float maxRange, float speed) {
        this.pendingNode = pendingNode;
        this.context = context;
        this.selectGlyph = selectGlyph;
        this.startPosition = startPosition;
        this.direction = direction;
        this.maxRange = maxRange;
        this.speed = speed;
        this.resolved = false;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * @return The hex node to execute when projectile hits
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
     * @return The select glyph that created this delay
     */
    public SelectGlyph getSelectGlyph() {
        return selectGlyph;
    }

    /**
     * @return Starting position of the projectile/beam
     */
    public Vector3d getStartPosition() {
        return startPosition;
    }

    /**
     * @return Direction of travel
     */
    public Vector3d getDirection() {
        return direction;
    }

    /**
     * @return Maximum range
     */
    public float getMaxRange() {
        return maxRange;
    }

    /**
     * @return Speed of the projectile/beam
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * @return true if this delay has been resolved
     */
    public boolean isResolved() {
        return resolved;
    }

    /**
     * Mark this delay as resolved.
     */
    public void resolve() {
        this.resolved = true;
    }

    /**
     * @return Time in milliseconds since creation
     */
    public long getAge() {
        return System.currentTimeMillis() - createdAt;
    }

    /**
     * @return Expected travel time in milliseconds
     */
    public long getExpectedTravelTime() {
        return (long) ((maxRange / speed) * 1000);
    }

    /**
     * Check if this delay has expired (projectile should have hit or expired by now).
     */
    public boolean isExpired() {
        return getAge() > getExpectedTravelTime() * 2; // 2x buffer
    }
}
