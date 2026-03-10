package com.riprod.hexcode.core.state.crafting.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIDefaultCollapsedState;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditorSectionStart;
import com.hypixel.hytale.common.util.MapUtil;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset.AnimationSet;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.riprod.hexcode.utils.HexSlot;

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
            .append(new KeyedCodec<>("PlayerData", new MapCodec<>(PedestalPlayerData.CODEC, HashMap::new)),
                    (c, v) -> c.playerData = v,
                    c -> c.playerData)
            .documentation("The current state of the pedestal")
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
    private Map<String, PedestalPlayerData> playerData = new HashMap<>();
    // transient
    private Vector3i location = null;
    private Map<String, Float> lastTickMap = new HashMap<>();
    private List<Vector3i> obeliskLocations = new ArrayList<>();
    private Set<Ref<EntityStore>> activePlayerRefs = new HashSet<>();

    public void setLocation(Vector3i location) {
        this.location = location;
    }

    public Vector3i getLocation() {
        return location;
    }

    @Nullable
    public String getEssenceItemId(String playerId) {
        return getPlayerData(playerId).getEssence();
    }

    public void setEssenceItemId(String playerId, @Nullable String essenceItemId) {
        getPlayerData(playerId).setEssence(essenceItemId);
    }

    public PedestalPlayerData getPlayerData(String playerId) {
        if (!(playerData instanceof HashMap)) {
            playerData = new HashMap<>(playerData);
        }
        return playerData.computeIfAbsent(playerId, k -> new PedestalPlayerData());
    }

    public Map<String, PedestalPlayerData> getPlayerData() {
        return playerData;
    }

    public List<PedestalPlayerData> getAllPlayerData() {
        return new ArrayList<>(playerData.values());
    }

    public void setPlayerData(String playerId, PedestalPlayerData data) {
        this.playerData.put(playerId, data);
    }

    public void removePlayerData(String playerId) {
        this.playerData.remove(playerId);
    }

    public boolean isConsumeEssence() {
        return consumeEssence;
    }

    public void setConsumeEssence(boolean consumeEssence) {
        this.consumeEssence = consumeEssence;
    }

    public int getMaxObelisks() {
        return maxObelisks;
    }

    public ItemStack getStoredBook(String playerId) {
        return getPlayerData(playerId).getStoredBook();
    }

    public void setStoredBook(String playerId, ItemStack storedBook) {
        getPlayerData(playerId).setStoredBook(storedBook);
    }

    public String getReferenceHolder() {
        return referenceHolder;
    }

    public void setReferenceHolder(String referenceHolder) {
        this.referenceHolder = referenceHolder;
    }

    public List<PedestalState> getStates() {
        return playerData.values().stream().map(PedestalPlayerData::getState).toList();
    }

    public Boolean addDetectedPlayer(CommandBuffer<EntityStore> accessor, Ref<EntityStore> playerRef) {
        if (activePlayerRefs.contains(playerRef)) {
            return false;
        }
        activePlayerRefs.add(playerRef);
        UUIDComponent uuidComponent = accessor.getComponent(playerRef, UUIDComponent.getComponentType());
        this.playerData.computeIfAbsent(uuidComponent.getUuid().toString(), k -> new PedestalPlayerData());
        return true;
    }

    public Boolean addDetectedPlayers(List<Ref<EntityStore>> playerRefs) {
        boolean added = false;
        for (Ref<EntityStore> playerRef : playerRefs) {
            if (!activePlayerRefs.contains(playerRef)) {
                activePlayerRefs.add(playerRef);
                added = true;
            }
        }
        return added;
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
        return copy;
    }
}
