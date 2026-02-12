package com.riprod.hexcode.builtin.glyphs;

import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execute.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;

public class IceGlyph implements GlyphHandler {
    public static final String ID = "Ice";

    public IceGlyph() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void execute(HexContext hexContext, ExecutionContext executionContext) {

        // Continue the execution after
        Executor.continueExecution(hexContext, executionContext);
    }
}