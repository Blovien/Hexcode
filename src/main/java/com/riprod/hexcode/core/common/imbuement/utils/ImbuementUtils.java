package com.riprod.hexcode.core.common.imbuement.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.saved.SavedHexAsset;
import com.riprod.hexcode.core.common.hexes.utils.HexUtils;
import com.riprod.hexcode.core.common.imbuement.ImbuementMetadata;
import com.riprod.hexcode.core.common.imbuement.asset.ImbuementProfileAsset;
import com.riprod.hexcode.core.common.imbuement.component.ImbuementData;
import com.riprod.hexcode.core.common.imbuement.registry.ImbuementProfileRegistry;

public class ImbuementUtils {

    /** @deprecated use {@link ImbuementMetadata#KEY} */
    @Deprecated public static final String METADATA_KEY = ImbuementMetadata.KEY;
    /** @deprecated use {@link ImbuementMetadata#DEFAULT_SLOT} */
    @Deprecated public static final String DEFAULT_SLOT = ImbuementMetadata.DEFAULT_SLOT;

    private ImbuementUtils() {
    }

    // slot-keyed API

    public static Map<String, ImbuementData> readAll(@Nullable ItemStack item) {
        if (item == null || item.isEmpty()) return Collections.emptyMap();
        Map<String, ImbuementData> map = item.getFromMetadataOrNull(ImbuementMetadata.KEY, ImbuementMetadata.CODEC);
        return map != null ? map : Collections.emptyMap();
    }

    @Nullable
    public static ImbuementData read(@Nullable ItemStack item, String slotKey) {
        Map<String, ImbuementData> map = readAll(item);
        return map.get(slotKey);
    }

    public static ItemStack write(ItemStack item, String slotKey, @Nullable ImbuementData data) {
        Map<String, ImbuementData> map = new HashMap<>(readAll(item));
        if (data == null) map.remove(slotKey);
        else map.put(slotKey, data);
        if (map.isEmpty()) {
            return item.withMetadata(ImbuementMetadata.KEY, ImbuementMetadata.CODEC, null);
        }
        return item.withMetadata(ImbuementMetadata.KEY, ImbuementMetadata.CODEC, map);
    }

    public static ItemStack clear(ItemStack item, String slotKey) {
        return write(item, slotKey, null);
    }

    public static ItemStack writeAll(ItemStack item, @Nullable Map<String, ImbuementData> slots) {
        if (slots == null || slots.isEmpty()) {
            return item.withMetadata(ImbuementMetadata.KEY, ImbuementMetadata.CODEC, null);
        }
        return item.withMetadata(ImbuementMetadata.KEY, ImbuementMetadata.CODEC, new HashMap<>(slots));
    }

    @Nullable
    public static ImbuementProfileAsset resolveProfile(@Nullable ItemStack item) {
        return ImbuementProfileRegistry.first(item);
    }

    // legacy slotless API — delegates to DEFAULT_SLOT for backwards compat

    @Nullable
    public static ImbuementData read(@Nullable ItemStack item) {
        return read(item, ImbuementMetadata.DEFAULT_SLOT);
    }

    public static ItemStack write(ItemStack item, ImbuementData data) {
        return write(item, ImbuementMetadata.DEFAULT_SLOT, data);
    }

    public static ItemStack clear(ItemStack item) {
        return item.withMetadata(ImbuementMetadata.KEY, ImbuementMetadata.CODEC, null);
    }

    // hex resolution / construction helpers

    @Nullable
    public static Hex resolveHex(ImbuementData data) {
        if (data.getHex() != null) return data.getHex().clone();
        if (data.getHexCompressedId() != null) {
            Hex hex = HexUtils.deserialize(data.getHexCompressedId());
            if (hex != null) return hex.clone();
        }
        if (data.getHexAssetId() != null) {
            SavedHexAsset saved = SavedHexAsset.getAssetMap().getAsset(data.getHexAssetId());
            if (saved != null && saved.getHex() != null) return saved.getHex().clone();
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
        data.setHexCompressedId(hexAssetId);
        return data;
    }
}
