package com.riprod.hexcode.core.common.imbuement.asset;

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
import com.riprod.hexcode.core.state.execution.component.HexContext;

import javax.annotation.Nullable;

public class EssenceAsset
        implements JsonAssetWithMap<String, DefaultAssetMap<String, EssenceAsset>> {

    public static final AssetBuilderCodec<String, EssenceAsset> CODEC;
    private static AssetStore<String, EssenceAsset, DefaultAssetMap<String, EssenceAsset>> ASSET_STORE;
    public static final ValidatorCache<String> VALIDATOR_CACHE;

    protected AssetExtraInfo.Data data;
    protected String id;
    @Nullable
    protected String element;
    @Nullable
    protected HexContext defaults;

    public static AssetStore<String, EssenceAsset, DefaultAssetMap<String, EssenceAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(EssenceAsset.class);
        }
        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, EssenceAsset> getAssetMap() {
        return (DefaultAssetMap<String, EssenceAsset>) getAssetStore().getAssetMap();
    }

    private EssenceAsset() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Nullable
    public String getElement() {
        return element;
    }

    @Nullable
    public HexContext getDefaults() {
        return defaults;
    }

    static {
        CODEC = AssetBuilderCodec
                .builder(EssenceAsset.class, EssenceAsset::new, Codec.STRING,
                        (asset, s) -> asset.id = s,
                        asset -> asset.id,
                        (asset, data) -> asset.data = data,
                        asset -> asset.data)
                .append(new KeyedCodec<>("Element", Codec.STRING),
                        (a, v) -> a.element = v,
                        a -> a.element)
                .documentation("Optional canonical label (e.g. \"flame\", \"frost\"). Free-form; downstream may switch on this.")
                .add()
                .append(new KeyedCodec<>("Defaults", HexContext.CODEC),
                        (a, v) -> a.defaults = v,
                        a -> a.defaults)
                .documentation("Cast overrides applied when this essence is consumed. Same shape as ImbuementProfileAsset.Defaults / ImbuementData.Overrides.")
                .add()
                .build();
        VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(EssenceAsset::getAssetStore));
    }
}
