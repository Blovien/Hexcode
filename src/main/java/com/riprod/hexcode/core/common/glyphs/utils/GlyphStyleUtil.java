package com.riprod.hexcode.core.common.glyphs.utils;

import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;

public class GlyphStyleUtil {
    public static EntityEffect getGlyphEffect(float volatility, float efficiency) {
        EntityEffect tintEffect = null;
        float combinedScore = (volatility + efficiency) / 2f; // just an average

        if (combinedScore > 0.9f) {
            tintEffect = EntityEffect.getAssetMap().getAsset("Hexcode_Tint_Purple");
        }

        if (combinedScore > 0.7f) {
            tintEffect = EntityEffect.getAssetMap().getAsset("Hexcode_Tint_Arcane");
        }

        if (combinedScore > 0.5f) {
            tintEffect = EntityEffect.getAssetMap().getAsset("Hexcode_Tint_Red");
        }

        return tintEffect;
    }
}
