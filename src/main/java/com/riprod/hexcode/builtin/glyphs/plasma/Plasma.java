package com.riprod.hexcode.builtin.glyphs.plasma;

import java.util.List;
import java.util.UUID;

import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execute.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

public class Plasma implements GlyphHandler {
    public static final String ID = "Plasma";

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
                if (target instanceof EntityVar entityVar) {
                    PlasmaEffect.apply(entityVar.ref, hexContext.casterRef, hexContext.accessor);
                }
            }
        }

        Executor.continueExecution(hexContext, executionContext);
    }
}
