package com.riprod.hexcode.core.common.pedestal.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.EnterSelectingEvent;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.core.common.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.PlayerUtils;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hexes.utils.HexUtils;
import com.riprod.hexcode.core.common.obelisk.component.ObeliskBlockComponent;
import com.riprod.hexcode.core.common.obelisk.system.ObeliskDispatcher;
import com.riprod.hexcode.core.common.obelisk.system.ObeliskSystem;
import com.riprod.hexcode.core.common.obelisk.utils.ObeliskBlockUtil;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.common.pedestal.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.entity.AnchorEntity;
import com.riprod.hexcode.core.state.crafting.entity.PedestalEntity;
import com.riprod.hexcode.core.state.crafting.handlers.CraftingDragHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Container.ContainerNodeHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Slot.SlotNodeHandler;
import com.riprod.hexcode.core.state.crafting.session.HexcodeSessionComponent;
import com.riprod.hexcode.core.state.crafting.session.SessionUtils;
import com.riprod.hexcode.core.state.crafting.utils.PedestalItemUtil;
import com.riprod.hexcode.core.state.crafting.utils.RadialPositionUtil;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.utils.HexSlot;

import io.sentry.util.Pair;

public class PedestalSystem {

    public static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    public static final float PREVIEW_RADIUS = 3.5f;
    public static final Vector3f ACTIVE_HEX_OFFSET = new Vector3f(0, 1.3f, 0);
    public static final Vector3f HEX_SLOT_OFFSET = new Vector3f(0, -0.8f, 0);

    public static void SpawnHexPreviews(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            PedestalBlockComponent pedestal,
            HexcodeSessionComponent session) {

        Integer totalSlots = session.getBookSlots();
        if (totalSlots == null || totalSlots <= 0) {
            return;
        }

        List<Hex> hexes = session.getHexes();
        Ref<EntityStore> anchorRef = session.getAnchorRef();
        if (anchorRef == null || !anchorRef.isValid()) {
            return;
        }

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(pedestal.getLocation());
        List<Vector3f> offsets = RadialPositionUtil.calculateOffsets(totalSlots, PREVIEW_RADIUS, 0,
                HEX_SLOT_OFFSET);
        List<Ref<EntityStore>> spawnedRefs = new ArrayList<>();

        for (int i = 0; i < totalSlots; i++) {
            Vector3f offset = offsets.get(i);

            Hex hex = null;
            if (i < hexes.size()) {
                hex = hexes.get(i).clone();
            }
            Ref<EntityStore> hexRef = ContainerNodeHandler.INSTANCE.spawnContainer(buffer, hex, anchorRef,
                    anchorPos, offset, playerRef);
            spawnedRefs.add(hexRef);
        }

        session.setHexPreviewRefs(spawnedRefs);
    }

    public static void enterCrafting(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            PedestalBlockComponent pedestal, Ref<EntityStore> selectedAnchorNodeRef) {

        HexcodeSessionComponent session = SessionUtils.resolveSession(pedestal, buffer);
        if (session == null)
            return;

        List<Ref<EntityStore>> refs = session.getHexPreviewRefs();
        if (refs == null || refs.isEmpty()) {
            return;
        }

        for (Ref<EntityStore> ref : refs) {
            if (ref == null || !ref.isValid() || ref.equals(selectedAnchorNodeRef)) {
                continue;
            }

            HexComponent hexComp = buffer.getComponent(ref, HexComponent.getComponentType());
            if (hexComp != null) {
                Map<String, Ref<EntityStore>> childRefs = hexComp.getChildGlyphRefs();
                if (childRefs != null) {
                    for (Ref<EntityStore> glyphRef : childRefs.values()) {
                        if (glyphRef == null || !glyphRef.isValid())
                            continue;
                        SlotNodeHandler.INSTANCE.despawnSlotsForGlyph(buffer, glyphRef);
                        buffer.tryRemoveComponent(glyphRef, MountedComponent.getComponentType());
                        buffer.tryRemoveEntity(glyphRef, RemoveReason.REMOVE);
                    }
                }
            }

            if (ref.isValid()) {
                buffer.tryRemoveComponent(ref, MountedComponent.getComponentType());
                buffer.tryRemoveEntity(ref, RemoveReason.REMOVE);
            }
        }

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(session.getPedestalLocation());
        Vector3d activePos = new Vector3d(
                anchorPos.x + ACTIVE_HEX_OFFSET.x,
                anchorPos.y + ACTIVE_HEX_OFFSET.y,
                anchorPos.z + ACTIVE_HEX_OFFSET.z);
        TransformComponent anchorTransform = buffer.getComponent(selectedAnchorNodeRef,
                TransformComponent.getComponentType());
        anchorTransform.getPosition().assign(activePos);
        anchorTransform.getRotation().assign(0, 0, 0);
        if (buffer.getComponent(selectedAnchorNodeRef, MountedComponent.getComponentType()) != null) {
            buffer.removeComponent(selectedAnchorNodeRef, MountedComponent.getComponentType());
        }

        session.setHexPreviewRefs(List.of(selectedAnchorNodeRef));

        World world = buffer.getExternalData().getWorld();
        updateState(buffer, pedestal, session, world, PedestalState.CRAFTING);
    }

    public static void saveHexToBook(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            HexcodeSessionComponent session) {

        int slotIndex = session.getActiveSlotIndex();
        if (slotIndex < 0) {
            return;
        }

        List<Ref<EntityStore>> previewRefs = session.getHexPreviewRefs();
        if (previewRefs == null || previewRefs.isEmpty()) {
            return;
        }

        Ref<EntityStore> activeHexRef = previewRefs.get(0);
        if (activeHexRef == null || !activeHexRef.isValid()) {
            return;
        }

        HexComponent hexComp = buffer.getComponent(activeHexRef, HexComponent.getComponentType());
        if (hexComp == null) {
            return;
        }

        Hex hex = hexComp.getHex().clone();
        HexUtils.compress(hex);

        HexBookComponent bookComp = session.getStoredBookComponent();
        if (bookComp == null) {
            return;
        }

        if (hex.getGlyphs().isEmpty()) {
            if (slotIndex < bookComp.getHexes().size()) {
                bookComp.getHexes().remove(slotIndex);
            }
        } else {
            bookComp.setHex(slotIndex, hex);
        }

        session.setStoredBookComponent(bookComp);
    }

    public static void saveHexToBook(Store<EntityStore> store, Ref<EntityStore> playerRef,
            HexcodeSessionComponent session) {

        int slotIndex = session.getActiveSlotIndex();
        if (slotIndex < 0)
            return;

        List<Ref<EntityStore>> previewRefs = session.getHexPreviewRefs();
        if (previewRefs == null || previewRefs.isEmpty())
            return;

        Ref<EntityStore> activeHexRef = previewRefs.get(0);
        if (activeHexRef == null || !activeHexRef.isValid())
            return;

        HexComponent hexComp = store.getComponent(activeHexRef, HexComponent.getComponentType());
        if (hexComp == null)
            return;

        Hex hex = hexComp.getHex().clone();
        HexUtils.compress(hex);

        HexBookComponent bookComp = session.getStoredBookComponent();
        if (bookComp == null)
            return;

        if (hex.getGlyphs().isEmpty()) {
            if (slotIndex < bookComp.getHexes().size()) {
                bookComp.getHexes().remove(slotIndex);
            }
        } else {
            bookComp.setHex(slotIndex, hex);
        }

        session.setStoredBookComponent(bookComp);
    }

    public static void exitCrafting(CommandBuffer<EntityStore> accessor, Ref<EntityStore> playerRef,
            PedestalBlockComponent pedestal, HexcodeSessionComponent session) {

        ObeliskDispatcher.dispatchExitCrafting(accessor, pedestal, playerRef);
        saveHexToBook(accessor, playerRef, session);

        SlotNodeHandler.INSTANCE.despawn(accessor, session);
        session.setSlotNodeRefs(new ArrayList<>());

        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp != null) {
            CraftingDragHandler.endDrag(accessor, craftingComp.getDraggingRef(),
                    craftingComp.getHeadAnchorRef(), craftingComp);
        }

        AnchorEntity.DespawnHexPreviews(accessor, session);

        Ref<EntityStore> anchorNodeRef = session.getAnchorNodeRef();
        if (anchorNodeRef != null && anchorNodeRef.isValid()) {
            accessor.tryRemoveEntity(anchorNodeRef, RemoveReason.REMOVE);
            session.setAnchorNodeRef(null);
        }

        session.setActiveSlotIndex(-1);

        if (craftingComp != null) {
            craftingComp.clearCraftingState();
        }

        Ref<EntityStore> essenceRef = session.getEssenceDisplayRef();
        if (essenceRef != null && essenceRef.isValid()) {
            accessor.removeEntity(essenceRef, RemoveReason.REMOVE);
            session.setEssenceDisplayRef(null);
        }
        session.setEssence(null);
    }

    public static void handleEssencePlacement(CommandBuffer<EntityStore> buffer,
            Player player, ItemStack essenceItem, HexSlot slot,
            PedestalBlockComponent pedestalComponent, HexcodeSessionComponent session,
            Vector3i blockPos) {

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(blockPos);

        Ref<EntityStore> oldEssenceRef = session.getEssenceDisplayRef();
        if (oldEssenceRef != null && oldEssenceRef.isValid()) {
            buffer.removeEntity(oldEssenceRef, RemoveReason.REMOVE);
        }

        Ref<EntityStore> newEssenceRef = PedestalEntity.spawnEssenceDisplay(
                buffer, pedestalComponent, session,
                anchorPos, essenceItem.getItem(),
                pedestalComponent.getReferenceHolder(), player.getReference());
        session.setEssenceDisplayRef(newEssenceRef);
        session.setEssence(essenceItem.getItem().getId());

        PlayerUtils.consumeOneFromHand(buffer, player.getReference(), slot);
    }

    public static void handleBookPlacement(CommandBuffer<EntityStore> buffer,
            Player player, ItemStack bookItem, HexSlot slot, PedestalBlockComponent pedestalComponent,
            HexcodeSessionComponent session, Vector3i blockPos) {

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(blockPos);

        ItemStack prepared = PedestalItemUtil.ensureHexBookComponent(bookItem);
        session.setStoredBook(prepared);
        session.setBookSourceSlot(slot);

        pedestalComponent.setBookAssetId(bookItem.getItem().getId());

        Ref<EntityStore> oldBookDisplay = session.getBookDisplayRef();
        if (oldBookDisplay != null && oldBookDisplay.isValid()) {
            buffer.removeEntity(oldBookDisplay, RemoveReason.REMOVE);
        }

        Ref<EntityStore> newBookDisplayRef = PedestalEntity.spawnBookDisplay(
                buffer, pedestalComponent, session, anchorPos, bookItem.getItem(), player.getReference());
        session.setBookDisplayRef(newBookDisplayRef);
        PlayerUtils.consumeOneFromHand(buffer, player.getReference(), slot);
    }

    public static void enterSelecting(PedestalBlockComponent pedestalComponent, Player player,
            World world, CommandBuffer<EntityStore> buffer) {

        Ref<EntityStore> sessionRef = SessionUtils.getSessionRef(pedestalComponent);
        HexcodeSessionComponent session = sessionRef != null
                ? buffer.getComponent(sessionRef, HexcodeSessionComponent.getComponentType())
                : null;

        if (session == null) {
            return;
        }

        PedestalSystem.SpawnHexPreviews(buffer, player.getReference(), pedestalComponent, session);

        HexcasterCraftingComponent craftingComp = buffer.getComponent(player.getReference(),
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp != null) {
            craftingComp.setSessionRef(sessionRef);
        }

        HexcasterComponent hexcaster = buffer.getComponent(player.getReference(),
                HexcasterComponent.getComponentType());
        if (hexcaster != null) {
            hexcaster.requestStateChange(HexState.CRAFTING);
        }

        List<Pair<Vector3i, ObeliskBlockComponent>> obeliskPairs = ObeliskBlockUtil
                .getAvailableObelisks(pedestalComponent.getLocation(), pedestalComponent.getObeliskRange(),
                        world, pedestalComponent.getMaxObelisks());

        List<Vector3i> obelisks = new ArrayList<>();
        for (Pair<Vector3i, ObeliskBlockComponent> obeliskPair : obeliskPairs) {
            obelisks.add(obeliskPair.getFirst());
            obeliskPair.getSecond().setRegisteredPedestalLoc(pedestalComponent.getLocation());
        }

        List<Vector3i> removedObelisks = pedestalComponent.setActiveObelisks(obelisks);
        ObeliskSystem.cleanupObelisks(buffer, world, removedObelisks);

        updateState(buffer, pedestalComponent, session, world, PedestalState.SELECTING);
        HytaleServer.get().getEventBus().dispatchFor(EnterSelectingEvent.class)
                .dispatch(new EnterSelectingEvent(player.getReference(), pedestalComponent.getLocation()));
    }

    public static void handleReady(CommandBuffer<EntityStore> accessor, HexcodeSessionComponent session,
            PedestalBlockComponent pedestal,
            World world) {

        if (session.getStoredBook() == null || session.getStoredBook().isEmpty()) {
            return;
        }

        updateState(accessor, pedestal, session, world, PedestalState.READY);
    }

    public static void updateState(CommandBuffer<EntityStore> accessor, PedestalBlockComponent pedestal,
            HexcodeSessionComponent session, World world,
            PedestalState state) {

        PedestalState previousState = session.getState();

        Vector3i blockPos = pedestal.getLocation();

        String blockState = switch (state) {
            case IDLE -> "Idle";
            case READY -> "Ready";
            case SELECTING -> "Selecting";
            case CRAFTING -> "Crafting";
        };

        boolean canSwitch = canSwitchState(accessor, pedestal, state);

        session.setState(state);

        Ref<EntityStore> bookRef = session.getBookDisplayRef();
        if (bookRef != null && bookRef.isValid()) {
            AnimationUtils.playAnimation(bookRef, AnimationSlot.Action, blockState, accessor);
        }

        Ref<EntityStore> essenceRef = session.getEssenceDisplayRef();
        if (essenceRef != null && essenceRef.isValid()) {
            AnimationUtils.playAnimation(essenceRef, AnimationSlot.Action, blockState, accessor);
        }

        if (canSwitch) {
            PedestalBlockUtil.changeBlockState(world, blockPos, blockState);
            ObeliskSystem.updateState(accessor, pedestal, world, previousState, state);
        }
    }

    private static boolean canSwitchState(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestal, PedestalState newState) {
        return true;
    }
}
