package com.riprod.hexcode.builtin.glyphs.swap;

import com.riprod.hexcode.builtin.utils.BlockUtils;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

import java.util.List;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.components.Glyph;

public class SwapGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Swap";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        
        int slotA = glyph.getVariable(glyph.getNumber(1));
        int slotB = glyph.getVariable(glyph.getNumber(2));
        List<SpellVar> varsA = executionContext.getVariable(slotA);
        List<SpellVar> varsB = executionContext.getVariable(slotB);

        World world = hexContext.accessor.getExternalData().getWorld();

        int pairCount = Math.min(varsA.size(), varsB.size());
        for (int i = 0; i < pairCount; i++) {
            SpellVar varA = varsA.get(i);
            SpellVar varB = varsB.get(i);
            BlockUtils.swapPair(varA, varB, world, hexContext);
         }

        Executor.continueExecution(hexContext, executionContext);
    }
}
