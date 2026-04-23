package com.riprod.hexcode.builtin.glyphs.swap;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.builtin.glyphs.swap.style.SwapStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.BlockUtils;
import com.riprod.hexcode.utils.SpellVarUtil;

public class SwapGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
public String getId() { return ID; };

public static final String ID = "Swap";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar varsA = glyph.readSlot(SwapGlyphSlots.A, hexContext);
        HexVar varsB = glyph.readSlot(SwapGlyphSlots.B, hexContext);

        if (varsA == null || varsB == null) {
            LOGGER.atWarning().log("swap glyph: missing input variables");
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        World world = hexContext.getAccessor().getExternalData().getWorld();

        Vector3d posA = SpellVarUtil.resolvePosition(varsA, hexContext.getAccessor());
        Vector3d posB = SpellVarUtil.resolvePosition(varsB, hexContext.getAccessor());
        if (posA != null && posB != null) {
            SwapStyle.render(posA, posB, hexContext.getColors(), hexContext.getAccessor());
        }
        BlockUtils.swapPair(varsA, varsB, world, hexContext);

        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
