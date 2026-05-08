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
import com.riprod.hexcode.core.common.hexes.registry.HexStyleAsset;
import com.riprod.hexcode.core.state.execution.component.HexColors;

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
    protected float volatilityMultiplier = 1.0f;
    @Nullable
    protected String styleId;

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

    public float getVolatilityMultiplier() {
        return volatilityMultiplier;
    }

    @Nullable
    public String getStyleId() {
        return styleId;
    }

    @Nullable
    public HexStyleAsset getStyle() {
        if (styleId == null) return null;
        return HexStyleAsset.getAssetMap().getAsset(styleId);
    }

    @Nullable
    public HexColors getColors() {
        HexStyleAsset style = getStyle();
        if (style == null) return null;
        HexColors c = new HexColors();
        if (style.getPrimaryColor() != null) c.setPrimaryColor(style.getPrimaryColor().clone());
        if (style.getSecondaryColor() != null) c.setSecondaryColor(style.getSecondaryColor().clone());
        c.setPrimaryAlpha(style.getAlphaOrDefault());
        return c;
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
                .append(new KeyedCodec<>("VolatilityMultiplier", Codec.FLOAT),
                        (a, v) -> a.volatilityMultiplier = v,
                        a -> a.volatilityMultiplier)
                .documentation("Multiplier applied to a block's cast volatility budget when this essence is consumed. 2.0 for concentrated variants.")
                .add()
                .append(new KeyedCodec<>("Style", HexStyleAsset.CHILD_ASSET_CODEC),
                        (a, v) -> a.styleId = v,
                        a -> a.styleId)
                .addValidatorLate(() -> HexStyleAsset.VALIDATOR_CACHE.getValidator().late())
                .documentation("HexStyleAsset id whose style overrides the base hex's style during this cast. Same shape as HexStaff/HexBook's Style field.")
                .add()
                .build();
        VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(EssenceAsset::getAssetStore));
    }
}
