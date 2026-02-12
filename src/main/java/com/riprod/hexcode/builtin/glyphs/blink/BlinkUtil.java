package com.riprod.hexcode.builtin.glyphs.blink;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class BlinkUtil {
    private BlinkUtil() {}

    public static Vector3d getLookDirection(Ref<EntityStore> entityRef, ComponentAccessor<EntityStore> accessor) {
        HeadRotation headRot = accessor.getComponent(entityRef, HeadRotation.getComponentType());
        if (headRot == null) return null;
        return headRot.getDirection();
    }
}
