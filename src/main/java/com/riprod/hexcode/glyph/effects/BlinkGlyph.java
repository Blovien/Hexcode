package com.riprod.hexcode.glyph.effects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.util.HexMathUtil;

import java.util.Set;

/**
 * Blink effect glyph - teleports target a short distance.
 */
public class BlinkGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "hexcode:blink";
    public static final int BASE_COST = 25;
    public static final float BASE_DISTANCE = 8.0f;

    public BlinkGlyph() {
        super(
            ID,
            "Blink",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_BLINK, "blink"),
            Set.of("hexcode:range")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float distance = ctx.calculateModifiedRange(BASE_DISTANCE);

        Store<EntityStore> store = ctx.getStore();
        Vector3d castDirection = ctx.getCastDirection();

        LOGGER.atInfo().log("Applying blink effect: %.1f distance to %d targets",
                distance, targets.getEntityCount());

        // Teleport each target entity
        for (Ref<EntityStore> targetRef : targets.getEntities()) {
            teleportEntity(targetRef, store, castDirection, distance);
        }
    }

    /**
     * Teleport an entity in the given direction.
     */
    private void teleportEntity(Ref<EntityStore> targetRef, Store<EntityStore> store,
                                 Vector3d direction, float distance) {
        TransformComponent transform = store.getComponent(targetRef, TransformComponent.getComponentType());
        if (transform == null) {
            LOGGER.atWarning().log("Target has no TransformComponent, cannot teleport");
            return;
        }

        Vector3d currentPos = transform.getPosition();

        // Calculate teleport destination in the cast direction
        Vector3d normalizedDir = new Vector3d(direction).normalize();
        Vector3d offset = HexMathUtil.mul(normalizedDir, distance);
        Vector3d destination = new Vector3d(
                currentPos.x + offset.x,
                currentPos.y + offset.y,
                currentPos.z + offset.z
        );

        // Validate destination is safe (basic ground check)
        destination = validateDestination(store, currentPos, destination);

        // Update entity position by modifying existing component
        transform.setPosition(destination);

        LOGGER.atInfo().log("Teleported entity from (%.1f, %.1f, %.1f) to (%.1f, %.1f, %.1f)",
                currentPos.x, currentPos.y, currentPos.z,
                destination.x, destination.y, destination.z);
    }

    /**
     * Validate and adjust the destination position if needed.
     * Basic implementation - would need proper collision checking in production.
     */
    private Vector3d validateDestination(Store<EntityStore> store, Vector3d from, Vector3d to) {
        // In a full implementation, this would:
        // 1. Check for solid blocks at destination
        // 2. Find safe ground if teleporting into air
        // 3. Prevent teleporting into walls
        // 4. Use raycasting to find valid landing spot

        // For now, keep the y-coordinate the same to avoid falling/clipping issues
        // This makes blink primarily horizontal movement
        return new Vector3d(to.x, from.y, to.z);
    }
}
