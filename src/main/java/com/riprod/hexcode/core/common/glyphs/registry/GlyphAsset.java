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
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.riprod.hexcode.core.state.drawing.component.DrawnShapeComponent;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

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
    protected int volatilityCost = 10;
    protected ArrayList<DrawnShapeComponent> shapes = new ArrayList<>();
    protected LinkedHashMap<String, SlotAsset> slots = new LinkedHashMap<>();

    private transient Object2IntOpenHashMap<String> slotIndexCache;

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

    public int getVolatilityCost() {
        return this.volatilityCost;
    }

    public List<DrawnShapeComponent> getShapes() {
        return this.shapes;
    }

    public Map<String, SlotAsset> getSlots() {
        return this.slots;
    }

    public SlotAsset getSlot(String key) {
        return this.slots.get(key);
    }

    public Set<String> getSlotKeys() {
        return this.slots.keySet();
    }

    public boolean hasSlot(String key) {
        return this.slots.containsKey(key);
    }

    public int getSlotCount() {
        return this.slots.size();
    }

    public int getSlotIndex(String key) {
        if (this.slotIndexCache == null) {
            this.slotIndexCache = buildSlotIndexCache();
        }
        return this.slotIndexCache.getOrDefault(key, -1);
    }

    private Object2IntOpenHashMap<String> buildSlotIndexCache() {
        Object2IntOpenHashMap<String> cache = new Object2IntOpenHashMap<>(this.slots.size());
        cache.defaultReturnValue(-1);
        int i = 0;
        for (String key : this.slots.keySet()) {
            cache.put(key, i++);
        }
        return cache;
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
        Codec<Map<String, SlotAsset>> slotMapCodec =
                (Codec<Map<String, SlotAsset>>) (Codec<?>) new MapCodec<>(SlotAsset.CODEC, LinkedHashMap::new, false);
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
                .<Integer>appendInherited(new KeyedCodec<>("VolatilityCost", Codec.INTEGER),
                        (a, v) -> a.volatilityCost = v, a -> a.volatilityCost,
                        (a, p) -> a.volatilityCost = p.volatilityCost)
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
                .<Map<String, SlotAsset>>appendInherited(
                        new KeyedCodec<>("Slots", slotMapCodec),
                        (a, v) -> {
                            a.slots = v != null ? new LinkedHashMap<>(v) : new LinkedHashMap<>();
                            a.slotIndexCache = null;
                        },
                        a -> a.slots,
                        (a, p) -> {
                            a.slots = new LinkedHashMap<>(p.slots);
                            a.slotIndexCache = null;
                        })
                .add()
                .build();
    }
}
