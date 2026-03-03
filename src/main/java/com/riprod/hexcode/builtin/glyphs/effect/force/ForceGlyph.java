package com.riprod.hexcode.builtin.glyphs.effect.force;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class ForceGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final double DEFAULT_FORCE = 20.0;

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.getInput(0, hexContext);

        if (targets == null || targets.size() == 0) {
            LOGGER.atInfo().log("force glyph: no targets, skipping");
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        HexVar dirInput = glyph.getInputOrDefault(1, hexContext, new PositionVar(new Vector3d(0, 1, 0)));
        HexVar magInput = glyph.getInputOrDefault(2, hexContext, new NumberVar(DEFAULT_FORCE));

        Vector3d force;
        double magnitude = SpellVarUtil.resolveNumberOrDefault(magInput, DEFAULT_FORCE);

        if (dirInput instanceof PositionVar posVar && posVar.size() > 0) {
            force = posVar.getAt(0).scale(magnitude);
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
                    vel.addInstruction(force, null, ChangeVelocityType.Add);
                    TransformComponent tc = hexContext.getAccessor().getComponent(ref, TransformComponent.getComponentType());
                    if (tc != null) {
                        ForceGlyphStyle.render(tc.getPosition(), force, hexContext.getAccessor());
                    }
                } catch (Exception e) {
                    LOGGER.atWarning().log("force glyph: could not apply force to entity " + entityVar.getAt(i).getUuid() + ": " + e.getMessage());
                }
            }
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
