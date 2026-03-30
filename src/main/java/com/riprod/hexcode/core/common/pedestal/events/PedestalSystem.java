package com.riprod.hexcode.core.common.pedestal.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.PlayerUtils;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hexes.utils.HexUtils;
import com.riprod.hexcode.api.event.EnterSelectingEvent;
import com.riprod.hexcode.api.event.HexcodeEvents;
import com.riprod.hexcode.core.common.obelisk.system.ObeliskDispatcher;
import com.riprod.hexcode.core.common.obelisk.component.ObeliskBlockComponent;
import com.riprod.hexcode.core.common.obelisk.system.ObeliskSystem;
import com.riprod.hexcode.core.common.obelisk.utils.ObeliskBlockUtil;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.common.pedestal.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.handlers.CraftingDragHandler;
import com.riprod.hexcode.core.state.crafting.handlers.DetailsHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Container.ContainerNodeHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Slot.SlotNodeHandler;
import com.riprod.hexcode.core.state.crafting.component.CraftingData;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.entity.AnchorEntity;
import com.riprod.hexcode.core.state.crafting.entity.PedestalEntity;
import com.riprod.hexcode.core.state.crafting.utils.CraftingDataUtil;
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
            CraftingData playerData) {

        Integer totalSlots = playerData.getBookSlots();
        if (totalSlots == null || totalSlots <= 0) {
            logger.atWarning().log("pedestal: SpawnHexPreviews aborted — bookSlots=%s, storedBook=%s, bookComponent=%s",
                    totalSlots, playerData.getStoredBook(), playerData.getStoredBookComponent());
            return;
        }

        List<Hex> hexes = playerData.getHexes();
        Ref<EntityStore> anchorRef = playerData.getAnchorRef();
        if (anchorRef == null || !anchorRef.isValid()) {
            logger.atWarning().log("pedestal: SpawnHexPreviews aborted — anchorRef=%s, valid=%s",
                    anchorRef, anchorRef != null ? anchorRef.isValid() : "null");
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
            if (hexRef == null) {
                logger.atWarning().log("pedestal: spawnFilledSlot returned null for slot %d", i);
            }
            spawnedRefs.add(hexRef);
        }

        playerData.setHexPreviewRefs(spawnedRefs);
    }

    public static void enterCrafting(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            PedestalBlockComponent pedestal, Ref<EntityStore> selectedAnchorNodeRef) {

        CraftingData playerData = pedestal.getCraftingDataComponent();

        List<Ref<EntityStore>> refs = playerData.getHexPreviewRefs();
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
                        GlyphComponent effect = buffer.getComponent(glyphRef,
                                GlyphComponent.getComponentType());
                        if (effect != null && effect.getNodeRef() != null
                                && effect.getNodeRef().isValid()) {
                            buffer.tryRemoveComponent(effect.getNodeRef(), MountedComponent.getComponentType());
                            buffer.tryRemoveEntity(effect.getNodeRef(), RemoveReason.REMOVE);
                        }
                        if (glyphRef.isValid()) {
                            buffer.tryRemoveComponent(glyphRef, MountedComponent.getComponentType());
                            buffer.tryRemoveEntity(glyphRef, RemoveReason.REMOVE);
                        }
                    }
                }
            }

            if (ref.isValid()) {
                buffer.tryRemoveComponent(ref, MountedComponent.getComponentType());
                buffer.tryRemoveEntity(ref, RemoveReason.REMOVE);
            }
        }

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(pedestal.getCraftingDataComponent().getPedestalLocation());
        Vector3d activePos = new Vector3d(
                anchorPos.x + ACTIVE_HEX_OFFSET.x,
                anchorPos.y + ACTIVE_HEX_OFFSET.y,
                anchorPos.z + ACTIVE_HEX_OFFSET.z);
        TransformComponent anchorTransform = buffer.getComponent(selectedAnchorNodeRef, TransformComponent.getComponentType());
        anchorTransform.getPosition().assign(activePos);
        anchorTransform.getRotation().assign(0, 0, 0);
        if (buffer.getComponent(selectedAnchorNodeRef, MountedComponent.getComponentType()) != null) {
            buffer.removeComponent(selectedAnchorNodeRef, MountedComponent.getComponentType());
        }

        playerData.setHexPreviewRefs(List.of(selectedAnchorNodeRef));

        World world = buffer.getExternalData().getWorld();
        updateState(buffer, pedestal, playerData, world, PedestalState.CRAFTING);
    }

    public static void saveHexToBook(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            CraftingData playerData) {

        int slotIndex = playerData.getActiveSlotIndex();
        if (slotIndex < 0) {
            logger.atWarning().log("pedestal: saveHexToBook — no active slot index");
            return;
        }

        List<Ref<EntityStore>> previewRefs = playerData.getHexPreviewRefs();
        if (previewRefs == null || previewRefs.isEmpty()) {
            logger.atWarning().log("pedestal: saveHexToBook — no hex preview refs");
            return;
        }

        Ref<EntityStore> activeHexRef = previewRefs.get(0);
        if (activeHexRef == null || !activeHexRef.isValid()) {
            logger.atWarning().log("pedestal: saveHexToBook — active hex ref invalid");
            return;
        }

        HexComponent hexComp = buffer.getComponent(activeHexRef, HexComponent.getComponentType());
        if (hexComp == null) {
            logger.atWarning().log("pedestal: saveHexToBook — no HexComponent on active hex");
            return;
        }

        Hex hex = hexComp.getHex().clone();
        HexUtils.compress(hex);

        HexBookComponent bookComp = playerData.getStoredBookComponent();
        if (bookComp == null) {
            logger.atWarning().log("pedestal: saveHexToBook — no book component");
            return;
        }

        if (hex.getGlyphs().isEmpty()) {
            if (slotIndex < bookComp.getHexes().size()) {
                bookComp.getHexes().remove(slotIndex);
                logger.atInfo().log("pedestal: removed empty hex from book — slot=%d", slotIndex);
            }
        } else {
            bookComp.setHex(slotIndex, hex);
            logger.atInfo().log("pedestal: saved hex to book — slot=%d, hex=%s", slotIndex, hex);
        }

        playerData.setStoredBookComponent(bookComp);
    }

    public static void saveHexToBook(Store<EntityStore> store, Ref<EntityStore> playerRef,
            CraftingData playerData) {

        int slotIndex = playerData.getActiveSlotIndex();
        if (slotIndex < 0) return;

        List<Ref<EntityStore>> previewRefs = playerData.getHexPreviewRefs();
        if (previewRefs == null || previewRefs.isEmpty()) return;

        Ref<EntityStore> activeHexRef = previewRefs.get(0);
        if (activeHexRef == null || !activeHexRef.isValid()) return;

        HexComponent hexComp = store.getComponent(activeHexRef, HexComponent.getComponentType());
        if (hexComp == null) return;

        Hex hex = hexComp.getHex().clone();
        HexUtils.compress(hex);

        HexBookComponent bookComp = playerData.getStoredBookComponent();
        if (bookComp == null) return;

        if (hex.getGlyphs().isEmpty()) {
            if (slotIndex < bookComp.getHexes().size()) {
                bookComp.getHexes().remove(slotIndex);
            }
        } else {
            bookComp.setHex(slotIndex, hex);
        }

        playerData.setStoredBookComponent(bookComp);
    }

    public static void exitCrafting(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            PedestalBlockComponent pedestal, CraftingData playerData) {

        ObeliskDispatcher.dispatchExitCrafting(buffer, pedestal, playerRef);
        saveHexToBook(buffer, playerRef, playerData);

        // cleanup slots
        SlotNodeHandler.INSTANCE.despawn(buffer, playerData);
        playerData.setSlotNodeRefs(new ArrayList<>());

        // cleanup dragging state
        HexcasterCraftingComponent craftingComp = buffer.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp != null) {
            CraftingDragHandler.endDrag(buffer, craftingComp.getDraggingRef(),
                    craftingComp.getHeadAnchorRef());
        }

        AnchorEntity.DespawnHexPreviews(buffer, pedestal, playerData);

        Ref<EntityStore> anchorNodeRef = playerData.getAnchorNodeRef();
        if (anchorNodeRef != null && anchorNodeRef.isValid()) {
            buffer.tryRemoveEntity(anchorNodeRef, RemoveReason.REMOVE);
            playerData.setAnchorNodeRef(null);
        }

        playerData.setActiveSlotIndex(-1);

        if (craftingComp != null) {
            craftingComp.clearCraftingState();
        }

    }

    public static void handleEssencePlacement(CommandBuffer<EntityStore> buffer,
            Player player, ItemStack essenceItem, HexSlot slot,
            PedestalBlockComponent pedestalComponent, CraftingData playerData,
            Vector3i blockPos) {

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(blockPos);

        Ref<EntityStore> oldEssenceRef = playerData.getEssenceDisplayRef();
        if (oldEssenceRef != null && oldEssenceRef.isValid()) {
            buffer.removeEntity(oldEssenceRef, RemoveReason.REMOVE);
        }

        Ref<EntityStore> newEssenceRef = PedestalEntity.spawnEssenceDisplay(
                buffer, pedestalComponent, playerData, anchorPos, essenceItem.getItem(),
                pedestalComponent.getReferenceHolder(), player.getReference());
        playerData.setEssenceDisplayRef(newEssenceRef);
        playerData.setEssence(essenceItem.getItem().getId());

        PlayerUtils.consumeOneFromHand(buffer, player.getReference(), slot);
    }

    public static void handleBookPlacement(CommandBuffer<EntityStore> buffer,
            Player player, ItemStack bookItem, HexSlot slot, PedestalBlockComponent pedestalComponent,
            CraftingData playerData, Vector3i blockPos) {

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(blockPos);

        ItemStack prepared = PedestalItemUtil.ensureHexBookComponent(bookItem);
        playerData.setStoredBook(prepared);
        playerData.setBookSourceSlot(slot);

        Ref<EntityStore> oldBookDisplay = playerData.getBookDisplayRef();
        if (oldBookDisplay != null && oldBookDisplay.isValid()) {
            buffer.removeEntity(oldBookDisplay, RemoveReason.REMOVE);
        }

        Ref<EntityStore> newBookDisplayRef = PedestalEntity.spawnBookDisplay(
                buffer, pedestalComponent, playerData, anchorPos, bookItem.getItem(), player.getReference());
        playerData.setBookDisplayRef(newBookDisplayRef);
        PlayerUtils.consumeOneFromHand(buffer, player.getReference(), slot);
    }

    public static void enterSelecting(PedestalBlockComponent pedestalComponent, Player player,
            World world, CommandBuffer<EntityStore> buffer) {

        CraftingData playerData = pedestalComponent.getCraftingDataComponent();

        if (playerData == null) {
            logger.atWarning().log("pedestal: enterSelecting aborted — playerData is null for player %s",
                    player.getReference());
            return;
        }

        PedestalSystem.SpawnHexPreviews(buffer, player.getReference(), pedestalComponent, playerData);

        HexcasterCraftingComponent craftingComp = buffer.getComponent(player.getReference(),
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) {
            craftingComp = new HexcasterCraftingComponent();
            buffer.putComponent(player.getReference(), HexcasterCraftingComponent.getComponentType(), craftingComp);
        }
        craftingComp.setPedestalLocation(playerData.getPedestalLocation());

        HexcasterComponent hexcaster = buffer.getComponent(player.getReference(),
                HexcasterComponent.getComponentType());
        if (hexcaster != null) {
            hexcaster.requestStateChange(HexState.CRAFTING);
            pedestalComponent.addDetectedPlayer(buffer, player.getReference());
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

        updateState(buffer, pedestalComponent, playerData, world, PedestalState.SELECTING);
        HexcodeEvents.fire(new EnterSelectingEvent(player.getReference(), pedestalComponent.getLocation()));
    }

    public static void enterIdle(CommandBuffer<EntityStore> buffer,
            Player player, PedestalBlockComponent pedestalComponent,
            World world) {

        CraftingData playerData = pedestalComponent.getCraftingDataComponent();

        AnchorEntity.DespawnHexPreviews(buffer, pedestalComponent, playerData);

        logger.atInfo().log("pedestal: entering idle at %s for player %s", pedestalComponent.getLocation(),
                player.getReference());

        Set<Ref<EntityStore>> activePlayers = pedestalComponent.getActivePlayerRefs();
        for (Ref<EntityStore> activePlayerRef : activePlayers) {
            if (activePlayerRef == null || !activePlayerRef.isValid()) {
                continue;
            }
            HexcasterComponent hexcaster = buffer.getComponent(activePlayerRef, HexcasterComponent.getComponentType());
            if (hexcaster != null
                    && (hexcaster.getState() == HexState.CRAFTING || hexcaster.getState() == HexState.DRAWING)) {
                hexcaster.requestStateChange(HexState.IDLE);
                logger.atInfo().log("pedestal: kicking player from %s on deactivation",
                        hexcaster.getState().toString());
            }
        }
        activePlayers.clear();

        SlotNodeHandler.INSTANCE.despawn(buffer, playerData);

        Vector3i blockPos = pedestalComponent.getLocation();

        Ref<EntityStore> ownerRef = playerData.getOwnerRef();
        ItemStack bookStack = playerData.getStoredBook();
        if (bookStack != null && !bookStack.isEmpty()) {
            if (ownerRef != null && ownerRef.isValid()) {
                PedestalItemUtil.returnBookToPlayer(buffer, ownerRef, bookStack, playerData.getBookSourceSlot());
            } else {
                PedestalItemUtil.dropBookAtPosition(buffer, bookStack, blockPos);
            }
            playerData.setStoredBook(ItemStack.EMPTY);
        }

        Ref<EntityStore> bookRef = playerData.getBookDisplayRef();
        if (bookRef != null && bookRef.isValid()) {
            buffer.removeEntity(bookRef, RemoveReason.REMOVE);
            playerData.setBookDisplayRef(null);
        }

        Ref<EntityStore> essenceRef = playerData.getEssenceDisplayRef();
        if (essenceRef != null && essenceRef.isValid()) {
            buffer.removeEntity(essenceRef, RemoveReason.REMOVE);
            playerData.setEssenceDisplayRef(null);
        }
        if (!pedestalComponent.isConsumeEssence()) {
            Ref<EntityStore> essenceReturnRef = (ownerRef != null && ownerRef.isValid()) ? ownerRef : null;
            boolean returned = essenceReturnRef != null && PedestalItemUtil.returnEssenceToPlayer(buffer, essenceReturnRef, playerData.getEssence());
            if (!returned && playerData.getEssence() != null) {
                PedestalItemUtil.dropEssenceAtPosition(buffer, playerData.getEssence(), blockPos);
            }
        }
        playerData.setEssence(null);
        playerData.setOwnerRef(null);

        AnchorEntity.DespawnHexPreviews(buffer, pedestalComponent, playerData);

        Ref<EntityStore> anchorNodeRef = playerData.getAnchorNodeRef();
        if (anchorNodeRef != null && anchorNodeRef.isValid()) {
            buffer.tryRemoveEntity(anchorNodeRef, RemoveReason.REMOVE);
            playerData.setAnchorNodeRef(null);
        }

        updateState(buffer, pedestalComponent, playerData, world, PedestalState.IDLE);
    }

    public static void handleReady(CommandBuffer<EntityStore> accessor, CraftingData playerData,
            PedestalBlockComponent pedestal,
            World world) {

        if (playerData.getStoredBook() == null || playerData.getEssence() == null
                || playerData.getStoredBook().isEmpty()) {
            // updateState(accessor, pedestal, world, PedestalState.IDLE);
            return;
        }

        // Update the state
        updateState(accessor, pedestal, playerData, world, PedestalState.READY);
    }

    public static void updateState(CommandBuffer<EntityStore> accessor, PedestalBlockComponent pedestal,
            CraftingData playerData, World world,
            PedestalState state) {

        PedestalState previousState = playerData.getState();

        Vector3i blockPos = pedestal.getLocation();

        String blockState = switch (state) {
            case IDLE -> "Idle";
            case READY -> "Ready";
            case SELECTING -> "Selecting";
            case CRAFTING -> "Crafting";
        };

        boolean canSwitch = canSwitchState(accessor, pedestal, state);

        playerData.setState(state);

        Ref<EntityStore> bookRef = playerData.getBookDisplayRef();
        if (bookRef != null && bookRef.isValid()) {
            AnimationUtils.playAnimation(bookRef, AnimationSlot.Action, blockState, accessor);
        }

        Ref<EntityStore> essenceRef = playerData.getEssenceDisplayRef();
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
