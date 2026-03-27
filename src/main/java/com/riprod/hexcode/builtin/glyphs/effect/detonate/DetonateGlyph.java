package com.riprod.hexcode.builtin.glyphs.effect.detonate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.entity.ExplosionUtils;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.PointKnockback;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class DetonateGlyph implements GlyphHandler {
    public static final String ID = "Glyph_Detonate";

    private static final double MIN_KNOCKBACK_OFFSET = 0.1;
    private static final float BASE_DAMAGE = 2.0f;
    private static final float DAMAGE_SCALE = 0.2f;
    private static final float VOLATILITY_HARSHNESS = 0.5f;

    @Override
    public boolean resolveVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;
        float chance = tracker.computeSuccessChance(glyph) * VOLATILITY_HARSHNESS;
        chance = Math.max(0f, Math.min(1f, chance));
        float roll = ThreadLocalRandom.current().nextFloat();
        tracker.incrementGlyphType(glyph.getGlyphId());
        if (roll >= chance) {
            LOGGER.atInfo().log("glyph %s fizzled: rolled %.3f against %.3f chance (harshness %.1fx)",
                    glyph.getGlyphId(), roll, chance, 1.0f / VOLATILITY_HARSHNESS);
        }
        return roll < chance;
    }

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        HexVar radiusVar = glyph.resolveInput("radius", hexContext);
        HexVar magVar = glyph.resolveInput("magnitude", hexContext);
        double radius = SpellVarUtil.resolveNumberOrDefault(radiusVar, 5.0);
        double mag = SpellVarUtil.resolveNumberOrDefault(magVar, 10.0);

        float radiusFactor = (float) ((radius * radius) / 25.0);
        float magFactor = (float) (mag / 10.0);

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;
        float finalCost = baseCost * castMultiplier * radiusFactor * magFactor;

        boolean consumed = hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
        if (!consumed) {
            float currentMana = hexContext.getRoot().getCurrentMana(hexContext.getAccessor());
            LOGGER.atInfo().log("glyph %s insufficient mana: needs %.1f, has %.1f",
                    glyph.getGlyphId(), finalCost, currentMana);
        }
        return consumed;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar centerVar = glyph.resolveInput("center", hexContext);
        double radius = SpellVarUtil.resolveNumberOrDefault(glyph.resolveInput("radius", hexContext), 5.0);
        double mag = SpellVarUtil.resolveNumberOrDefault(glyph.resolveInput("magnitude", hexContext), 10.0);

        if (centerVar == null || centerVar.size() == 0) {
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
        float damage = BASE_DAMAGE + (float) mag * DAMAGE_SCALE;

        for (Vector3d center : centers) {
            Vector3d explosionCenter = new Vector3d(center).add(0, MIN_KNOCKBACK_OFFSET, 0);

            ExplosionConfig config = new ExplosionConfig() {
                {
                    damageEntities = true;
                    damageBlocks = false;
                    entityDamageRadius = (float) radius;
                    entityDamage = damage;
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
                    explosionCenter,
                    config,
                    null,
                    accessor,
                    hexContext.getChunkAccessor());

            DetonateGlyphStyle.render(center, radius, hexContext.getColors(), accessor);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
