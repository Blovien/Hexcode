package com.riprod.hexcode.core.common.armor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

public class ArmorManaConfig implements JsonAssetWithMap<String, DefaultAssetMap<String, ArmorManaConfig>> {

    public static final String ASSET_PATH = "Hexcode/ArmorMana";

    public static final AssetBuilderCodec<String, ArmorManaConfig> CODEC;
    private static AssetStore<String, ArmorManaConfig, DefaultAssetMap<String, ArmorManaConfig>> ASSET_STORE;

    protected AssetExtraInfo.Data data;
    protected String id;
    @Nonnull
    protected Map<String, Integer> tiers = Collections.emptyMap();

    public static AssetStore<String, ArmorManaConfig, DefaultAssetMap<String, ArmorManaConfig>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(ArmorManaConfig.class);
        }
        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, ArmorManaConfig> getAssetMap() {
        return (DefaultAssetMap<String, ArmorManaConfig>) getAssetStore().getAssetMap();
    }

    private ArmorManaConfig() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Nonnull
    public Map<String, Integer> getTiers() {
        return this.tiers;
    }

    static {
        CODEC = buildCodec();
    }

    @SuppressWarnings("unchecked")
    private static AssetBuilderCodec<String, ArmorManaConfig> buildCodec() {
        Codec<Map<String, Integer>> tierMapCodec =
                (Codec<Map<String, Integer>>) (Codec<?>) new MapCodec<>(Codec.INTEGER, HashMap::new, false);
        return AssetBuilderCodec
                .builder(ArmorManaConfig.class, ArmorManaConfig::new, Codec.STRING,
                        (asset, s) -> asset.id = s,
                        asset -> asset.id,
                        (asset, d) -> asset.data = d,
                        asset -> asset.data)
                .append(new KeyedCodec<>("Tiers", tierMapCodec),
                        (a, v) -> a.tiers = v != null ? v : Collections.emptyMap(),
                        a -> a.tiers)
                .add()
                .build();
    }
}
