package com.riprod.hexcode.builtin.glyphs.effect.bolt.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.VfxUtil;

public class BoltStyle {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String SHOCK_EFFECT_ID = "Hexcode_Shock";
    private static final String BOLT_PARTICLE = "Shock_Spawner_Temporary";
    private static final String BOLT_SOUND = "SFX_Staff_Fire_Shoot";
    private static final Vector3f DEFAULT_COLOR = new Vector3f(0.6f, 0.9f, 1.0f);
    private static final float BEAM_THICKNESS = 0.2f;
    private static final float BEAM_DURATION = 0.3f;
    private static final int PARTICLES_PER_BOLT = 12;

    private BoltStyle() {
    }

    public static Vector3f resolveColor(HexContext hexContext) {
        if (hexContext == null) return DEFAULT_COLOR;
        HexColors colors = hexContext.getColors();
        if (colors == null || colors.getPrimaryColor() == null) return DEFAULT_COLOR;
        return toVector3f(colors.getPrimaryColor());
    }

    public static void renderBolt(CommandBuffer<EntityStore> accessor, World world,
            Vector3d sourcePos, Vector3d targetPos, Vector3f color) {
        VfxUtil.line(accessor, world, sourcePos, targetPos, color, BEAM_THICKNESS, BEAM_DURATION, 0);
        VfxUtil.particleAlongPath(BOLT_PARTICLE, sourcePos, targetPos, PARTICLES_PER_BOLT, accessor);
        VfxUtil.sound(BOLT_SOUND, sourcePos, accessor);
    }

    public static void renderImpact(CommandBuffer<EntityStore> accessor, Vector3d position) {
        VfxUtil.particle(BOLT_PARTICLE, position, accessor);
        VfxUtil.sound(BOLT_SOUND, position, accessor);
    }

    public static void applyShockEffect(CommandBuffer<EntityStore> accessor, Ref<EntityStore> targetRef) {
        EntityEffect shockEffect = EntityEffect.getAssetMap().getAsset(SHOCK_EFFECT_ID);
        if (shockEffect == null) {
            LOGGER.atWarning().log("bolt: Hexcode_Shock effect asset not found");
            return;
        }

        EffectControllerComponent controller = accessor.getComponent(
                targetRef, EffectControllerComponent.getComponentType());
        if (controller == null) return;

        controller.addEffect(targetRef, shockEffect, 1.0f, OverlapBehavior.OVERWRITE, accessor);
    }

    private static Vector3f toVector3f(Color c) {
        return new Vector3f(
                (c.red & 0xFF) / 255f,
                (c.green & 0xFF) / 255f,
                (c.blue & 0xFF) / 255f);
    }
}
