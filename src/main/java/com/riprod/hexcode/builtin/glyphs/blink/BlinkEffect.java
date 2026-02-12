package com.riprod.hexcode.builtin.glyphs.blink;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.GlyphConstants;

public final class BlinkEffect {
    private BlinkEffect() {}

    public static void teleportToPosition(Ref<EntityStore> entityRef, ComponentAccessor<EntityStore> accessor, Vector3d targetPosition) {
        TransformComponent transform = accessor.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform == null) return;

        transform.teleportPosition(targetPosition);
    }

    public static void teleportForward(Ref<EntityStore> entityRef, ComponentAccessor<EntityStore> accessor) {
        Vector3d direction = BlinkUtil.getLookDirection(entityRef, accessor);
        if (direction == null) return;

        TransformComponent transform = accessor.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        Vector3d destination = new Vector3d(
            pos.x + direction.x * GlyphConstants.BLINK_DISTANCE,
            pos.y + direction.y * GlyphConstants.BLINK_DISTANCE,
            pos.z + direction.z * GlyphConstants.BLINK_DISTANCE
        );
        transform.teleportPosition(destination);
    }
}
