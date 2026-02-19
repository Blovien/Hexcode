package com.riprod.hexcode.builtin.glyphs.detonate;

import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.utils.SpellVarUtil;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.components.Glyph;

/**
 * number 1 = targets
 * number 2 = radius
 * number 3 = magnitude
 */
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
            List<Ref<EntityStore>> entities = TargetUtil.getAllEntitiesInSphere(
                    center, radius, hexContext.accessor);

            for (Ref<EntityStore> entityRef : entities) {
                TransformComponent transform = hexContext.accessor.getComponent(
                        entityRef, TransformComponent.getComponentType());
                if (transform == null)
                    continue;

                Vector3d direction = new Vector3d(transform.getPosition());
                direction.subtract(center);
                double distance = direction.length();

                if (distance < 0.01) {
                    direction.setX(0);
                    direction.setY(1);
                    direction.setZ(0);
                } else {
                    direction.normalize();
                }

                double falloff = 1.0 - (distance / radius);
                if (falloff <= 0)
                    continue;

                direction.scale(mag * falloff);

                Velocity vel = hexContext.accessor.getComponent(
                        entityRef, Velocity.getComponentType());
                if (vel == null)
                    continue;

                vel.addInstruction(direction, null, ChangeVelocityType.Add);
            }
        }

        Executor.continueExecution(hexContext, executionContext);
    }
}
