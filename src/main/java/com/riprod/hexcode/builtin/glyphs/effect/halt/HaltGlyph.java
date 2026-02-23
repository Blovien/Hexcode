package com.riprod.hexcode.builtin.glyphs.effect.halt;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.glyphs.variables.HexVar;

public class HaltGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Halt";

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        HexVar targets = glyph.getInput(0, executionContext, hexContext);

        if (targets instanceof EntityVar entityVar) {
            for (int i = 0; i < entityVar.size(); i++) {
                Ref<EntityStore> ref = entityVar.getRef(i, hexContext.accessor);
                if (ref == null || !ref.isValid()) continue;

                try {
                    Velocity vel = hexContext.accessor.getComponent(ref, Velocity.getComponentType());
                    vel.addInstruction(new Vector3d(0, 0, 0), null, ChangeVelocityType.Set);
                    TransformComponent tc = hexContext.accessor.getComponent(ref, TransformComponent.getComponentType());
                    if (tc != null) {
                        HaltGlyphStyle.render(tc.getPosition(), hexContext.accessor);
                    }
                } catch (Exception e) {
                    LOGGER.atWarning().log("halt glyph: could not halt entity " + entityVar.getAt(i).getUuid() + ": " + e.getMessage());
                }
            }
        }

        Executor.continueExecution(hexContext, executionContext);
    }
}
