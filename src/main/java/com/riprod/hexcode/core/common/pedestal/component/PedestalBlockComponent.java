package com.riprod.hexcode.core.common.pedestal.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset.AnimationSet;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

public class PedestalBlockComponent implements Component<ChunkStore> {

    public static final BuilderCodec<PedestalBlockComponent> CODEC = BuilderCodec
            .builder(PedestalBlockComponent.class, PedestalBlockComponent::new)
            .append(
                    new KeyedCodec<>("MaxObelisks", Codec.INTEGER),
                    (state, v) -> state.maxObelisks = v,
                    state -> state.maxObelisks)
            .documentation("The max amount of obelsisk that can be spawned")
            .add()
            .append(
                    new KeyedCodec<>("ObeliskRange", Codec.INTEGER),
                    (state, v) -> state.obeliskRange = v,
                    state -> state.obeliskRange)
            .documentation("The range obelisks are detected")
            .add()
            .append(
                    new KeyedCodec<>("PerPlayer", Codec.BOOLEAN),
                    (state, v) -> state.perPlayer = v,
                    state -> state.perPlayer)
            .documentation("Whether the pedestal should be activated per player or globally")
            .add()
            .append(
                    new KeyedCodec<>("MaxRadius", Codec.INTEGER),
                    (state, v) -> state.maxRadius = v,
                    state -> state.maxRadius)
            .documentation("The radius in which players are detected")
            .add()
            .appendInherited(new KeyedCodec<>("EssenceOffsetVector", Vector3f.CODEC),
                    (a, v) -> a.essenceOffset = v,
                    a -> a.essenceOffset,
                    (a, p) -> a.essenceOffset = p.essenceOffset)
            .documentation("Essence offset vector")
            .add()
            .appendInherited(new KeyedCodec<>("BookOffsetVector", Vector3f.CODEC),
                    (a, v) -> a.bookOffset = v,
                    a -> a.bookOffset,
                    (a, p) -> a.bookOffset = p.bookOffset)
            .documentation("Book offset from the center")
            .add()
            .append(new KeyedCodec<>("AnimationSets", new MapCodec<>(ModelAsset.AnimationSet.CODEC, HashMap::new)),
                    (model, m) -> model.animationSetMap = m,
                    (model) -> model.animationSetMap)
            .add()
            .append(new KeyedCodec<>("HolderReference", Codec.STRING), (c, v) -> c.referenceHolder = v,
                    c -> c.referenceHolder)
            .addValidatorLate(() -> ModelAsset.VALIDATOR_CACHE.getValidator().late())
            .documentation(
                    "A model that has the animations for the items (book/essence). Used for customizing the animation displays.")
            .add()
            .append(new KeyedCodec<>("Location", Vector3i.CODEC), (c, v) -> c.location = v, c -> c.location)
            .documentation("The location of the pedestal in the world.")
            .add()
            .build();

    private static ComponentType<ChunkStore, PedestalBlockComponent> componentType;

    public static void setComponentType(ComponentType<ChunkStore, PedestalBlockComponent> type) {
        componentType = type;
    }

    public static ComponentType<ChunkStore, PedestalBlockComponent> getComponentType() {
        return componentType;
    }

    private int maxObelisks = 4;
    private int maxRadius = 30;
    private boolean perPlayer = false;
    private int obeliskRange = 30;
    private String referenceHolder = "Pedestal_Holder";
    private Map<String, AnimationSet> animationSetMap = Collections.emptyMap();
    protected Vector3f essenceOffset = new Vector3f(0f, -0.5f, 0f);
    protected Vector3f bookOffset = new Vector3f(0f, 0.3f, 0f);
    private boolean consumeEssence = false;
    // transient
    private Vector3i location = null;
    private Map<String, Float> lastTickMap = new HashMap<>();
    private List<Vector3i> obeliskLocations = new ArrayList<>();
    private Set<Ref<EntityStore>> activePlayerRefs = new HashSet<>();
    private Ref<EntityStore> pedestalEntityRef;

    public void setLocation(Vector3i location) {
        this.location = location;
    }

    public Vector3i getLocation() {
        return location;
    }

    public void setPedestalEntityRef(Ref<EntityStore> pedestalEntityRef) {
        this.pedestalEntityRef = pedestalEntityRef;
    }

    public Ref<EntityStore> getPedestalEntityRef() {
        return pedestalEntityRef;
    }

    public boolean isConsumeEssence() {
        return consumeEssence;
    }

    public int getMaxObelisks() {
        return maxObelisks;
    }

    public String getReferenceHolder() {
        return referenceHolder;
    }

    public Boolean addDetectedPlayer(CommandBuffer<EntityStore> accessor, Ref<EntityStore> playerRef) {
        if (activePlayerRefs.contains(playerRef)) {
            return false;
        }
        activePlayerRefs.add(playerRef);
        return true;
    }

    public Set<Ref<EntityStore>> getActivePlayerRefs() {
        return activePlayerRefs;
    }

    public Boolean isActivePlayer(Ref<EntityStore> playerRef) {
        return activePlayerRefs.contains(playerRef);
    }

    public int getObeliskRange() {
        return obeliskRange;
    }

    public int getMaxRadius() {
        return maxRadius;
    }

    public int getActiveObeliskCount() {
        return obeliskLocations.size();
    }

    public List<Vector3i> getActiveObelisks() {
        return obeliskLocations;
    }

    public List<Vector3i> setActiveObelisks(List<Vector3i> obelisks) {
        List<Vector3i> removed = new ArrayList<>(obeliskLocations);
        removed.removeAll(obelisks);
        this.obeliskLocations = new ArrayList<>(obelisks);
        return removed;
    }

    public void addObelisk(Vector3i obeliskLoc) {
        this.obeliskLocations.add(obeliskLoc);
    }

    // style
    public Vector3f getBookOffset() {
        return this.bookOffset;
    }

    public Vector3f getEssenceOffset() {
        return this.essenceOffset;
    }

    public Map<String, AnimationSet> getAnimationSetMap() {
        return animationSetMap;
    }

    public float getTickLength(String keyId) {
        return this.lastTickMap.getOrDefault(keyId, 0f);
    }

    public void setTickLength(String keyId, float value) {
        this.lastTickMap.put(keyId, value);
    }

    public void incrementTickLength(String keyId, float dt) {
        this.lastTickMap.merge(keyId, dt, Float::sum);
    }

    public boolean isPerPlayer() {
        return perPlayer;
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        PedestalBlockComponent copy = new PedestalBlockComponent();
        copy.consumeEssence = this.consumeEssence;
        copy.maxObelisks = this.maxObelisks;
        copy.maxRadius = this.maxRadius;
        copy.perPlayer = this.perPlayer;
        copy.obeliskRange = this.obeliskRange;
        copy.referenceHolder = this.referenceHolder;
        copy.activePlayerRefs = new HashSet<>(this.activePlayerRefs);
        copy.bookOffset = this.bookOffset;
        copy.essenceOffset = this.essenceOffset;
        copy.location = this.location != null ? this.location.clone() : null;
        copy.animationSetMap = this.animationSetMap;
        copy.lastTickMap = new HashMap<>(this.lastTickMap);
        copy.obeliskLocations = new ArrayList<>(this.obeliskLocations);
        return copy;
    }
}
