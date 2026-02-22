package com.riprod.hexcode.builtin.glyphs.halt;

import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

import java.util.List;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.riprod.hexcode.components.Glyph;

public class HaltGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Halt";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        int targetSlot = glyph.getVariable(1);
        List<SpellVar> targets = executionContext.getVariable(targetSlot);

        for (SpellVar target : targets) {
            if (target instanceof EntityVar entityVar && entityVar.ref != null && entityVar.ref.isValid()) {
                try {
                    Velocity vel = hexContext.accessor.getComponent(entityVar.ref, Velocity.getComponentType());
                    vel.addInstruction(new Vector3d(0, 0, 0), null, ChangeVelocityType.Set);
                    TransformComponent tc = hexContext.accessor.getComponent(entityVar.ref, TransformComponent.getComponentType());
                    if (tc != null) {
                        HaltGlyphStyle.render(tc.getPosition(), hexContext.accessor);
                    }
                } catch (Exception e) {
                    LOGGER.atWarning().log("Halt glyph: could not halt entity " + entityVar.entityId + ": " + e.getMessage());
                }
            }
        }

        Executor.continueExecution(hexContext, executionContext);
    }
}
