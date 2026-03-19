package com.riprod.hexcode.builtin.glyphs.effect.propel;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class PropelGlyphStyle {

    private PropelGlyphStyle() {
    }

    public static void renderLaunch(Vector3d position, Vector3d direction,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect("Propel_Launch", "SFX_Staff_Fire_Shoot", position, accessor);
    }

    public static void renderEntityHit(Vector3d projectilePos, Vector3d hitPos,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect("Propel_Hit", "SFX_Staff_Flame_Fireball_Impact", hitPos, accessor);
    }

    public static void renderBlockHit(Vector3d hitPos, ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect("Propel_Hit", "SFX_Fireball_Miss", hitPos, accessor);
    }

    public static void renderMiss(Vector3d endPos, ComponentAccessor<EntityStore> accessor) {
        VfxUtil.particle("Propel_Miss", endPos, accessor);
    }
}
