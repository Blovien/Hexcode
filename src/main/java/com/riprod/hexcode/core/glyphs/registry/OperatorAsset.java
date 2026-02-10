package com.riprod.hexcode.core.glyphs.registry;

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

public class OperatorAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, OperatorAsset>> {
    public static final AssetBuilderCodec<String, OperatorAsset> CODEC;
    private static AssetStore<String, OperatorAsset, DefaultAssetMap<String, OperatorAsset>> ASSET_STORE;
    public static final ValidatorCache<String> VALIDATOR_CACHE;

    protected AssetExtraInfo.Data data;
    protected String id;
    protected String modelPath;
    protected String imagePath;

    public static AssetStore<String, OperatorAsset, DefaultAssetMap<String, OperatorAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(OperatorAsset.class);
        }

        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, OperatorAsset> getAssetMap() {
        return (DefaultAssetMap<String, OperatorAsset>) getAssetStore().getAssetMap();
    }

    private OperatorAsset() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getModelPath() {
        return this.modelPath;
    }

    public String getImagePath() {
        return this.imagePath;
    }

    static {
        CODEC = AssetBuilderCodec
                .builder(OperatorAsset.class, OperatorAsset::new, Codec.STRING, (OperatorAsset, s) -> {
                    OperatorAsset.id = s;
                }, (OperatorAsset) -> {
                    return OperatorAsset.id;
                }, (asset, data) -> {
                    asset.data = data;
                }, (asset) -> {
                    return asset.data;
                })
                .append(new KeyedCodec<>("ModelPath", Codec.STRING),
                        (a, v) -> a.modelPath = v, a -> a.modelPath)
                .add()
                .append(new KeyedCodec<>("ImagePath", Codec.STRING),
                        (a, v) -> a.imagePath = v, a -> a.imagePath)
                .add()
                .build();
        VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(OperatorAsset::getAssetStore));
    }
}
