package com.riprod.hexcode.core.hexbook.registry;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;

public class HexBookAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, HexBookAsset>> {
    public static final AssetBuilderCodec<String, HexBookAsset> CODEC;
    private static AssetStore<String, HexBookAsset, DefaultAssetMap<String, HexBookAsset>> ASSET_STORE;
    public static final ValidatorCache<String> VALIDATOR_CACHE;

    protected AssetExtraInfo.Data data;
    protected String id;
    protected String itemId;
    protected int maxGlyphs = 10;

    public static AssetStore<String, HexBookAsset, DefaultAssetMap<String, HexBookAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(HexBookAsset.class);
        }

        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, HexBookAsset> getAssetMap() {
        return (DefaultAssetMap<String, HexBookAsset>) getAssetStore().getAssetMap();
    }

    private HexBookAsset() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getItemId() {
        return this.itemId;
    }

    public int getMaxGlyphs() {
        return this.maxGlyphs;
    }

    static {
        CODEC = AssetBuilderCodec
                .builder(HexBookAsset.class, HexBookAsset::new, Codec.STRING, (glyphAsset, s) -> {
                    glyphAsset.id = s;
                }, (glyphAsset) -> {
                    return glyphAsset.id;
                }, (asset, data) -> {
                    asset.data = data;
                }, (asset) -> {
                    return asset.data;
                })
                .append(new KeyedCodec<>("ItemId", Codec.STRING),
                        (a, v) -> a.itemId = v, a -> a.itemId)
                .add()
                .append(new KeyedCodec<>("MaxGlyphs", Codec.INTEGER),
                        (a, v) -> a.maxGlyphs = v, a -> a.maxGlyphs)
                .add()
                .build();
        VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(HexBookAsset::getAssetStore));
    }
}
