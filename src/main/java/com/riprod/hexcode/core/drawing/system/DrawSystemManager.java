package com.riprod.hexcode.core.drawing.system;

import java.util.List;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.drawing.component.DrawnShapeComponent;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.hexbook.component.HexBookComponent;
import com.riprod.hexcode.player.component.HexcasterComponent;
import com.riprod.hexcode.player.system.CasterInventory;

public class DrawSystemManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static InteractionState EnterDrawingMode(ComponentAccessor<EntityStore> accessor,
            HexcasterComponent hexcaster,
            Ref<EntityStore> playerRef) {

        // Setup logic
        return InteractionState.NotFinished;
    }

    public static InteractionState ExitDrawingMode(ComponentAccessor<EntityStore> accessor,
            HexcasterComponent hexcaster,
            Ref<EntityStore> playerRef) {

        List<DrawnShapeComponent> drawnShapes = hexcaster.getDrawnGlyphs();
        if (drawnShapes == null || drawnShapes.isEmpty()) {
            // shape detection and cleanup logic
            hexcaster.clearDrawing();
            hexcaster.clearTrailRef();
            return InteractionState.Finished; // nothing drawn, exit normally
        }

        GlyphCreationManager.NormalizeShapeSizes(drawnShapes);

        GlyphAsset matchedGlyph = GlyphCreationManager.MatchGlyph(drawnShapes);

        // create glyph component from matched asset (if any)
        GlyphComponent glyphComponent = GlyphCreationManager.CreateGlyphComponent(matchedGlyph, 1.0f, 1l);

        if (glyphComponent == null) {
            // no match, maybe give feedback to player?
            LOGGER.atInfo().log("Player drew a shape but it did not match any known glyph.");
            hexcaster.clearDrawing();
            hexcaster.clearTrailRef();
            return InteractionState.Finished;
        }

        // add it to your book. TODO: Fix this after multiblock is made
        HexBookComponent bookComponent = CasterInventory.getHexBookComponent(accessor, playerRef);
        bookComponent.addGlyph(glyphComponent);
        CasterInventory.saveHexBookComponent(accessor, playerRef, bookComponent);

        LOGGER.atInfo().log("Player drew a shape matching glyph '%s' with score %.2f",
                matchedGlyph != null ? matchedGlyph.getId() : "None",
                matchedGlyph != null ? GlyphCreationManager.ScoreAsset(drawnShapes, matchedGlyph.getShapes()) : 0f);

        // Cleanup
        hexcaster.clearDrawing();
        hexcaster.clearTrailRef();

        return InteractionState.Finished;
    }

    public static InteractionState DrawingTick(ComponentAccessor<EntityStore> accessor,
            HexcasterComponent hexcaster,
            Ref<EntityStore> playerRef) {
        // Tick while drawing (sfx, animations, etc)

        if (!hexcaster.isInDrawingMode()) {
            return InteractionState.Finished;
        }

        // spawn the shape particles along the path
        InterfaceManager.createIndicator(accessor, playerRef, hexcaster);

        return InteractionState.NotFinished;
    }
}
