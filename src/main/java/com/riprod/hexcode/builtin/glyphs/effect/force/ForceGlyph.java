package com.riprod.hexcode.builtin.glyphs.effect.force;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class ForceGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // force power where mana cost equals the base JSON cost (ManaConsumption: 8)
    // power 16 ≈ a solid shove, costs exactly base mana
    private static final double MANA_REFERENCE_POWER = 16.0;
    // >1 = superlinear: big forces cost disproportionately more
    private static final double MANA_POWER_EXPONENT = 1.5;
    // floor so near-zero forces aren't completely free
    private static final double MANA_MIN_MULTIPLIER = 0.1;

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        double power = computeForcePower(glyph, hexContext);

        HexVar targets = glyph.resolveInput("target", hexContext);
        int targetCount = (targets != null) ? Math.max(targets.size(), 1) : 1;

        double powerMultiplier = Math.max(MANA_MIN_MULTIPLIER,
                Math.pow(power / MANA_REFERENCE_POWER, MANA_POWER_EXPONENT));

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;
        float finalCost = (float) (baseCost * castMultiplier * powerMultiplier * targetCount);

        boolean consumed = hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
        if (!consumed) {
            float currentMana = hexContext.getRoot().getCurrentMana(hexContext.getAccessor());
            LOGGER.atInfo().log("glyph force: needs %.1f mana (power=%.1f, targets=%d), has %.1f",
                    finalCost, power, targetCount, currentMana);
        }
        return consumed;
    }

    private double computeForcePower(Glyph glyph, HexContext hexContext) {
        HexVar dirInput = glyph.resolveInput("direction", hexContext);
        HexVar magInput = glyph.resolveInput("magnitude", hexContext);
        double magnitude = SpellVarUtil.resolveNumberOrDefault(magInput, 20.0);

        Vector3d force;
        if (dirInput instanceof PositionVar posVar && posVar.size() > 0) {
            force = new Vector3d(posVar.getAt(0)).scale(magnitude);
        } else {
            Vector3d direction = null;
            if (dirInput != null) {
                direction = SpellVarUtil.resolveDirection(dirInput, null, hexContext.getAccessor());
            }
            if (direction == null) {
                direction = new Vector3d(0, 1, 0);
            }
            force = new Vector3d(direction).scale(magnitude);
        }

        return Math.abs(force.getX()) + Math.abs(force.getY()) + Math.abs(force.getZ());
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.resolveInput("target", hexContext);
        if (targets == null || targets.size() == 0) {
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        HexVar dirInput = glyph.resolveInput("direction", hexContext);
        HexVar magInput = glyph.resolveInput("magnitude", hexContext);

        Vector3d force;
        double magnitude = SpellVarUtil.resolveNumberOrDefault(magInput, 20.0);

        if (dirInput instanceof PositionVar posVar && posVar.size() > 0) {
            force = new Vector3d(posVar.getAt(0)).scale(magnitude);
        } else {
            Vector3d direction = null;
            if (dirInput != null) {
                direction = SpellVarUtil.resolveDirection(dirInput, null, hexContext.getAccessor());
            }
            if (direction == null) {
                direction = new Vector3d(0, 1, 0);
            }

            force = new Vector3d(direction).scale(magnitude);
        }

        if (targets instanceof EntityVar entityVar) {
            for (int i = 0; i < entityVar.size(); i++) {
                Ref<EntityStore> ref = entityVar.getRef(i, hexContext.getAccessor());
                if (ref == null || !ref.isValid()) continue;

                try {
                    Velocity vel = hexContext.getAccessor().getComponent(ref, Velocity.getComponentType());
                    if (vel != null) {
                        vel.addInstruction(new Vector3d(force), new VelocityConfig(), ChangeVelocityType.Add);
                    }

                    TransformComponent tc = hexContext.getAccessor().getComponent(ref, TransformComponent.getComponentType());
                    if (tc != null) {
                        ForceGlyphStyle.render(tc.getPosition(), force, hexContext.getColors(), hexContext.getAccessor());
                    }
                } catch (Exception e) {
                    LOGGER.atWarning().log("force glyph: could not apply force to entity %s: %s",
                            entityVar.getAt(i).getUuid(), e.getMessage());
                }
            }
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
