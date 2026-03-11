package com.riprod.hexcode.core.common.glyphs.utils;

import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;

public class GlyphStyleUtil {
    public static EntityEffect getGlyphEffect(float volatility, float efficiency) {
        float combinedScore = (volatility + efficiency) / 2f;

        if (combinedScore >= 0.85f) {
            return EntityEffect.getAssetMap().getAsset("Hexcode_Tint_Purple");
        } else if (combinedScore >= 0.6f) {
            return EntityEffect.getAssetMap().getAsset("Hexcode_Tint_Arcane");
        } else {
            return EntityEffect.getAssetMap().getAsset("Hexcode_Tint_Red");
        }
    }
}
