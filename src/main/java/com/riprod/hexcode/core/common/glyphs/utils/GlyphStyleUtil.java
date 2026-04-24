package com.riprod.hexcode.core.common.glyphs.utils;

import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;

public class GlyphStyleUtil {
    public static EntityEffect getGlyphEffect(float volatility, float efficiency) {
        float combinedScore = (volatility + efficiency) / 2f;

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
