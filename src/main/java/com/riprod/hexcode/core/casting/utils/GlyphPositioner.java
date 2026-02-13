package com.riprod.hexcode.core.casting.utils;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GlyphPositioner {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void PositionGlyphs(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ownerRef,
            Vector3d ownerPos, Ref<EntityStore> rootGlyph) {

        if (rootGlyph == null) {
            LOGGER.atSevere().log("Root glyph ref is null, cannot position glyphs");
            return;
        }

        TransformComponent rootTransform = accessor.getComponent(rootGlyph,
                TransformComponent.getComponentType());
        if (rootTransform == null) {
            return;
        }

        float eyeHeight = 0f;
        ModelComponent modelComp = accessor.getComponent(ownerRef, ModelComponent.getComponentType());
        if (modelComp != null && modelComp.getModel() != null) {
            eyeHeight = modelComp.getModel().getEyeHeight(ownerRef, accessor);
        }

        rootTransform.getPosition().assign(ownerPos.x, ownerPos.y + eyeHeight, ownerPos.z);
    }
}
