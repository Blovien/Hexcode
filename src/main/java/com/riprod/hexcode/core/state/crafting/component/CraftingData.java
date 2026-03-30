package com.riprod.hexcode.core.state.crafting.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.Color;
import com.riprod.hexcode.core.common.hexbook.component.HexBookAsset;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.crafting.constants.CraftingColors;
import com.riprod.hexcode.utils.HexSlot;

public class CraftingData {

    public static final BuilderCodec<CraftingData> CODEC = BuilderCodec
            .builder(CraftingData.class, CraftingData::new)
            .append(new KeyedCodec<>("StoredBook", ItemStack.CODEC), (c, v) -> c.storedBook = v, c -> c.storedBook)
            .documentation(
                    "The book currently stored in the pedestal. Set at runtime when a player places a book on the pedestal")
            .add()
            .append(new KeyedCodec<>("EssenceItemId", Codec.STRING), (c, v) -> c.essenceItemId = v,
                    c -> c.essenceItemId)
            .documentation(
                    "The essence item ID currently stored in the pedestal. Set at runtime when a player places a book on the pedestal")
            .documentation("The current state of the pedestal")
            .add()
            .append(new KeyedCodec<>("Location", Vector3i.CODEC), (c, v) -> c.pedestalLocation = v, c -> c.pedestalLocation)
            .documentation("The location of the pedestal in the world.")
            .add()
            .build();

    public CraftingData() {
    }

    public CraftingData(Vector3i pedestalLocation) {
        this.pedestalLocation = pedestalLocation;
        this.blockState = PedestalState.IDLE;
        this.anchorNodeRef = null;
        this.bookDisplayRef = null;
        this.essenceDisplayRef = null;
    }

    // persistent
    private ItemStack storedBook = ItemStack.EMPTY;
    private String essenceItemId = null;

    // transient
    private HexSlot bookSourceSlot = HexSlot.MainHand;
    private Vector3f cachedGlyphColor = null;
    private Color cachedGlyphProtocolColor = null;

    // pedestal state
    private PedestalState blockState = PedestalState.IDLE;
    private Ref<EntityStore> bookDisplayRef;
    private Ref<EntityStore> essenceDisplayRef;
    private List<Ref<EntityStore>> hexPreviewRefs = new ArrayList<>();
    private Ref<EntityStore> anchorRef = null;
    private Vector3i pedestalLocation = null;
    private Ref<EntityStore> ownerRef = null;
    private List<Ref<EntityStore>> slotNodeRefs = new ArrayList<>();
    private Ref<EntityStore> anchorNodeRef;
    private int activeSlotIndex = -1;
    private int autosaveTickCounter = 0;
    public static final int AUTOSAVE_INTERVAL = 600;

    public List<Ref<EntityStore>> getAllRefs() {
        List<Ref<EntityStore>> allRefs = new ArrayList<>();
        if (bookDisplayRef != null && bookDisplayRef.isValid()) {
            allRefs.add(bookDisplayRef);
        }
        if (essenceDisplayRef != null && essenceDisplayRef.isValid()) {
            allRefs.add(essenceDisplayRef);
        }
        if (hexPreviewRefs != null) {
            allRefs.addAll(hexPreviewRefs);
        }
        if (anchorNodeRef != null && anchorNodeRef.isValid()) {
            allRefs.add(anchorNodeRef);
        }
        if (slotNodeRefs != null) {
            allRefs.addAll(slotNodeRefs);
        }
        return allRefs;
    }

    public Ref<EntityStore> getAnchorRef() {
        return anchorRef;
    }

    public void setAnchorEntityRef(Ref<EntityStore> anchorEntityRef) {
        this.anchorRef = anchorEntityRef;
    }

    public String getEssence() {
        return essenceItemId;
    }

    public void setEssence(String essenceItemId) {
        this.essenceItemId = essenceItemId;
    }

    public HexSlot getBookSourceSlot() {
        return bookSourceSlot;
    }

    public void setBookSourceSlot(HexSlot bookSourceSlot) {
        this.bookSourceSlot = bookSourceSlot;
    }

    public ItemStack getStoredBook() {
        return storedBook;
    }

    public void setStoredBook(ItemStack storedBook) {
        this.storedBook = storedBook;
        this.cachedGlyphColor = null;
        this.cachedGlyphProtocolColor = null;
        HexBookAsset bookAsset = CasterInventory.getHexBookAsset(storedBook);
        if (bookAsset != null && bookAsset.getColors() != null
                && bookAsset.getColors().getPrimaryColor() != null) {
            this.cachedGlyphProtocolColor = bookAsset.getColors().getPrimaryColor();
            this.cachedGlyphColor = HexColors.toVector3f(this.cachedGlyphProtocolColor);
        }
    }

    public Vector3f getGlyphColor() {
        return cachedGlyphColor != null ? cachedGlyphColor : CraftingColors.GLYPH_LINK;
    }

    public Color getGlyphProtocolColor() {
        return cachedGlyphProtocolColor;
    }

    public HexBookComponent getStoredBookComponent() {
        if (storedBook == null || storedBook.isEmpty()) {
            return null;
        }

        return storedBook.getFromMetadataOrNull(CasterInventory.METADATA_KEY_HEX_BOOK, HexBookComponent.CODEC);
    }

    public void setStoredBookComponent(HexBookComponent bookComponent) {
        if (storedBook == null || storedBook.isEmpty()) {
            return;
        }
        this.storedBook = storedBook.withMetadata(CasterInventory.METADATA_KEY_HEX_BOOK, HexBookComponent.CODEC,
                bookComponent);
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

    public PedestalState getState() {
        return blockState;
    }

    public void setState(PedestalState state) {
        this.blockState = state;
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

    public List<Ref<EntityStore>> getSlotNodeRefs() {
        return slotNodeRefs;
    }

    public void setSlotNodeRefs(List<Ref<EntityStore>> slotNodeRefs) {
        this.slotNodeRefs = slotNodeRefs;
    }

    public Ref<EntityStore> getAnchorNodeRef() {
        return anchorNodeRef;
    }

    public void setAnchorNodeRef(Ref<EntityStore> anchorNodeRef) {
        this.anchorNodeRef = anchorNodeRef;
    }

    public int getActiveSlotIndex() {
        return activeSlotIndex;
    }

    public void setActiveSlotIndex(int activeSlotIndex) {
        this.activeSlotIndex = activeSlotIndex;
    }

    public Ref<EntityStore> getOwnerRef() {
        return ownerRef;
    }

    public void setOwnerRef(Ref<EntityStore> ownerRef) {
        this.ownerRef = ownerRef;
    }

    public boolean isOwner(Ref<EntityStore> playerRef) {
        return ownerRef != null && ownerRef.equals(playerRef);
    }

    public int getAutosaveTickCounter() {
        return autosaveTickCounter;
    }

    public void setAutosaveTickCounter(int count) {
        this.autosaveTickCounter = count;
    }

    public void setPedestalLocation(Vector3i pedestalLocation) {
        this.pedestalLocation = pedestalLocation;
    }

    public Vector3i getPedestalLocation() {
        return pedestalLocation;
    }

    public CraftingData clone() {
        CraftingData copy = new CraftingData();
        copy.storedBook = this.storedBook;
        copy.essenceItemId = this.essenceItemId;
        copy.bookSourceSlot = this.bookSourceSlot;
        copy.cachedGlyphColor = this.cachedGlyphColor != null ? this.cachedGlyphColor.clone() : null;
        copy.cachedGlyphProtocolColor = this.cachedGlyphProtocolColor;
        copy.blockState = this.blockState;
        copy.bookDisplayRef = this.bookDisplayRef;
        copy.essenceDisplayRef = this.essenceDisplayRef;
        copy.hexPreviewRefs = this.hexPreviewRefs != null ? new ArrayList<>(this.hexPreviewRefs) : new ArrayList<>();
        copy.anchorRef = this.anchorRef;
        copy.pedestalLocation = this.pedestalLocation != null ? this.pedestalLocation.clone() : null;
        copy.ownerRef = this.ownerRef;
        copy.slotNodeRefs = this.slotNodeRefs != null ? new ArrayList<>(this.slotNodeRefs) : new ArrayList<>();
        copy.anchorNodeRef = this.anchorNodeRef;
        copy.activeSlotIndex = this.activeSlotIndex;
        return copy;
    }
}
