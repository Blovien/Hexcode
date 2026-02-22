package com.riprod.hexcode.core.glyphs.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.riprod.hexcode.core.drawing.component.DrawnShapeComponent;

public class GlyphAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, GlyphAsset>> {
    public static final AssetBuilderCodec<String, GlyphAsset> CODEC;
    private static AssetStore<String, GlyphAsset, DefaultAssetMap<String, GlyphAsset>> ASSET_STORE;
    public static final ValidatorCache<String> VALIDATOR_CACHE;

    protected AssetExtraInfo.Data data;
    protected String id;
    protected String modelPath;
    protected String imagePath;
    protected float basePower = 1.0f;
    protected int manaConsumption = 10;
    protected ArrayList<DrawnShapeComponent> shapes = new ArrayList<>();

    public static AssetStore<String, GlyphAsset, DefaultAssetMap<String, GlyphAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(GlyphAsset.class);
        }

        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, GlyphAsset> getAssetMap() {
        return (DefaultAssetMap<String, GlyphAsset>) getAssetStore().getAssetMap();
    }

    private GlyphAsset() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getModelPath() {
        return this.modelPath;
    }

    public float getBasePower() {
        return this.basePower;
    }

    public int getManaConsumption() {
        return this.manaConsumption;
    }

    public List<DrawnShapeComponent> getShapes() {
        return this.shapes;
    }

    static {
        CODEC = AssetBuilderCodec
                .builder(GlyphAsset.class, GlyphAsset::new, Codec.STRING, (glyphAsset, s) -> {
                    glyphAsset.id = s;
                }, (glyphAsset) -> {
                    return glyphAsset.id;
                }, (asset, data) -> {
                    asset.data = data;
                }, (asset) -> {
                    return asset.data;
                })
                .<String>appendInherited(new KeyedCodec<>("ModelPath", Codec.STRING),
                        (a, v) -> a.modelPath = v, a -> a.modelPath,
                        (a, p) -> a.modelPath = p.modelPath)
                .add()
                .<Float>appendInherited(new KeyedCodec<>("BasePower", Codec.FLOAT),
                        (a, v) -> a.basePower = v, a -> a.basePower,
                        (a, p) -> a.basePower = p.basePower)
                .add()
                .<String>appendInherited(new KeyedCodec<>("ImagePath", Codec.STRING),
                        (a, v) -> a.imagePath = v, a -> a.imagePath,
                        (a, p) -> a.imagePath = p.imagePath)
                .add()
                .<Integer>appendInherited(new KeyedCodec<>("ManaConsumption", Codec.INTEGER),
                        (a, v) -> a.manaConsumption = v, a -> a.manaConsumption,
                        (a, p) -> a.manaConsumption = p.manaConsumption)
                .add()
                .<DrawnShapeComponent[]>appendInherited(new KeyedCodec<>("ShapeStructure", new ArrayCodec<>(DrawnShapeComponent.CODEC, DrawnShapeComponent[]::new)),
                        (c, v) -> {
                            if (v != null) {
                                c.shapes = new ArrayList<>(Arrays.asList(v));
                            } else {
                                c.shapes = new ArrayList<>();
                            }
                        },
                        c -> c.shapes.toArray(DrawnShapeComponent[]::new),
                        (a, p) -> a.shapes = new ArrayList<>(p.shapes))
                .add()
                .build();
        VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(GlyphAsset::getAssetStore));
    }
}
