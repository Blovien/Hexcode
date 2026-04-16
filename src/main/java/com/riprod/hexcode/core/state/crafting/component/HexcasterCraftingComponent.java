package com.riprod.hexcode.core.state.crafting.component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.CleanupUtils;
import com.riprod.hexcode.utils.HexSlot;

public class HexcasterCraftingComponent implements Component<EntityStore> {

    public static final BuilderCodec<HexcasterCraftingComponent> CODEC = BuilderCodec
            .builder(HexcasterCraftingComponent.class, HexcasterCraftingComponent::new)
            .append(new KeyedCodec<>("PersistedBookSnapshot", ItemStack.CODEC),
                    (c, v) -> c.persistedBookSnapshot = v,
                    c -> c.persistedBookSnapshot)
            .documentation("Autosaved hex book snapshot for crash recovery")
            .add()
            .append(new KeyedCodec<>("PersistedBookSourceSlot", new EnumCodec<>(HexSlot.class)),
                    (c, v) -> c.persistedBookSourceSlot = v,
                    c -> c.persistedBookSourceSlot)
            .documentation("Inventory slot the snapshot should be returned to on recovery")
            .add()
            .build();

    private static ComponentType<EntityStore, HexcasterCraftingComponent> componentType;

    public HexcasterCraftingComponent() {
    }

    public static void setComponentType(ComponentType<EntityStore, HexcasterCraftingComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexcasterCraftingComponent> getComponentType() {
        return componentType;
    }

    private Ref<EntityStore> sessionRef;
    private ItemStack persistedBookSnapshot;
    private HexSlot persistedBookSourceSlot;

    private Ref<EntityStore> headAnchorRef;
    private Ref<EntityStore> draggingRef;
    private Ref<EntityStore> hoveredRef;
    private int dragTickCount;

    @Nullable
    public Ref<EntityStore> getSessionRef() {
        return sessionRef;
    }

    public void setSessionRef(@Nullable Ref<EntityStore> sessionRef) {
        this.sessionRef = sessionRef;
    }

    public boolean hasActiveSession() {
        return sessionRef != null && sessionRef.isValid();
    }

    @Nullable
    public ItemStack getPersistedBookSnapshot() {
        return persistedBookSnapshot;
    }

    public void setPersistedBookSnapshot(@Nullable ItemStack snapshot) {
        this.persistedBookSnapshot = snapshot;
    }

    @Nullable
    public HexSlot getPersistedBookSourceSlot() {
        return persistedBookSourceSlot;
    }

    public void setPersistedBookSourceSlot(@Nullable HexSlot slot) {
        this.persistedBookSourceSlot = slot;
    }

    @Nullable
    public Ref<EntityStore> getHeadAnchorRef() {
        return headAnchorRef;
    }

    public void setHeadAnchorRef(CommandBuffer<EntityStore> accessor, @Nullable Ref<EntityStore> headAnchorRef) {
        CleanupUtils.safeRemoveEntity(accessor, this.headAnchorRef);
        this.headAnchorRef = headAnchorRef;
    }

    public Ref<EntityStore> setHeadAnchorRef(@Nullable Ref<EntityStore> headAnchorRef) {
        Ref<EntityStore> oldRef = this.headAnchorRef;
        this.headAnchorRef = headAnchorRef;
        return oldRef;
    }

    @Nullable
    public Ref<EntityStore> getDraggingRef() {
        return draggingRef;
    }

    public void setDraggingRef(@Nullable Ref<EntityStore> draggingNodeRef) {
        this.draggingRef = draggingNodeRef;
    }

    @Nullable
    public Ref<EntityStore> getHoveredRef() {
        return hoveredRef;
    }

    public void setHoveredRef(@Nullable Ref<EntityStore> hoveredGlyphRef) {
        this.hoveredRef = hoveredGlyphRef;
    }

    public int getDragTickCount() {
        return dragTickCount;
    }

    public void setDragTickCount(int dragTickCount) {
        this.dragTickCount = dragTickCount;
    }

    public void clearCraftingState() {
        this.headAnchorRef = null;
        this.draggingRef = null;
        this.hoveredRef = null;
        this.dragTickCount = 0;
    }

    public void clear(CommandBuffer<EntityStore> buffer) {
        CleanupUtils.safeRemoveEntity(buffer, this.headAnchorRef);
        this.headAnchorRef = null;
        this.draggingRef = null;
        this.hoveredRef = null;
        this.dragTickCount = 0;
        this.sessionRef = null;
    }

    @Nonnull
    @Override
    public HexcasterCraftingComponent clone() {
        HexcasterCraftingComponent copy = new HexcasterCraftingComponent();
        copy.sessionRef = this.sessionRef;
        copy.persistedBookSnapshot = this.persistedBookSnapshot;
        copy.persistedBookSourceSlot = this.persistedBookSourceSlot;
        copy.headAnchorRef = this.headAnchorRef;
        copy.draggingRef = this.draggingRef;
        copy.hoveredRef = this.hoveredRef;
        copy.dragTickCount = this.dragTickCount;
        return copy;
    }
}
