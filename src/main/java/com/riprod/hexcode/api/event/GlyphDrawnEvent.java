package com.riprod.hexcode.api.event;

import java.util.List;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.state.drawing.component.DrawnShapeComponent;

public class GlyphDrawnEvent implements IEvent<Void> {

    private final Ref<EntityStore> playerRef;
    private final Glyph glyph;
    private final List<DrawnShapeComponent> drawnShapes;
    private final GlyphAsset matchedGlyphAsset;

    public GlyphDrawnEvent(Ref<EntityStore> playerRef, Glyph glyph,
            List<DrawnShapeComponent> drawnShapes, GlyphAsset matchedGlyphAsset) {
        this.playerRef = playerRef;
        this.glyph = glyph;
        this.drawnShapes = drawnShapes;
        this.matchedGlyphAsset = matchedGlyphAsset;
    }

    public Ref<EntityStore> getPlayerRef() {
        return playerRef;
    }

    public Glyph getGlyph() {
        return glyph;
    }

    public List<DrawnShapeComponent> getDrawnShapes() {
        return drawnShapes;
    }

    public GlyphAsset getMatchedGlyphAsset() {
        return matchedGlyphAsset;
    }
}
