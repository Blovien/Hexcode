package com.riprod.hexcode.builtin.glyphs.warp;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.builtin.glyphs.warp.style.WarpStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.BlockUtils;
import com.riprod.hexcode.utils.SpellVarUtil;

public class WarpGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
public String getId() { return ID; };

public static final String ID = "Warp";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.readSlot(WarpGlyphSlots.TARGET, hexContext);
        HexVar destInput = glyph.readSlot(WarpGlyphSlots.DESTINATION, hexContext);

        if (destInput == null) {
            LOGGER.atWarning().log("warp glyph: no destination provided");
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        Vector3d destination = SpellVarUtil.resolvePosition(destInput, hexContext.getAccessor());

        if (destination == null) {
            LOGGER.atWarning().log("warp glyph: could not resolve destination");
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        if (targets == null) {
            LOGGER.atWarning().log("warp glyph: no targets to warp");
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        World world = hexContext.getAccessor().getExternalData().getWorld();

        Vector3d departurePos = SpellVarUtil.resolvePosition(targets, hexContext.getAccessor());
        BlockUtils.moveToDestination(targets, destination, world, hexContext);
        if (departurePos != null) {
            WarpStyle.render(departurePos, destination, hexContext.getColors(), hexContext.getAccessor());
        }

        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
