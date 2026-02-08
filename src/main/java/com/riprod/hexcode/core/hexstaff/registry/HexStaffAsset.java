package com.riprod.hexcode.core.hexstaff.registry;

import java.util.HashMap;
import java.util.Map;

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

public class HexStaffAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, HexStaffAsset>> {
    public static final AssetBuilderCodec<String, HexStaffAsset> CODEC;
    private static AssetStore<String, HexStaffAsset, DefaultAssetMap<String, HexStaffAsset>> ASSET_STORE;
    public static final ValidatorCache<String> VALIDATOR_CACHE;
    private static Map<String, HexStaffAsset> itemIdIndex;

    protected AssetExtraInfo.Data data;
    protected String id;
    protected String itemId;
    protected String castStyleId;

    public static AssetStore<String, HexStaffAsset, DefaultAssetMap<String, HexStaffAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(HexStaffAsset.class);
        }

        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, HexStaffAsset> getAssetMap() {
        return (DefaultAssetMap) getAssetStore().getAssetMap();
    }

    private HexStaffAsset() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getItemId() {
        return this.itemId;
    }

    public String getCastStyleId() {
        return this.castStyleId;
    }

    static {
        CODEC = AssetBuilderCodec
                .builder(HexStaffAsset.class, HexStaffAsset::new, Codec.STRING, (glyphAsset, s) -> {
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
                .append(new KeyedCodec<>("CastStyleId", Codec.STRING),
                        (a, v) -> a.castStyleId = v, a -> a.castStyleId)
                .add()
                .build();
        VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(HexStaffAsset::getAssetStore));
    }
}
