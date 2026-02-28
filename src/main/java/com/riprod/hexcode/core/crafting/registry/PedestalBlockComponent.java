package com.riprod.hexcode.core.crafting.registry;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIDefaultCollapsedState;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditorSectionStart;
import com.hypixel.hytale.common.util.MapUtil;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset.AnimationSet;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.crafting.component.PedestalState;
import com.riprod.hexcode.core.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.hexes.component.Hex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
            .documentation("Particles that spawn while the pedestal is ready for activation")
            .add()
            .appendInherited(new KeyedCodec<>("BookOffsetVector", Vector3f.CODEC),
                    (a, v) -> a.bookOffset = v,
                    a -> a.bookOffset,
                    (a, p) -> a.bookOffset = p.bookOffset)
            .documentation("Particles that spawn when the pedestal is active")
            .add()
            .<Map<String, ModelAsset.AnimationSet>>appendInherited(new KeyedCodec<>("AnimationSets", new MapCodec<>(ModelAsset.AnimationSet.CODEC, HashMap::new)),
                    (model, m) -> model.animationSetMap = MapUtil.combineUnmodifiable(model.animationSetMap, m),
                    (model) -> model.animationSetMap,
                    (model, parent) -> model.animationSetMap = parent.animationSetMap)
            .metadata(new UIEditorSectionStart("Animations"))
            .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()
            .append(new KeyedCodec<>("StoredBook", ItemStack.CODEC), (c, v) -> c.storedBook = v, c -> c.storedBook)
            .documentation(
                    "The book currently stored in the pedestal. Set at runtime when a player places a book on the pedestal")
            .add()
            .append(new KeyedCodec<>("EssenceItemId", Codec.STRING), (c, v) -> c.essenceItemId = v,
                    c -> c.essenceItemId)
            .documentation(
                    "The essence item ID currently stored in the pedestal. Set at runtime when a player places a book on the pedestal")
            .add()
            .append(new KeyedCodec<>("HolderReference", Codec.STRING), (c, v) -> c.referenceHolder = v,
                    c -> c.referenceHolder)
            .addValidatorLate(() -> ModelAsset.VALIDATOR_CACHE.getValidator().late())
            .documentation(
                    "A model that has the animations for the items (book/essence). Used for customizing the animation displays.")
            .add().append(new KeyedCodec<>("Location", Vector3i.CODEC), (c, v) -> c.location = v, c -> c.location)
            .documentation("The location of the pedestal in the world.").add().build();

    private static ComponentType<ChunkStore, PedestalBlockComponent> componentType;

    public static void setComponentType(ComponentType<ChunkStore, PedestalBlockComponent> type) {
        componentType = type;
    }

    public static ComponentType<ChunkStore, PedestalBlockComponent> getComponentType() {
        return componentType;
    }

    private ItemStack storedBook = ItemStack.EMPTY;
    private String essenceItemId = null;
    private int maxObelisks = 4;
    private int maxRadius = 30;
    private boolean perPlayer = false;
    private int obeliskRange = 30;
    private String referenceHolder = null;
    private Map<String, AnimationSet> animationSetMap = Collections.emptyMap();
    protected Vector3f essenceOffset;
    protected Vector3f bookOffset;
    // transient
    private PedestalState blockState = PedestalState.OFF;
    private Hex activeHex = null;
    private Ref<EntityStore> activeHexEntityRef;
    private Ref<EntityStore> anchorEntityRef;
    private Ref<EntityStore> bookDisplayRef;
    private Ref<EntityStore> essenceDisplayRef;
    private List<Ref<EntityStore>> activePlayerRefs = new ArrayList<>();
    private List<Ref<EntityStore>> hexPreviewRefs = new ArrayList<>();
    private Vector3i location = null;

    public void setLocation(Vector3i location) {
        this.location = location;
    }

    public Vector3i getLocation() {
        return location;
    }

    @Nullable
    public String getEssenceItemId() {
        return essenceItemId;
    }

    public void setEssenceItemId(@Nullable String essenceItemId) {
        this.essenceItemId = essenceItemId;
    }

    public int getMaxObelisks() {
        return maxObelisks;
    }

    public ItemStack getStoredBook() {
        return storedBook;
    }

    public void setStoredBook(ItemStack storedBook) {
        this.storedBook = storedBook;
    }

    public HexBookComponent getStoredBookComponent() {
        if (storedBook == null || storedBook.isEmpty()) {
            return null;
        }

        return storedBook.getFromMetadataOrNull(CasterInventory.METADATA_KEY_HEX_BOOK, HexBookComponent.CODEC);
    }

    public Integer getBookSlots() {
        if (storedBook == null || storedBook.isEmpty()) {
            return null;
        }

        HexBookComponent bookComponent = getStoredBookComponent();

        if (bookComponent == null) {
            return null;
        }

        return bookComponent.getMaxCapacity();
    }

    public Hex getActiveHex() {
        return activeHex;
    }

    public void setActiveHex(@Nullable Hex hex) {
        this.activeHex = hex;
    }

    public Ref<EntityStore> getActiveHexEntityRef() {
        return activeHexEntityRef;
    }

    public void setActiveHexEntityRef(@Nullable Ref<EntityStore> ref) {
        this.activeHexEntityRef = ref;
    }

    public List<Ref<EntityStore>> getHexPreviewRefs() {
        return hexPreviewRefs;
    }

    public void setHexPreviewRefs(List<Ref<EntityStore>> refs) {
        this.hexPreviewRefs = refs;
    }

    public void clearHexPreviewRefs() {
        this.hexPreviewRefs = new ArrayList<>();
    }

    public List<Hex> getHexes() {
        HexBookComponent bookComponent = getStoredBookComponent();
        if (bookComponent == null) {
            return List.of();
        }

        return bookComponent.getHexes();
    }

    public String getReferenceHolder() {
        return referenceHolder;
    }

    public void setReferenceHolder(String referenceHolder) {
        this.referenceHolder = referenceHolder;
    }

    public PedestalState getState() {
        return blockState;
    }

    public void setState(PedestalState state) {
        this.blockState = state;
    }

    public Ref<EntityStore> getAnchorRef() {
        return anchorEntityRef;
    }

    public void setAnchorEntityRef(Ref<EntityStore> anchorEntityRef) {
        this.anchorEntityRef = anchorEntityRef;
    }

    public Ref<EntityStore> getBookDisplayRef() {
        return bookDisplayRef;
    }

    public void setBookDisplayRef(Ref<EntityStore> bookDisplayRef) {
        this.bookDisplayRef = bookDisplayRef;
    }

    public Ref<EntityStore> getEssenceDisplayRef() {
        return essenceDisplayRef;
    }

    public void setEssenceDisplayRef(Ref<EntityStore> essenceDisplayRef) {
        this.essenceDisplayRef = essenceDisplayRef;
    }

    public Boolean addDetectedPlayer(Ref<EntityStore> playerRef) {
        if (activePlayerRefs.contains(playerRef)) {
            return false;
        }
        activePlayerRefs.add(playerRef);
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

    public List<Ref<EntityStore>> getActivePlayerRefs() {
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
        return 0; // implement
    }

    // style
    public Vector3f getBlockOffset() {
        return this.bookOffset;
    }

    public Vector3f getEssenceOffset() {
        return this.essenceOffset;
    }

    public Map<String, AnimationSet> getAnimationSetMap() {
        return animationSetMap;
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        PedestalBlockComponent copy = new PedestalBlockComponent();
        copy.essenceItemId = this.essenceItemId;
        copy.maxObelisks = this.maxObelisks;
        copy.maxRadius = this.maxRadius;
        copy.perPlayer = this.perPlayer;
        copy.obeliskRange = this.obeliskRange;
        copy.referenceHolder = this.referenceHolder;
        copy.storedBook = this.storedBook;
        copy.blockState = this.blockState;
        copy.activeHex = this.activeHex;
        copy.activeHexEntityRef = this.activeHexEntityRef;
        copy.anchorEntityRef = this.anchorEntityRef;
        copy.bookDisplayRef = this.bookDisplayRef;
        copy.essenceDisplayRef = this.essenceDisplayRef;
        copy.activePlayerRefs = new ArrayList<>(this.activePlayerRefs);
        copy.hexPreviewRefs = new ArrayList<>(this.hexPreviewRefs);
        copy.bookOffset = this.bookOffset;
        copy.essenceOffset = this.essenceOffset;
        copy.location = this.location != null ? this.location.clone() : null;
        return copy;
    }
}
