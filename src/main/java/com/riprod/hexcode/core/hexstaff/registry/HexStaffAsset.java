package com.riprod.hexcode.core.hexstaff.registry;

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
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;

public class HexStaffAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, HexStaffAsset>> {
    public static final AssetBuilderCodec<String, HexStaffAsset> CODEC;
    private static AssetStore<String, HexStaffAsset, DefaultAssetMap<String, HexStaffAsset>> ASSET_STORE;
    public static final ValidatorCache<String> VALIDATOR_CACHE;

    protected AssetExtraInfo.Data data;
    protected String id;
    protected String itemId;
    protected String castStyleId;
    protected ModelParticle[] castingAuraParticles;
    protected ModelParticle[] craftingAuraParticles;

    public static AssetStore<String, HexStaffAsset, DefaultAssetMap<String, HexStaffAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(HexStaffAsset.class);
        }

        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, HexStaffAsset> getAssetMap() {
        return (DefaultAssetMap<String, HexStaffAsset>) getAssetStore().getAssetMap();
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

    public ModelParticle[] getCastingAuraParticles() {
        return this.castingAuraParticles;
    }

    public ModelParticle[] getCraftingAuraParticles() {
        return this.craftingAuraParticles;
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
                .<String>appendInherited(new KeyedCodec<>("ItemId", Codec.STRING),
                        (a, v) -> a.itemId = v, a -> a.itemId,
                        (a, p) -> a.itemId = p.itemId)
                .addValidator(Item.VALIDATOR_CACHE.getValidator())
                .add()
                .<String>appendInherited(new KeyedCodec<>("CastStyleId", Codec.STRING),
                        (a, v) -> a.castStyleId = v, a -> a.castStyleId,
                        (a, p) -> a.castStyleId = p.castStyleId)
                .add()
                .<ModelParticle[]>appendInherited(new KeyedCodec<>("CastingAuraParticles", ModelParticle.ARRAY_CODEC),
                        (a, v) -> a.castingAuraParticles = v, a -> a.castingAuraParticles,
                        (a, p) -> a.castingAuraParticles = p.castingAuraParticles)
                .add()
                .<ModelParticle[]>appendInherited(new KeyedCodec<>("CraftingAuraParticles", ModelParticle.ARRAY_CODEC),
                        (a, v) -> a.craftingAuraParticles = v, a -> a.craftingAuraParticles,
                        (a, p) -> a.craftingAuraParticles = p.craftingAuraParticles)
                .add()
                .build();
        VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(HexStaffAsset::getAssetStore));
    }
}
