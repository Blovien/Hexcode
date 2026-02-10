package com.riprod.hexcode.core.drawing.component;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;

public class DrawnShapeComponent implements JsonAssetWithMap<String, DefaultAssetMap<String, GlyphAsset>> {

    public static final AssetBuilderCodec<String, DrawnShapeComponent> CODEC;
    public static final ValidatorCache<String> VALIDATOR_CACHE;

    protected AssetExtraInfo.Data data;
    protected String id;
    private String glyphId;
    private float size;
    private float relativeSize;
    private float accuracy;

    public DrawnShapeComponent(String shapeId, float size, float relativeSize, float accuracy) {
        this.glyphId = shapeId;
        this.size = size;
        this.relativeSize = relativeSize;
        this.accuracy = accuracy;
    }

    public DrawnShapeComponent(String glyphId, float accuracy) {
        this(glyphId, 1.0f, 1.0f, accuracy);
    }

    private DrawnShapeComponent() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getGlyphId() {
        return glyphId;
    }

    public float getSize() {
        return size;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public float getRelativeSize() {
        return relativeSize;
    }

    public void setGlyphId(String glyphId) {
        this.glyphId = glyphId;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public void setRelativeSize(float relativeSize) {
        this.relativeSize = relativeSize;
    }

    static {
        CODEC = AssetBuilderCodec
                .builder(DrawnShapeComponent.class, DrawnShapeComponent::new, Codec.STRING, (glyphAsset, s) -> {
                    glyphAsset.id = s;
                }, (glyphAsset) -> {
                    return glyphAsset.id;
                }, (asset, data) -> {
                    asset.data = data;
                }, (asset) -> {
                    return asset.data;
                })
                .append(new KeyedCodec<>("GlyphID", Codec.STRING),
                        (a, v) -> a.glyphId = v, a -> a.glyphId)
                .add()
                .append(new KeyedCodec<>("Size", Codec.FLOAT),
                        (a, v) -> a.size = v, a -> a.size)
                .add()
                .build();
        VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(GlyphAsset::getAssetStore));
    }
}
