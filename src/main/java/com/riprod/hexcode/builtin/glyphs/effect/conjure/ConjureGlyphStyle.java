package com.riprod.hexcode.builtin.glyphs.effect.conjure;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class ConjureGlyphStyle {

    private ConjureGlyphStyle() {
    }

    public static void renderSpawn(Vector3d center, ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect("Conjure_Spawn", "SFX_Ice_Build", center, accessor);
    }

    public static void renderTrigger(Vector3d center, ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect("Conjure_Trigger", "SFX_Arrow_Frost_Hit", center, accessor);
    }

    public static void renderDespawn(Vector3d center, ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect("Conjure_Despawn", "SFX_Fireball_Miss", center, accessor);
    }
}
