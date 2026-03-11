package com.riprod.hexcode.builtin.glyphs.effect.detonate;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.entity.ExplosionUtils;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.PointKnockback;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class DetonateGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Detonate";

    private static final double RADIUS = 5.0;
    private static final double MAG = 10.0;
    private static final double MIN_KNOCKBACK_DISTANCE = 0.1;

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar centerVar = glyph.resolveInput("center", hexContext);
        double radius = SpellVarUtil.resolveNumberOrDefault(glyph.resolveInput("radius", hexContext), RADIUS);
        double mag = SpellVarUtil.resolveNumberOrDefault(glyph.resolveInput("magnitude", hexContext), MAG);

        if (centerVar == null || centerVar.size() == 0) {
            LOGGER.atInfo().log("detonate glyph: no centers, skipping");
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        List<Vector3d> centers = new ArrayList<>();
        for (int i = 0; i < centerVar.size(); i++) {
            Vector3d pos = SpellVarUtil.resolvePositionAt(centerVar, i, hexContext.getAccessor());
            if (pos != null)
                centers.add(pos);
        }

        if (centers.isEmpty()) {
            Vector3d casterPos = SpellVarUtil.resolvePosition(
                    hexContext.getVariable(1), hexContext.getAccessor());
            if (casterPos != null)
                centers.add(casterPos);
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        for (Vector3d center : centers) {
            ExplosionConfig config = new ExplosionConfig() {
                {
                    damageEntities = true;
                    damageBlocks = false;
                    entityDamageRadius = (float) radius;
                    entityDamage = (float) mag;
                    entityDamageFalloff = 1.0f;
                    knockback = new PointKnockback() {
                        {
                            force = (float) mag;
                            velocityY = (float) (mag * 0.3);
                            duration = 0;
                        }
                    };
                }
            };

            ExplosionUtils.performExplosion(
                    new Damage.EnvironmentSource("hex_detonate"),
                    center,
                    config,
                    null,
                    accessor,
                    hexContext.getChunkAccessor());

            DetonateGlyphStyle.render(center, radius, accessor);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
