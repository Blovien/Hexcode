package com.riprod.hexcode.glyph.effects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.execution.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.util.HexMathUtil;

/**
 * Blink effect glyph - teleports target a short distance.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>baseDistance - base teleport distance in blocks (default: 8.0)</li>
 *   <li>keepVertical - whether to preserve Y coordinate (default: true)</li>
 * </ul>
 */
public class BlinkGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Create a blink glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public BlinkGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.effect(GlyphVisual.COLOR_BLINK, "Blink"));
    }

    @Override
    protected void applyEffect(SpellContext context, Ref<EntityStore> target, float power) {
        Store<EntityStore> store = context.getStore();

        // Get asset-driven properties
        float baseDistance = getProperty("baseDistance", 8.0f);
        boolean keepVertical = getProperty("keepVertical", true);

        // Calculate final distance with range multiplier
        float actualDistance = baseDistance * context.getRangeMultiplier();

        LOGGER.atInfo().log("Applying blink effect: %.1f distance", actualDistance);

        // Teleport the target
        teleportEntity(target, store, context.getCastDirection(), actualDistance, keepVertical);
    }

    @Override
    protected void applyEffectAtPosition(SpellContext context, Vector3d position, float power) {
        // Blink to a position - teleport caster to that position
        Store<EntityStore> store = context.getStore();
        Ref<EntityStore> caster = context.getCaster();

        TransformComponent transform = store.getComponent(caster, TransformComponent.getComponentType());
        if (transform == null) {
            LOGGER.atWarning().log("Caster has no TransformComponent, cannot teleport");
            return;
        }

        Vector3d currentPos = transform.getPosition();
        boolean keepVertical = getProperty("keepVertical", true);

        Vector3d destination = keepVertical ?
                new Vector3d(position.x, currentPos.y, position.z) :
                position;

        transform.setPosition(destination);

        LOGGER.atInfo().log("Teleported caster to position (%.1f, %.1f, %.1f)",
                destination.x, destination.y, destination.z);
    }

    /**
     * Teleport an entity in the given direction.
     */
    private void teleportEntity(Ref<EntityStore> targetRef, Store<EntityStore> store,
                                 Vector3d direction, float distance, boolean keepVertical) {
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

        // Validate destination is safe
        destination = validateDestination(store, currentPos, destination, keepVertical);

        // Update entity position
        transform.setPosition(destination);

        LOGGER.atInfo().log("Teleported entity from (%.1f, %.1f, %.1f) to (%.1f, %.1f, %.1f)",
                currentPos.x, currentPos.y, currentPos.z,
                destination.x, destination.y, destination.z);
    }

    /**
     * Validate and adjust the destination position if needed.
     */
    private Vector3d validateDestination(Store<EntityStore> store, Vector3d from, Vector3d to, boolean keepVertical) {
        // In a full implementation, this would:
        // 1. Check for solid blocks at destination
        // 2. Find safe ground if teleporting into air
        // 3. Prevent teleporting into walls
        // 4. Use raycasting to find valid landing spot

        // For now, optionally keep the y-coordinate the same to avoid falling/clipping issues
        if (keepVertical) {
            return new Vector3d(to.x, from.y, to.z);
        }
        return to;
    }
}
