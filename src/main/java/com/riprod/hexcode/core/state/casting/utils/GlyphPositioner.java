package com.riprod.hexcode.core.state.casting.utils;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import org.joml.Vector3d;
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

        rootTransform.getRotation().set(0f, 0f, 0f);
    }
}
