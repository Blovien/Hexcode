package com.riprod.hexcode.builtin.glyphs.ice;

import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

public class Ice implements GlyphHandler {
    public static final String ID = "Ice";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void execute(HexContext hexContext, ExecutionContext executionContext) {
        UUID self = executionContext.getCurrentNode();
        Glyph glyph = hexContext.spellGraph.nodes.get(self);
        int variableIndex = glyph.getVariable(0);
        List<SpellVar> targets = executionContext.getVariable(variableIndex);

        if (targets != null) {
            for (SpellVar target : targets) {
                Vector3d position = extractPosition(target, hexContext);
                if (position != null) {
                    IceEffect.applySensory(position, hexContext.accessor);
                }
            }
        }

        Executor.continueExecution(hexContext, executionContext);
    }

    private Vector3d extractPosition(SpellVar target, HexContext hexContext) {
        if (target instanceof PositionVar positionVar) {
            return positionVar.position;
        } else if (target instanceof BlockVar blockVar) {
            return new Vector3d(blockVar.position.getX(), blockVar.position.getY(), blockVar.position.getZ());
        } else if (target instanceof EntityVar entityVar) {
            TransformComponent transform = hexContext.accessor.getComponent(entityVar.ref, TransformComponent.getComponentType());
            if (transform != null) return transform.getPosition();
        }
        return null;
    }
}
