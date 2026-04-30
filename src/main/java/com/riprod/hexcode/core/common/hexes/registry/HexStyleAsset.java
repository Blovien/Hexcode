package com.riprod.hexcode.core.common.hexes.registry;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.riprod.hexcode.core.state.execution.component.HexColors;

public class HexStyleAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, HexStyleAsset>> {
    public static final AssetBuilderCodec<String, HexStyleAsset> CODEC;
    public static final Codec<String> CHILD_ASSET_CODEC;
    private static AssetStore<String, HexStyleAsset, DefaultAssetMap<String, HexStyleAsset>> ASSET_STORE;
    public static final ValidatorCache<String> VALIDATOR_CACHE;

    protected AssetExtraInfo.Data data;
    protected String id;
    protected HexColors colors;

    public static AssetStore<String, HexStyleAsset, DefaultAssetMap<String, HexStyleAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(HexStyleAsset.class);
        }
        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, HexStyleAsset> getAssetMap() {
        return (DefaultAssetMap<String, HexStyleAsset>) getAssetStore().getAssetMap();
    }

    private HexStyleAsset() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    public HexColors getColor() {
        return this.colors;
    }

    static {
        CODEC = AssetBuilderCodec
                .builder(HexStyleAsset.class, HexStyleAsset::new, Codec.STRING, (asset, s) -> {
                    asset.id = s;
                }, (asset) -> {
                    return asset.id;
                }, (asset, data) -> {
                    asset.data = data;
                }, (asset) -> {
                    return asset.data;
                })
                .appendInherited(new KeyedCodec<>("Colors", HexColors.CODEC),
                        (a, v) -> a.colors = v,
                        a -> a.colors,
                        (a, p) -> {
                            if (p.colors != null)
                                a.colors = p.colors.clone();
                        })
                .add()
                .build();
        CHILD_ASSET_CODEC = new ContainedAssetCodec<>(HexStyleAsset.class, CODEC);
        VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(HexStyleAsset::getAssetStore));
    }
}
