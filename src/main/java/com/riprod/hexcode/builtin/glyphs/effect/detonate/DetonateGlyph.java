package com.riprod.hexcode.builtin.glyphs.effect.detonate;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.entity.ExplosionUtils;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.HexVar;
import com.riprod.hexcode.utils.SpellVarUtil;

public class DetonateGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Detonate";

    private static final double RADIUS = 5.0;
    private static final double MAG = 10.0;
    private static final double MIN_KNOCKBACK_DISTANCE = 0.1;

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar centerVar = glyph.getInput(0, hexContext);
        double radius = SpellVarUtil.resolveNumberOrDefault(glyph.getInput(1, hexContext), RADIUS);
        double mag = SpellVarUtil.resolveNumberOrDefault(glyph.getInput(2, hexContext), MAG);

        if (centerVar == null || centerVar.size() == 0) {
            LOGGER.atInfo().log("detonate glyph: no centers, skipping");
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        List<Vector3d> centers = new ArrayList<>();
        for (int i = 0; i < centerVar.size(); i++) {
            Vector3d pos = SpellVarUtil.resolvePositionAt(centerVar, i, hexContext.getAccessor());
            if (pos != null) centers.add(pos);
        }

        if (centers.isEmpty()) {
            Vector3d casterPos = SpellVarUtil.resolvePosition(
                    hexContext.getVariable(1), hexContext.getAccessor());
            if (casterPos != null) centers.add(casterPos);
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        for (Vector3d center : centers) {
            ExplosionConfig config = new ExplosionConfig() {{
                damageEntities = true;
                damageBlocks = false;
                entityDamageRadius = (float) radius;
                entityDamage = (float) mag;
                entityDamageFalloff = 1.0f;
                knockback = null;
            }};

            ExplosionUtils.performExplosion(
                    new Damage.EnvironmentSource("hex_detonate"),
                    center,
                    config,
                    null,
                    accessor,
                    hexContext.getChunkAccessor()
            );

            applyKnockback(accessor, center, radius, mag);
            DetonateGlyphStyle.render(center, radius, accessor);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }

    private static void applyKnockback(CommandBuffer<EntityStore> accessor,
            Vector3d center, double radius, double mag) {

        List<Ref<EntityStore>> targets = new ArrayList<>();
        Selector.selectNearbyEntities(accessor, center, radius, targets::add, null);

        for (Ref<EntityStore> target : targets) {
            if (!target.isValid()) continue;

            TransformComponent transform = accessor.getComponent(target, TransformComponent.getComponentType());
            Velocity velocity = accessor.getComponent(target, Velocity.getComponentType());
            if (transform == null || velocity == null) continue;

            Vector3d diff = transform.getPosition().clone().subtract(center);
            double distance = diff.length();

            if (distance < MIN_KNOCKBACK_DISTANCE) {
                // too close to normalize safely, push straight up
                velocity.addForce(0, mag * 0.3, 0);
                continue;
            }

            double falloff = 1.0 - (distance / radius);
            if (falloff <= 0) continue;

            Vector3d direction = diff.scale(1.0 / distance);
            double force = mag * falloff;
            velocity.addForce(
                    direction.getX() * force,
                    mag * 0.3 * falloff,
                    direction.getZ() * force
            );
        }
    }
}
