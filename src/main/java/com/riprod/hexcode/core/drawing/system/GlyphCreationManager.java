package com.riprod.hexcode.core.drawing.system;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.drawing.component.DrawnShapeComponent;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;

public class GlyphCreationManager {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void NormalizeShapeSizes(List<DrawnShapeComponent> drawn) {
        float maxSize = 0f;
        for (DrawnShapeComponent shape : drawn) {
            maxSize = Math.max(maxSize, shape.getSize());
        }

        // normalize each to get relativeSize
        for (DrawnShapeComponent shape : drawn) {
            shape.setRelativeSize(shape.getSize() / maxSize);
        }
    }

    public static float ScoreAsset(List<DrawnShapeComponent> drawn, List<DrawnShapeComponent> asset) {
        // early return if the num shapes is wrong
        if (drawn.size() != asset.size())
            return 0f;

        float score = 0f;
        for (int i = 0; i < drawn.size(); i++) {
            DrawnShapeComponent d = drawn.get(i);
            DrawnShapeComponent a = asset.get(i);

            // shape type must match
            if (!d.getShapeId().equals(a.getShapeId()))
                return 0f;

            // score based on how close the relative sizes are
            float sizeDiff = Math.abs(d.getRelativeSize() - a.getRelativeSize());
            score += 1.0f - sizeDiff; // 1.0 = perfect, 0.0 = completely off
        }

        return score / drawn.size(); // average across all shapes
    }

    @Nullable
    public static GlyphAsset MatchGlyph(List<DrawnShapeComponent> drawn) {
        Map<String, GlyphAsset> assetMap = GlyphAsset.getAssetMap().getAssetMap();

        GlyphAsset bestMatch = null;
        float bestScore = 0f;
        float threshold = 0.7f;

        for (GlyphAsset asset : assetMap.values()) {
            float score = ScoreAsset(drawn, asset.getShapes());
            if (score > bestScore && score >= threshold) {
                bestScore = score;
                bestMatch = asset;
            }
            if (score > 0f) {
                LOGGER.atInfo().log("Scored glyph '%s' with %.2f accuracy", asset.getId(), score);
            }
        }

        LOGGER.atInfo().log("Best glyph match: " + (bestMatch != null ? bestMatch.getId() : "none") + " with score " + bestScore);

        return bestMatch; // null if nothing matched
    }

    public static GlyphComponent CreateGlyphComponent(GlyphAsset asset, float accuracy, long drawTimeMillis) {
        if (asset == null)
            return null;

        GlyphComponent component = new GlyphComponent(asset.getId(), accuracy, drawTimeMillis);
        // set the rest of the glyph component data
        return component;
    }
}
