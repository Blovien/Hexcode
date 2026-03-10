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

public class PedestalPlayerData implements Component<ChunkStore> {

    public static final BuilderCodec<PedestalPlayerData> CODEC = BuilderCodec
            .builder(PedestalPlayerData.class, PedestalPlayerData::new)
            .append(new KeyedCodec<>("StoredBook", ItemStack.CODEC), (c, v) -> c.storedBook = v, c -> c.storedBook)
            .documentation(
                    "The book currently stored in the pedestal. Set at runtime when a player places a book on the pedestal")
            .add()
            .append(new KeyedCodec<>("EssenceItemId", Codec.STRING), (c, v) -> c.essenceItemId = v,
                    c -> c.essenceItemId)
            .documentation(
                    "The essence item ID currently stored in the pedestal. Set at runtime when a player places a book on the pedestal")
            .add()
            .append(new KeyedCodec<>("State", new EnumCodec<>(PedestalState.class)),
                    (c, v) -> c.blockState = v, c -> c.blockState)
            .documentation("The current state of the pedestal")
            .add()
            .build();

    private static ComponentType<ChunkStore, PedestalPlayerData> componentType;

    public static void setComponentType(ComponentType<ChunkStore, PedestalPlayerData> type) {
        componentType = type;
    }

    public static ComponentType<ChunkStore, PedestalPlayerData> getComponentType() {
        return componentType;
    }

    // persistent
    private ItemStack storedBook = ItemStack.EMPTY;
    private String essenceItemId = null;

    // transient
    private HexSlot bookSourceSlot = HexSlot.MainHand;

    // per player
    private PedestalState blockState = PedestalState.IDLE;
    private Hex activeHex = null;
    private Ref<EntityStore> activeHexEntityRef;
    private Ref<EntityStore> bookDisplayRef;
    private Ref<EntityStore> essenceDisplayRef;
    private List<Ref<EntityStore>> hexPreviewRefs = new ArrayList<>();
    private Ref<EntityStore> anchorRef = null;

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

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        PedestalPlayerData copy = new PedestalPlayerData();
        copy.essenceItemId = this.essenceItemId;
        copy.bookSourceSlot = this.bookSourceSlot;
        copy.storedBook = this.storedBook;
        copy.blockState = this.blockState;
        copy.anchorRef = this.anchorRef;
        copy.activeHex = this.activeHex;
        copy.activeHexEntityRef = this.activeHexEntityRef;
        copy.bookDisplayRef = this.bookDisplayRef;
        copy.essenceDisplayRef = this.essenceDisplayRef;
        copy.hexPreviewRefs = new ArrayList<>(this.hexPreviewRefs);
        return copy;
    }
}
