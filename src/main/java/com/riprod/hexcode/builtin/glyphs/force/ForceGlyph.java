package com.riprod.hexcode.builtin.glyphs.force;

import java.util.List;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.utils.SpellVarUtil;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

public class ForceGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Force";
    private static final double DEFAULT_FORCE = 50.0;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        int targetSlot = glyph.getVariable(1);
        List<SpellVar> targets = executionContext.getVariable(targetSlot);

        if (targets.isEmpty()) {
            LOGGER.atInfo().log("Force glyph: no targets, skipping");
            Executor.continueExecution(hexContext, executionContext);
            return;
        }

        // --- determine force vector ---
        boolean hasX = glyph.getNumbers().containsKey(1);
        boolean hasY = glyph.getNumbers().containsKey(2);
        boolean hasZ = glyph.getNumbers().containsKey(3);

        Vector3d force;

        if (hasX && hasY && hasZ) {
            // All three XYZ components explicitly provided
            force = new Vector3d(glyph.getNumber(1), glyph.getNumber(2), glyph.getNumber(3));

        } else if (glyph.getVariables().containsKey(2)) {
            // var(1) exists — check its type
            int dirSlot = glyph.getVariable(2);
            List<SpellVar> dirVars = executionContext.getVariable(dirSlot);
            SpellVar dirVar = dirVars.isEmpty() ? null : dirVars.get(0);

            if (dirVar instanceof PositionVar posVar && posVar.position != null) {
                // Position → treat directly as force magnitude vector
                force = new Vector3d(posVar.position);

            } else {
                // Entity or Rotation → resolve direction, then apply magnitude
                Vector3d direction = SpellVarUtil.resolveDirection(dirVars, null, hexContext.accessor);
                if (direction == null) {
                    direction = new Vector3d(0, 1, 0); // fallback upward
                }

                double magnitude = DEFAULT_FORCE;
                if (glyph.getVariables().containsKey(2)) {
                    // var(2) as magnitude source (NumberVar)
                    List<SpellVar> magVars = executionContext.getVariable(glyph.getVariable(2));
                    if (!magVars.isEmpty() && magVars.get(0) instanceof NumberVar numVar) {
                        magnitude = numVar.number;
                    }
                } else if (hasX) {
                    // single number → uniform magnitude
                    magnitude = glyph.getNumber(1);
                }

                force = new Vector3d(direction).scale(magnitude);
            }

        } else {
            // No XYZ, no var(1) → default launch upwards
            double magnitude = hasX ? glyph.getNumber(1) : DEFAULT_FORCE;
            force = new Vector3d(0, magnitude, 0);
        }

        // --- apply force to each target entity ---
        for (SpellVar target : targets) {
            if (target instanceof EntityVar entityVar && entityVar.ref != null && entityVar.ref.isValid()) {
                try {
                    Velocity vel = hexContext.accessor.getComponent(entityVar.ref, Velocity.getComponentType());
                    vel.addInstruction(force, null, ChangeVelocityType.Add);
                    LOGGER.atInfo().log("Force glyph: applied (" + force.getX() + ", " + force.getY() + ", " + force.getZ() + ") to entity " + entityVar.entityId);
                } catch (Exception e) {
                    LOGGER.atWarning().log("Force glyph: could not apply force to entity " + entityVar.entityId + ": " + e.getMessage());
                }
            }
        }

        Executor.continueExecution(hexContext, executionContext);
    }
}
