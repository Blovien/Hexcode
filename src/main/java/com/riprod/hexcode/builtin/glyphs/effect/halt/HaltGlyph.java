package com.riprod.hexcode.builtin.glyphs.effect.halt;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class HaltGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Halt";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.resolveInput("target", hexContext);

        if (targets == null || targets.size() == 0) {
            LOGGER.atInfo().log("halt glyph: no targets, skipping");
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        if (targets instanceof EntityVar entityVar) {
            for (int i = 0; i < entityVar.size(); i++) {
                Ref<EntityStore> ref = entityVar.getRef(i, hexContext.getAccessor());
                if (ref == null || !ref.isValid()) continue;

                try {
                    Velocity vel = hexContext.getAccessor().getComponent(ref, Velocity.getComponentType());
                    vel.addInstruction(new Vector3d(0, 0, 0), null, ChangeVelocityType.Set);
                    TransformComponent tc = hexContext.getAccessor().getComponent(ref, TransformComponent.getComponentType());
                    if (tc != null) {
                        HaltGlyphStyle.render(tc.getPosition(), hexContext.getAccessor());
                    }
                } catch (Exception e) {
                    LOGGER.atWarning().log("halt glyph: could not halt entity " + entityVar.getAt(i).getUuid() + ": " + e.getMessage());
                }
            }
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
