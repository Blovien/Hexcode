package com.riprod.hexcode.core.common.imbuement;

import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.utils.HexUtils;

public class ImbuementUtils {

    public static final String METADATA_KEY = "Imbuement";

    private ImbuementUtils() {
    }

    @Nullable
    public static ImbuementData read(@Nullable ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        return item.getFromMetadataOrNull(METADATA_KEY, ImbuementData.CODEC);
    }

    public static ItemStack write(ItemStack item, ImbuementData data) {
        return item.withMetadata(METADATA_KEY, ImbuementData.CODEC, data);
    }

    public static ItemStack clear(ItemStack item) {
        return item.withMetadata(METADATA_KEY, ImbuementData.CODEC, null);
    }

    @Nullable
    public static Hex resolveHex(ImbuementData data) {
        if (data.getHex() != null) return data.getHex().clone();
        if (data.getHexAssetId() != null) {
            Hex hex = HexUtils.deserialize(data.getHexAssetId());
            return hex != null ? hex.clone() : null;
        }
        return null;
    }

    public static ImbuementData fromHex(Hex hex) {
        ImbuementData data = new ImbuementData();
        data.setHex(hex);
        return data;
    }

    public static ImbuementData fromAsset(String hexAssetId) {
        ImbuementData data = new ImbuementData();
        data.setHexAssetId(hexAssetId);
        return data;
    }
}
