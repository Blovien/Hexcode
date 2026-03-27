package com.riprod.hexcode.builtin.glyphs.effect.conjure.style;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class ConjureStyle {

    private static final Vector3f DEFAULT_COLOR = new Vector3f(0.2f, 0.6f, 1.0f);

    private ConjureStyle() {
    }

    public static void renderSpawn(Vector3d center, HexColors colors,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect("Conjure_Spawn", "SFX_Ice_Build", center, accessor);
    }

    public static void renderTrigger(Vector3d center, HexColors colors,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect("Conjure_Trigger", "SFX_Arrow_Frost_Hit", center, accessor);
    }

    public static void renderDespawn(Vector3d center, HexColors colors,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect("Conjure_Despawn", "SFX_Fireball_Miss", center, accessor);
    }

    public static Vector3f resolveColor(HexColors colors) {
        if (colors == null || colors.getPrimaryColor() == null) return DEFAULT_COLOR;
        return HexColors.toVector3f(colors.getPrimaryColor());
    }
}
