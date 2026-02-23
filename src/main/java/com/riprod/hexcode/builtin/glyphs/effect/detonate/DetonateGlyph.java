package com.riprod.hexcode.builtin.glyphs.effect.detonate;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.entity.ExplosionUtils;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.PointKnockback;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.HexVar;
import com.riprod.hexcode.utils.SpellVarUtil;

public class DetonateGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Detonate";

    private static final double RADIUS = 5.0;
    private static final double MAG = 10.0;

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        HexVar centerVar = glyph.getInput(0, executionContext, hexContext);
        double radius = SpellVarUtil.resolveNumberOrDefault(glyph.getInput(1, executionContext, hexContext), RADIUS);
        double mag = SpellVarUtil.resolveNumberOrDefault(glyph.getInput(2, executionContext, hexContext), MAG);

        List<Vector3d> centers = new ArrayList<>();
        for (int i = 0; i < centerVar.size(); i++) {
            Vector3d pos = SpellVarUtil.resolvePositionAt(centerVar, i, hexContext.accessor);
            if (pos != null) centers.add(pos);
        }

        if (centers.isEmpty()) {
            Vector3d casterPos = SpellVarUtil.resolvePosition(
                    executionContext.getVariable(1), hexContext.accessor);
            if (casterPos != null) centers.add(casterPos);
        }

        for (Vector3d center : centers) {
            ExplosionConfig config = new ExplosionConfig() {{
                damageEntities = true;
                damageBlocks = false;
                entityDamageRadius = (float) radius;
                entityDamage = (float) mag;
                entityDamageFalloff = 1.0f;
                knockback = new PointKnockback() {{
                    force = (float) mag;
                    velocityY = (float) (mag * 0.3);
                }};
            }};

            ExplosionUtils.performExplosion(
                    new Damage.EnvironmentSource("hex_detonate"),
                    center,
                    config,
                    null,
                    hexContext.accessor,
                    hexContext.chunkAccessor
            );

            DetonateGlyphStyle.render(center, radius, hexContext.accessor);
        }

        Executor.continueExecution(hexContext, executionContext);
    }
}
