package com.riprod.hexcode.builtin.glyphs.example;

import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;

public class ExampleGlyph implements GlyphHandler {
    public static final String ID = "Example_Glyph";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void execute(HexContext hexContext, ExecutionContext executionContext) {
        Executor.continueExecution(hexContext, executionContext);
    }
}
