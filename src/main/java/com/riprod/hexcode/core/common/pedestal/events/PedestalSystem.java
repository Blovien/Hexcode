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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.block.component.UnbreakableBlockComponent;
import com.riprod.hexcode.core.common.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.PlayerUtils;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.api.event.EnterSelectingEvent;
import com.riprod.hexcode.api.event.HexcodeEvents;
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
import com.riprod.hexcode.core.state.crafting.component.CraftingDataComponent;
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
            CraftingDataComponent playerData) {

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

        CraftingDataComponent playerData = CraftingDataUtil.getPedestalData(buffer, playerRef);

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
                            buffer.tryRemoveEntity(effect.getNodeRef(), RemoveReason.REMOVE);
                        }
                        buffer.tryRemoveEntity(glyphRef, RemoveReason.REMOVE);
                    }
                }
            }

            buffer.tryRemoveEntity(ref, RemoveReason.REMOVE);
        }

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(pedestal.getLocation());
        Vector3d activePos = new Vector3d(
                anchorPos.x + ACTIVE_HEX_OFFSET.x,
                anchorPos.y + ACTIVE_HEX_OFFSET.y,
                anchorPos.z + ACTIVE_HEX_OFFSET.z);
        buffer.putComponent(selectedAnchorNodeRef, TransformComponent.getComponentType(),
                new TransformComponent(activePos, new Vector3f(0, 0, 0)));
        if (buffer.getComponent(selectedAnchorNodeRef, MountedComponent.getComponentType()) != null) {
            buffer.removeComponent(selectedAnchorNodeRef, MountedComponent.getComponentType());
        }

        playerData.setHexPreviewRefs(List.of(selectedAnchorNodeRef));
        playerData.setAnchorEntityRef(selectedAnchorNodeRef);

        World world = buffer.getExternalData().getWorld();
        UnbreakableBlockComponent.protect(world, pedestal.getLocation());

        updateState(buffer, pedestal, playerData, world, PedestalState.CRAFTING);
    }

    public static void saveHexToBook(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            CraftingDataComponent playerData) {

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

        HexBookComponent bookComp = playerData.getStoredBookComponent();
        if (bookComp == null) {
            logger.atWarning().log("pedestal: saveHexToBook — no book component");
            return;
        }

        bookComp.setHex(slotIndex, hex);

        playerData.setStoredBookComponent(bookComp);
        logger.atInfo().log("pedestal: saved hex to book — slot=%d, hex=%s", slotIndex, hex);
    }

    public static void exitCrafting(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            PedestalBlockComponent pedestal, CraftingDataComponent playerData) {

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

        // Ref<EntityStore> anchorRef = playerData.getAnchorRef();
        // if (anchorRef != null && anchorRef.isValid()) {
        // buffer.tryRemoveEntity(anchorRef, RemoveReason.REMOVE);
        // playerData.setAnchorEntityRef(null);
        // }

        playerData.setActiveSlotIndex(-1);

        if (craftingComp != null) {
            craftingComp.clearCraftingState();
        }

        playerData.setState(PedestalState.SELECTING); // go to idle to transition out of crafting
    }

    public static void handleEssencePlacement(CommandBuffer<EntityStore> buffer,
            Player player, ItemStack essenceItem, HexSlot slot,
            PedestalBlockComponent pedestalComponent, CraftingDataComponent playerData,
            Vector3i blockPos) {

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(blockPos);

        Ref<EntityStore> oldEssenceRef = playerData.getEssenceDisplayRef();
        if (oldEssenceRef != null && oldEssenceRef.isValid()) {
            buffer.removeEntity(oldEssenceRef, RemoveReason.REMOVE);
        }

        Ref<EntityStore> playerRefFlag = pedestalComponent.isPerPlayer() ? player.getReference() : null;

        Ref<EntityStore> newEssenceRef = PedestalEntity.spawnEssenceDisplay(
                buffer, pedestalComponent, playerData, anchorPos, essenceItem.getItem(),
                pedestalComponent.getReferenceHolder(), playerRefFlag);
        playerData.setEssenceDisplayRef(newEssenceRef);
        playerData.setEssence(essenceItem.getItem().getId());

        PlayerUtils.consumeOneFromHand(player, slot);
    }

    public static void handleBookPlacement(CommandBuffer<EntityStore> buffer,
            Player player, ItemStack bookItem, HexSlot slot, PedestalBlockComponent pedestalComponent,
            CraftingDataComponent playerData, Vector3i blockPos) {

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(blockPos);

        ItemStack prepared = PedestalItemUtil.ensureHexBookComponent(bookItem);
        playerData.setStoredBook(prepared);
        playerData.setBookSourceSlot(slot);

        Ref<EntityStore> oldBookDisplay = playerData.getBookDisplayRef();
        if (oldBookDisplay != null && oldBookDisplay.isValid()) {
            buffer.removeEntity(oldBookDisplay, RemoveReason.REMOVE);
        }

        Ref<EntityStore> playerRefFlag = pedestalComponent.isPerPlayer() ? player.getReference() : null;

        Ref<EntityStore> newBookDisplayRef = PedestalEntity.spawnBookDisplay(
                buffer, pedestalComponent, playerData, anchorPos, bookItem.getItem(), playerRefFlag);
        playerData.setBookDisplayRef(newBookDisplayRef);
        PlayerUtils.consumeOneFromHand(player, slot);
    }

    public static void enterSelecting(PedestalBlockComponent pedestalComponent, Player player,
            World world, CommandBuffer<EntityStore> buffer) {

        CraftingDataComponent playerData = CraftingDataUtil.getPedestalData(buffer, player.getReference());

        if (playerData == null) {
            logger.atWarning().log("pedestal: enterSelecting aborted — playerData is null for player %s",
                    player.getReference());
            return;
        }

        Integer bookSlots = playerData.getBookSlots();

        PedestalSystem.SpawnHexPreviews(buffer, player.getReference(), pedestalComponent, playerData);

        if (pedestalComponent.isPerPlayer()) {
            HexcasterComponent hexcaster = buffer.getComponent(player.getReference(),
                    HexcasterComponent.getComponentType());
            if (hexcaster != null) {
                hexcaster.setPendingPedestalRef(playerData.getAnchorRef());
                hexcaster.requestStateChange(HexState.CRAFTING);
                pedestalComponent.addDetectedPlayer(buffer, player.getReference());
            }
        }

        List<Pair<Vector3i, ObeliskBlockComponent>> obeliskPairs = ObeliskBlockUtil
                .getObelisks(pedestalComponent.getLocation(), pedestalComponent.getObeliskRange(), world);

        if (obeliskPairs.size() > pedestalComponent.getMaxObelisks()) {
            obeliskPairs = obeliskPairs.subList(0, pedestalComponent.getMaxObelisks());
        }

        List<Vector3i> obelisks = new ArrayList<>();

        for (Pair<Vector3i, ObeliskBlockComponent> obeliskPair : obeliskPairs) {
            obelisks.add(obeliskPair.getFirst());
        }

        List<Vector3i> removedObelisks = pedestalComponent.setActiveObelisks(obelisks);
        ObeliskSystem.CleanupObelisks(buffer, world, removedObelisks);

        updateState(buffer, pedestalComponent, playerData, world, PedestalState.SELECTING);
        HexcodeEvents.fire(new EnterSelectingEvent(player.getReference(), pedestalComponent.getLocation()));
    }

    public static void enterIdle(CommandBuffer<EntityStore> buffer,
            Player player, PedestalBlockComponent pedestalComponent,
            World world) {

        CraftingDataComponent playerData = CraftingDataUtil.getPedestalData(buffer, player.getReference());

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

        UnbreakableBlockComponent.unprotect(world, blockPos);

        ItemStack bookStack = playerData.getStoredBook();
        if (bookStack != null && !bookStack.isEmpty()) {
            PedestalItemUtil.returnBookToPlayer(player, bookStack, playerData.getBookSourceSlot());
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
            boolean returned = PedestalItemUtil.returnEssenceToPlayer(player, playerData.getEssence());
            if (!returned && playerData.getEssence() != null) {
                PedestalItemUtil.dropEssenceAtPosition(buffer, playerData.getEssence(), blockPos);
            }
        }
        playerData.setEssence(null);

        AnchorEntity.DespawnHexPreviews(buffer, pedestalComponent, playerData);

        Ref<EntityStore> anchorNodeRef = playerData.getAnchorNodeRef();
        if (anchorNodeRef != null && anchorNodeRef.isValid()) {
            buffer.tryRemoveEntity(anchorNodeRef, RemoveReason.REMOVE);
            playerData.setAnchorNodeRef(null);
        }

        Ref<EntityStore> anchorRef = playerData.getAnchorRef();
        if (anchorRef != null && anchorRef.isValid()) {
            buffer.tryRemoveEntity(anchorRef, RemoveReason.REMOVE);
            playerData.setAnchorEntityRef(null);
        }

        updateState(buffer, pedestalComponent, playerData, world, PedestalState.IDLE);
    }

    public static void handleReady(CommandBuffer<EntityStore> accessor, CraftingDataComponent playerData,
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
            CraftingDataComponent playerData, World world,
            PedestalState state) {

        Vector3i blockPos = pedestal.getLocation();

        String blockState = "Idle";
        switch (state) {
            case IDLE:
                blockState = "Idle";
                break;
            case READY:
                blockState = "Ready";
                break;
            case SELECTING:
                blockState = "Selecting";
                break;
            case CRAFTING:
                blockState = "Crafting";
                break;
        }

        Ref<EntityStore> bookRef = playerData.getBookDisplayRef();
        if (bookRef != null && bookRef.isValid()) {
            AnimationUtils.playAnimation(bookRef, AnimationSlot.Action, blockState, accessor);
        }

        // play essence animation
        Ref<EntityStore> essenceRef = playerData.getEssenceDisplayRef();
        if (essenceRef != null && essenceRef.isValid()) {
            AnimationUtils.playAnimation(essenceRef, AnimationSlot.Action, blockState, accessor);
        }

        boolean canSwitch = canSwitchState(accessor, pedestal, state);

        if (canSwitch) {
            PedestalBlockUtil.changeBlockState(world, blockPos, blockState);
        }
    }

    private static boolean canSwitchState(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestal, PedestalState newState) {
        if (!pedestal.isPerPlayer()) {
            return true;
        }

        Set<Ref<EntityStore>> activePlayers = pedestal.getActivePlayerRefs();
        if (activePlayers == null || activePlayers.isEmpty()) {
            return true;
        }

        boolean containsCrafting = false;
        boolean containsReady = false;
        boolean containsSelecting = false;

        for (Ref<EntityStore> playerRef : activePlayers) {
            if (playerRef == null || !playerRef.isValid())
                continue;
            CraftingDataComponent data = CraftingDataUtil.getPedestalData(buffer, playerRef);
            if (data == null)
                continue;
            switch (data.getState()) {
                case CRAFTING:
                    containsCrafting = true;
                    break;
                case READY:
                    containsReady = true;
                    break;
                case SELECTING:
                    containsSelecting = true;
                    break;
                default:
                    break;
            }
        }

        switch (newState) {
            case IDLE:
                return !containsCrafting && !containsReady;
            case SELECTING:
                return !containsCrafting && !containsSelecting;
            case CRAFTING:
                return !containsCrafting;
            case READY:
                return !containsCrafting && !containsSelecting && !containsReady;
            default:
                return true;
        }
    }
}
