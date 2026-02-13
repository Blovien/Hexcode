package com.riprod.hexcode.builtin.glyphs.blink;

import java.util.List;
import java.util.UUID;

import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

public class Blink implements GlyphHandler {
    public static final String ID = "Blink";

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
                if (target instanceof PositionVar positionVar) {
                    BlinkEffect.teleportToPosition(hexContext.casterRef, hexContext.accessor, positionVar.position);
                } else if (target instanceof EntityVar entityVar) {
                    BlinkEffect.teleportForward(entityVar.ref, hexContext.accessor);
                }
            }
        }

        Executor.continueExecution(hexContext, executionContext);
    }
}
