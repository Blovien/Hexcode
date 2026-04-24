package com.riprod.hexcode.core.state.crafting.session;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexbook.component.HexBookAsset;
import com.riprod.hexcode.core.common.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.state.crafting.constants.CraftingColors;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.HexSlot;

public class HexcodeSessionComponent implements Component<EntityStore> {

    public static final BuilderCodec<HexcodeSessionComponent> CODEC = BuilderCodec
            .builder(HexcodeSessionComponent.class, HexcodeSessionComponent::new)
            .append(new KeyedCodec<>("StoredBook", ItemStack.CODEC),
                    (c, v) -> c.storedBook = v,
                    c -> c.storedBook)
            .add()
            .append(new KeyedCodec<>("BookSourceSlot", new EnumCodec<>(HexSlot.class)),
                    (c, v) -> c.bookSourceSlot = v,
                    c -> c.bookSourceSlot)
            .add()
            .append(new KeyedCodec<>("PedestalLocation", Vector3i.CODEC),
                    (c, v) -> c.pedestalLocation = v,
                    c -> c.pedestalLocation)
            .add()
            .append(new KeyedCodec<>("EssenceItemId", Codec.STRING),
                    (c, v) -> c.essenceItemId = v,
                    c -> c.essenceItemId)
            .add()
            .build();

    private static ComponentType<EntityStore, HexcodeSessionComponent> componentType;

    public static void setComponentType(ComponentType<EntityStore, HexcodeSessionComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexcodeSessionComponent> getComponentType() {
        return componentType;
    }

    private UUID sessionId = UUID.randomUUID();
    private Vector3i pedestalLocation;
    private boolean isOpen = true;
    private Ref<EntityStore> ownerRef;
    private Set<Ref<EntityStore>> participantRefs = new HashSet<>();

    private ItemStack storedBook = ItemStack.EMPTY;
    private String essenceItemId = null;
    private HexSlot bookSourceSlot = HexSlot.MainHand;

    private Vector3f cachedGlyphColor = null;
    private Color cachedGlyphProtocolColor = null;

    private PedestalState blockState = PedestalState.IDLE;
    private Ref<EntityStore> anchorRef = null;
    private Ref<EntityStore> bookDisplayRef;
    private Ref<EntityStore> essenceDisplayRef;
    private List<Ref<EntityStore>> hexPreviewRefs = new ArrayList<>();
    private List<Ref<EntityStore>> slotNodeRefs = new ArrayList<>();
    private Ref<EntityStore> anchorNodeRef;

    private int activeSlotIndex = -1;
    private int autosaveTickCounter = 0;
    private Hex pendingImportHex = null;
    private int pendingReenterSlot = -1;

    public HexcodeSessionComponent() {
    }

    public HexcodeSessionComponent(Vector3i pedestalLocation, Ref<EntityStore> ownerRef, boolean isOpen) {
        this.pedestalLocation = pedestalLocation;
        this.ownerRef = ownerRef;
        this.isOpen = isOpen;
        if (ownerRef != null) {
            this.participantRefs.add(ownerRef);
        }
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        this.isOpen = open;
    }

    public Vector3i getPedestalLocation() {
        return pedestalLocation;
    }

    public void setPedestalLocation(Vector3i pedestalLocation) {
        this.pedestalLocation = pedestalLocation;
    }

    @Nullable
    public Ref<EntityStore> getOwnerRef() {
        return ownerRef;
    }

    public void setOwnerRef(@Nullable Ref<EntityStore> ownerRef) {
        this.ownerRef = ownerRef;
    }

    public boolean isOwner(@Nullable Ref<EntityStore> playerRef) {
        return ownerRef != null && ownerRef.equals(playerRef);
    }

    public Set<Ref<EntityStore>> getParticipantRefs() {
        return participantRefs;
    }

    public boolean addParticipant(@Nullable Ref<EntityStore> playerRef) {
        if (playerRef == null) return false;
        return participantRefs.add(playerRef);
    }

    public boolean removeParticipant(@Nullable Ref<EntityStore> playerRef) {
        if (playerRef == null) return false;
        return participantRefs.remove(playerRef);
    }

    public boolean isParticipant(@Nullable Ref<EntityStore> playerRef) {
        return playerRef != null && participantRefs.contains(playerRef);
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

    public HexBookComponent getStoredBookComponent() {
        if (storedBook == null || storedBook.isEmpty()) return null;
        return storedBook.getFromMetadataOrNull(CasterInventory.METADATA_KEY_HEX_BOOK, HexBookComponent.CODEC);
    }

    public void setStoredBookComponent(HexBookComponent bookComponent) {
        if (storedBook == null || storedBook.isEmpty()) return;
        this.storedBook = storedBook.withMetadata(CasterInventory.METADATA_KEY_HEX_BOOK, HexBookComponent.CODEC,
                bookComponent);
    }

    public Integer getBookSlots() {
        if (storedBook == null || storedBook.isEmpty()) return null;
        HexBookComponent bookComponent = getStoredBookComponent();
        if (bookComponent == null) return null;
        return bookComponent.getMaxCapacity();
    }

    public List<Hex> getHexes() {
        HexBookComponent bookComponent = getStoredBookComponent();
        if (bookComponent == null) return List.of();
        return bookComponent.getHexes();
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

    public Vector3f getGlyphColor() {
        return cachedGlyphColor != null ? cachedGlyphColor : CraftingColors.GLYPH_LINK;
    }

    public Color getGlyphProtocolColor() {
        return cachedGlyphProtocolColor;
    }

    public PedestalState getState() {
        return blockState;
    }

    public void setState(PedestalState state) {
        this.blockState = state;
    }

    public Ref<EntityStore> getAnchorRef() {
        return anchorRef;
    }

    public void setAnchorEntityRef(Ref<EntityStore> anchorEntityRef) {
        this.anchorRef = anchorEntityRef;
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

    public List<Ref<EntityStore>> getHexPreviewRefs() {
        return hexPreviewRefs;
    }

    public void setHexPreviewRefs(List<Ref<EntityStore>> refs) {
        this.hexPreviewRefs = refs;
    }

    public void clearHexPreviewRefs() {
        this.hexPreviewRefs = new ArrayList<>();
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

    public int getAutosaveTickCounter() {
        return autosaveTickCounter;
    }

    public void setAutosaveTickCounter(int count) {
        this.autosaveTickCounter = count;
    }

    public Hex getPendingImportHex() {
        return pendingImportHex;
    }

    public void setPendingImportHex(Hex hex) {
        this.pendingImportHex = hex;
    }

    public int getPendingReenterSlot() {
        return pendingReenterSlot;
    }

    public void setPendingReenterSlot(int slot) {
        this.pendingReenterSlot = slot;
    }

    public List<Ref<EntityStore>> getAllRefs() {
        List<Ref<EntityStore>> all = new ArrayList<>();
        if (bookDisplayRef != null && bookDisplayRef.isValid()) all.add(bookDisplayRef);
        if (essenceDisplayRef != null && essenceDisplayRef.isValid()) all.add(essenceDisplayRef);
        if (hexPreviewRefs != null) all.addAll(hexPreviewRefs);
        if (anchorNodeRef != null && anchorNodeRef.isValid()) all.add(anchorNodeRef);
        if (slotNodeRefs != null) all.addAll(slotNodeRefs);
        return all;
    }

    @Nonnull
    @Override
    public HexcodeSessionComponent clone() {
        HexcodeSessionComponent copy = new HexcodeSessionComponent();
        copy.sessionId = this.sessionId;
        copy.pedestalLocation = this.pedestalLocation != null ? this.pedestalLocation.clone() : null;
        copy.isOpen = this.isOpen;
        copy.ownerRef = this.ownerRef;
        copy.participantRefs = new HashSet<>(this.participantRefs);
        copy.storedBook = this.storedBook;
        copy.essenceItemId = this.essenceItemId;
        copy.bookSourceSlot = this.bookSourceSlot;
        copy.cachedGlyphColor = this.cachedGlyphColor != null ? this.cachedGlyphColor.clone() : null;
        copy.cachedGlyphProtocolColor = this.cachedGlyphProtocolColor;
        copy.blockState = this.blockState;
        copy.anchorRef = this.anchorRef;
        copy.bookDisplayRef = this.bookDisplayRef;
        copy.essenceDisplayRef = this.essenceDisplayRef;
        copy.hexPreviewRefs = this.hexPreviewRefs != null ? new ArrayList<>(this.hexPreviewRefs) : new ArrayList<>();
        copy.slotNodeRefs = this.slotNodeRefs != null ? new ArrayList<>(this.slotNodeRefs) : new ArrayList<>();
        copy.anchorNodeRef = this.anchorNodeRef;
        copy.activeSlotIndex = this.activeSlotIndex;
        copy.autosaveTickCounter = this.autosaveTickCounter;
        copy.pendingImportHex = this.pendingImportHex;
        copy.pendingReenterSlot = this.pendingReenterSlot;
        return copy;
    }
}
