package com.riprod.hexcode.core.casting;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.casting.utils.CastingStyle;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.utils.CreateGlyph;
import com.riprod.hexcode.utils.GlyphMath;
import com.riprod.hexcode.utils.SphericalPosition;

public class SpawnGlyphs {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static List<Ref<EntityStore>> spawnGlyphs(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ownerRef,
            Ref<EntityStore> castingRootRef,
            List<GlyphComponent> glyphs,
            String styleId) {

        LOGGER.atInfo().log("[spawnGlyphs] starting: %d glyphs, style='%s', accessor=%s",
                glyphs.size(), styleId, accessor.getClass().getSimpleName());

        CastingStyle style = CastingStyleRegistry.getOrDefault(styleId);
        TransformComponent ownerTransform = accessor.getComponent(ownerRef, TransformComponent.getComponentType());
        Vector3d ownerPos = ownerTransform.getPosition();
        Vector3f ownerRotation = ownerTransform.getRotation();
        LOGGER.atInfo().log("[spawnGlyphs] owner position=%s, rotation=%s", ownerPos, ownerRotation);

        List<SphericalPosition> positions = style.getInitialPositions(glyphs.size(), ownerRotation.getYaw(),
                ownerRotation.getPitch());

        List<Ref<EntityStore>> spawnedGlyphs = new ArrayList<>();

        for (int i = 0; i < glyphs.size(); i++) {
            GlyphComponent glyph = glyphs.get(i);
            SphericalPosition pos = positions.get(i);

            Vector3d cartesian = GlyphMath.sphericalToCartesian(pos);
            glyph.setOffset(new Vector3f((float) cartesian.x, (float) cartesian.y + 1.8f, (float) cartesian.z));
            glyph.setYaw(pos.getYaw());
            glyph.setPitch(pos.getPitch());
            glyph.setDistance(pos.getDistance());
            glyph.setRootRef(castingRootRef);
            glyph.setOwnerRef(ownerRef);

            LOGGER.atInfo().log("[spawnGlyphs] glyph[%d]='%s' spherical(yaw=%.2f, pitch=%.2f, dist=%.2f)",
                    i, glyph.getGlyphId(), pos.getYaw(), pos.getPitch(), pos.getDistance());

            try {
                Ref<EntityStore> spawnedGlyph = spawnGlyph(accessor, ownerRef, pos, glyph, ownerPos);
                spawnedGlyphs.add(spawnedGlyph);
                LOGGER.atInfo().log("[spawnGlyphs] glyph[%d] spawned, ref valid=%s", i, spawnedGlyph.isValid());
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e)
                        .log("[spawnGlyphs] failed to spawn glyph[%d] '%s'", i, glyph.getGlyphId());
            }
        }

        LOGGER.atInfo().log("[spawnGlyphs] done: %d/%d glyphs spawned", spawnedGlyphs.size(), glyphs.size());
        return spawnedGlyphs;

    }

    private static Ref<EntityStore> spawnGlyph(ComponentAccessor<EntityStore> buffer,
            Ref<EntityStore> ownerRef, SphericalPosition pos, GlyphComponent glyph, Vector3d parentPos) {

        List<Ref<EntityStore>> glyphRef = CreateGlyph.createGlyphEntity(buffer, glyph, parentPos);
        return glyphRef.get(0);
    }
}
