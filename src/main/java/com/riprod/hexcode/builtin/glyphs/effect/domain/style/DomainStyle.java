package com.riprod.hexcode.builtin.glyphs.effect.domain.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class DomainStyle {

    private static final Vector3f DEFAULT_COLOR = new Vector3f(0.5f, 0.2f, 0.8f);
    private static final String SPAWN_PARTICLE = "Conjure_Spawn";
    private static final String SPAWN_SOUND = "SFX_Ice_Build";
    private static final String DESPAWN_PARTICLE = "Conjure_Despawn";
    private static final String DESPAWN_SOUND = "SFX_Fireball_Miss";
    private static final String TRIGGER_PARTICLE = "Conjure_Trigger";
    private static final String TRIGGER_SOUND = "SFX_Arrow_Frost_Hit";
    private static final String CONTESTED_PARTICLE = "Halt_Crystallize";
    private static final String CONTESTED_SOUND = "SFX_Ice_Break";
    private static final String AMBIENT_PARTICLE = "Area_Pulse";

    private DomainStyle() {
    }

    public static Vector3f resolveColor(HexColors colors) {
        if (colors == null || colors.getPrimaryColor() == null) return DEFAULT_COLOR;
        return HexColors.toVector3f(colors.getPrimaryColor());
    }

    public static void renderSpawn(Vector3d pos, float radius, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect(SPAWN_PARTICLE, SPAWN_SOUND, pos, accessor);
    }

    public static void renderDespawn(Vector3d pos, float radius, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect(DESPAWN_PARTICLE, DESPAWN_SOUND, pos, accessor);
    }

    public static void renderTrigger(Vector3d entityPos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect(TRIGGER_PARTICLE, TRIGGER_SOUND, entityPos, accessor);
    }

    public static void renderContested(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect(CONTESTED_PARTICLE, CONTESTED_SOUND, pos, accessor);
    }

    public static void renderAmbient(Vector3d center, float radius, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.particle(AMBIENT_PARTICLE, center, accessor);
    }
}
