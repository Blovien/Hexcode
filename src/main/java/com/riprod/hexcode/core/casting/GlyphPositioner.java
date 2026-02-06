package com.riprod.hexcode.core.casting;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.utils.GlyphMath;
import com.riprod.hexcode.utils.SphericalPosition;

public class GlyphPositioner {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void PositionGlyphs(ComponentAccessor<EntityStore> accessor, Vector3d ownerPos,
            Iterable<Ref<EntityStore>> activeGlyphRefs,
            TransformComponent ownerTransform, Ref<EntityStore> rootGlyph) {

        TransformComponent rootTransform = accessor.getComponent(rootGlyph,
                TransformComponent.getComponentType());
        if (rootTransform == null) {
            return;
        }
        rootTransform.setPosition(ownerPos);

        // Apply rotation only to the children glyphs, not the root
        for (Ref<EntityStore> glyphRef : activeGlyphRefs) {
            try {

                TransformComponent glyphTransform = accessor.getComponent(glyphRef,
                        TransformComponent.getComponentType());
                if (glyphTransform == null) {
                    continue;
                }
                GlyphComponent glyphComp = accessor.getComponent(glyphRef, GlyphComponent.getComponentType());
                if (glyphComp == null) {
                    continue;
                }
                glyphTransform.setRotation(new Vector3f(glyphComp.getPitch(), glyphComp.getYaw(), 0));
            } catch (Exception e) {
                // Just in case, don't want to crash the server if something goes wrong with one
                // glyph
                LOGGER.atSevere().withCause(e).log("Error positioning glyph");
            }
        }
    }
}
