package com.riprod.hexcode.core.common.glyphs.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.riprod.hexcode.core.common.glyphs.utils.GlyphType;
import com.riprod.hexcode.core.state.drawing.component.DrawnShapeComponent;

public class GlyphAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, GlyphAsset>> {
    public static final AssetBuilderCodec<String, GlyphAsset> CODEC;
    private static AssetStore<String, GlyphAsset, DefaultAssetMap<String, GlyphAsset>> ASSET_STORE;
    public static final ValidatorCache<String> VALIDATOR_CACHE;

    protected AssetExtraInfo.Data data;
    protected String id;
    protected String modelPath;
    protected String imagePath;
    protected String title;
    protected String description;
    protected float basePower = 1.0f;
    protected int manaConsumption = 10;
    protected ArrayList<DrawnShapeComponent> shapes = new ArrayList<>();
    protected GlyphType glyphType;
    protected Map<String, SlotDefinition> inputs = new LinkedHashMap<>();
    protected Map<String, SlotDefinition> outputs = new LinkedHashMap<>();

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

    public GlyphType getGlyphType() {
        return this.glyphType;
    }

    public Map<String, SlotDefinition> getInputDefs() {
        return this.inputs;
    }

    public SlotDefinition getInputDef(String key) {
        return this.inputs.get(key);
    }

    public Set<String> getInputKeys() {
        return this.inputs.keySet();
    }

    public int getInputCount() {
        return this.inputs.size();
    }

    public Map<String, SlotDefinition> getOutputDefs() {
        return this.outputs;
    }

    public SlotDefinition getOutputDef(String key) {
        return this.outputs.get(key);
    }

    public Set<String> getOutputKeys() {
        return this.outputs.keySet();
    }

    public int getOutputCount() {
        return this.outputs.size();
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    static {
        CODEC = buildCodec();
        VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(GlyphAsset::getAssetStore));
    }

    @SuppressWarnings("unchecked")
    private static AssetBuilderCodec<String, GlyphAsset> buildCodec() {
        Codec<Map<String, SlotDefinition>> slotMapCodec =
                (Codec<Map<String, SlotDefinition>>) (Codec<?>) new MapCodec<>(SlotDefinition.CODEC, LinkedHashMap::new, false);
        return AssetBuilderCodec
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
                .addValidatorLate(() -> ModelAsset.VALIDATOR_CACHE.getValidator().late())
                .add()
                .<Float>appendInherited(new KeyedCodec<>("BasePower", Codec.FLOAT),
                        (a, v) -> a.basePower = v, a -> a.basePower,
                        (a, p) -> a.basePower = p.basePower)
                .add()
                .<String>appendInherited(new KeyedCodec<>("ImagePath", Codec.STRING),
                        (a, v) -> a.imagePath = v, a -> a.imagePath,
                        (a, p) -> a.imagePath = p.imagePath)
                .add()
                .<String>appendInherited(new KeyedCodec<>("Title", Codec.STRING),
                        (a, v) -> a.title = v, a -> a.title,
                        (a, p) -> a.title = p.title)
                .add()
                .<String>appendInherited(new KeyedCodec<>("Description", Codec.STRING),
                        (a, v) -> a.description = v, a -> a.description,
                        (a, p) -> a.description = p.description)
                .add()
                .<Integer>appendInherited(new KeyedCodec<>("ManaConsumption", Codec.INTEGER),
                        (a, v) -> a.manaConsumption = v, a -> a.manaConsumption,
                        (a, p) -> a.manaConsumption = p.manaConsumption)
                .add()
                .<GlyphType>appendInherited(new KeyedCodec<>("GlyphType", new EnumCodec<>(GlyphType.class)),
                        (a, v) -> a.glyphType = v, a -> a.glyphType,
                        (a, p) -> a.glyphType = p.glyphType)
                .addValidator(Validators.nonNull())
                .add()
                .<DrawnShapeComponent[]>appendInherited(
                        new KeyedCodec<>("ShapeStructure",
                                new ArrayCodec<>(DrawnShapeComponent.CODEC, DrawnShapeComponent[]::new)),
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
                .<Map<String, SlotDefinition>>appendInherited(
                        new KeyedCodec<>("Inputs", slotMapCodec),
                        (a, v) -> a.inputs = v != null ? new LinkedHashMap<>(v) : new LinkedHashMap<>(),
                        a -> a.inputs,
                        (a, p) -> a.inputs = new LinkedHashMap<>(p.inputs))
                .add()
                .<Map<String, SlotDefinition>>appendInherited(
                        new KeyedCodec<>("Outputs", slotMapCodec),
                        (a, v) -> a.outputs = v != null ? new LinkedHashMap<>(v) : new LinkedHashMap<>(),
                        a -> a.outputs,
                        (a, p) -> a.outputs = new LinkedHashMap<>(p.outputs))
                .add()
                .build();
    }
}
