package com.riprod.hexcode.core.common.glyphs.registry;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;

public class StyleAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, StyleAsset>> {
    public static final AssetBuilderCodec<String, StyleAsset> CODEC;
    private static AssetStore<String, StyleAsset, DefaultAssetMap<String, StyleAsset>> ASSET_STORE;
    public static final ValidatorCache<String> VALIDATOR_CACHE;

    protected AssetExtraInfo.Data data;
    protected String id;
    protected Vector3f color = new Vector3f(1f, 1f, 1f);
    protected DebugShape shape = DebugShape.Cube;
    protected String nodeHandlerId = "slot.standard";

    public static AssetStore<String, StyleAsset, DefaultAssetMap<String, StyleAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(StyleAsset.class);
        }
        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, StyleAsset> getAssetMap() {
        return (DefaultAssetMap<String, StyleAsset>) getAssetStore().getAssetMap();
    }

    private StyleAsset() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    public Vector3f getColor() {
        return this.color;
    }

    public DebugShape getShape() {
        return this.shape;
    }

    public String getNodeHandlerId() {
        return this.nodeHandlerId;
    }

    static {
        CODEC = AssetBuilderCodec
                .builder(StyleAsset.class, StyleAsset::new, Codec.STRING, (asset, s) -> {
                    asset.id = s;
                }, (asset) -> {
                    return asset.id;
                }, (asset, data) -> {
                    asset.data = data;
                }, (asset) -> {
                    return asset.data;
                })
                .append(new KeyedCodec<>("Color", Codec.FLOAT_ARRAY),
                        (a, v) -> {
                            if (v != null && v.length >= 3) a.color = new Vector3f(v[0], v[1], v[2]);
                        },
                        a -> new float[] { a.color.x, a.color.y, a.color.z })
                .add()
                .append(new KeyedCodec<>("Shape", new EnumCodec<>(DebugShape.class)),
                        (a, v) -> {
                            if (v != null) a.shape = v;
                        },
                        a -> a.shape)
                .add()
                .append(new KeyedCodec<>("NodeHandlerId", Codec.STRING),
                        (a, v) -> {
                            if (v != null) a.nodeHandlerId = v;
                        },
                        a -> a.nodeHandlerId)
                .add()
                .build();
        VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(StyleAsset::getAssetStore));
    }
}
