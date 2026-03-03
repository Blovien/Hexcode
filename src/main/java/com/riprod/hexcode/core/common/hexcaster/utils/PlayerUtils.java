package com.riprod.hexcode.core.common.hexcaster.utils;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerUtils {
    public static Vector3d getPlayerEyePosition(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef) {
        // Get the player's transform component to determine their position and eye height
        TransformComponent playerTransform = accessor.getComponent(playerRef,
                TransformComponent.getComponentType());
        if (playerTransform == null) {
            return Vector3d.ZERO; // Fallback to origin if we can't get the transform
        }

        ModelComponent modelComp = accessor.getComponent(playerRef, ModelComponent.getComponentType());
        float eyeHeight = modelComp != null ? modelComp.getModel().getEyeHeight(playerRef, accessor) : 1.68f;

        Vector3d playerPos = playerTransform.getPosition();

        return new Vector3d(playerPos.x, playerPos.y + eyeHeight, playerPos.z);
    }
}
