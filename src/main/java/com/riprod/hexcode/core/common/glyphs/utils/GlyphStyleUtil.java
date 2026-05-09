package com.riprod.hexcode.core.common.glyphs.utils;

import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;

import java.awt.Color;

public class GlyphStyleUtil {

    private static final float[] QUALITY_STOPS = { 0.80f, 0.90f, 0.95f, 0.98f, 1.00f };
    private static final Color[] QUALITY_COLORS = {
            new Color(0x4A2D8A),
            new Color(0x7B5CFF),
            new Color(0x4F7BFF),
            new Color(0x5CD9FF),
            new Color(0xFFFFFF)
    };

    public static float getQualityScore(float volatility, float efficiency) {
        return (volatility + efficiency) / 2f;
    }

    public static Color getQualityColor(float volatility, float efficiency) {
        float q = getQualityScore(volatility, efficiency);
        if (q <= QUALITY_STOPS[0]) return QUALITY_COLORS[0];
        for (int i = 1; i < QUALITY_STOPS.length; i++) {
            if (q <= QUALITY_STOPS[i]) {
                float t = (q - QUALITY_STOPS[i - 1]) / (QUALITY_STOPS[i] - QUALITY_STOPS[i - 1]);
                return lerp(QUALITY_COLORS[i - 1], QUALITY_COLORS[i], t);
            }
        }
        return QUALITY_COLORS[QUALITY_COLORS.length - 1];
    }

    private static Color lerp(Color a, Color b, float t) {
        int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(clamp(r), clamp(g), clamp(bl));
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }

    public static EntityEffect getGlyphEffect(float volatility, float efficiency) {
        float combinedScore = getQualityScore(volatility, efficiency);

        String assetId;
        if (combinedScore >= 0.95f) {
            assetId = "Hexcode_Tint_Tier_8";
        } else if (combinedScore >= 0.92f) {
            assetId = "Hexcode_Tint_Tier_7";
        } else if (combinedScore >= 0.89f) {
            assetId = "Hexcode_Tint_Tier_6";
        } else if (combinedScore >= 0.86f) {
            assetId = "Hexcode_Tint_Tier_5";
        } else if (combinedScore >= 0.83f) {
            assetId = "Hexcode_Tint_Tier_4";
        } else if (combinedScore >= 0.80f) {
            assetId = "Hexcode_Tint_Tier_3";
        } else if (combinedScore >= 0.75f) {
            assetId = "Hexcode_Tint_Tier_2";
        } else {
            assetId = "Hexcode_Tint_Tier_1";
        }
        return EntityEffect.getAssetMap().getAsset(assetId);
    }
}
