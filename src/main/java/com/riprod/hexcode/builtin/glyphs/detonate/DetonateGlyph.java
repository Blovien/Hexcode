package com.riprod.hexcode.builtin.glyphs.detonate;

import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.utils.SpellVarUtil;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.entity.ExplosionUtils;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.PointKnockback;
import com.riprod.hexcode.components.Glyph;

public class DetonateGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Detonate";

    private static final double RADIUS = 5.0;
    private static final double MAG = 10.0;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        int centerSlot = glyph.getVariable(1);
        List<SpellVar> centerVars = executionContext.getVariable(centerSlot);

        double radius = glyph.getNumbers().containsKey(1) ? glyph.getNumber(1) : RADIUS;
        double mag = glyph.getNumbers().containsKey(2) ? glyph.getNumber(2) : MAG;

        List<Vector3d> centers = new ArrayList<>();
        for (SpellVar var : centerVars) {
            Vector3d pos = SpellVarUtil.resolvePosition(List.of(var), hexContext.accessor);
            if (pos != null)
                centers.add(pos);
        }

        if (centers.isEmpty()) {
            Vector3d casterPos = SpellVarUtil.resolvePosition(
                    executionContext.getVariable(1), hexContext.accessor);
            if (casterPos != null)
                centers.add(casterPos);
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
