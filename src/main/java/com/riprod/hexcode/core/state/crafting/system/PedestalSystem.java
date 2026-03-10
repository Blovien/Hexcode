package com.riprod.hexcode.core.state.crafting.system;

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
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.block.component.UnbreakableBlockComponent;
import com.riprod.hexcode.core.common.block.event.BlockBreakEvent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.PlayerUtils;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hidden.component.HiddenComponent;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.state.crafting.component.ObeliskBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalPlayerData;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.entity.AnchorEntity;
import com.riprod.hexcode.core.state.crafting.entity.PedestalEntity;
import com.riprod.hexcode.core.state.crafting.utils.ObeliskBlockUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalItemUtil;
import com.riprod.hexcode.core.state.crafting.utils.RadialPositionUtil;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.utils.HexSlot;

import io.sentry.util.Pair;

public class PedestalSystem {

    public static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    public static final float PREVIEW_RADIUS = 3.5f;
    public static final Vector3f ACTIVE_HEX_OFFSET = new Vector3f(0, 1.3f, 0);

    public static void SpawnHexPreviews(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            PedestalBlockComponent pedestal,
            PedestalPlayerData playerData) {

        Integer totalSlots = playerData.getBookSlots();
        if (totalSlots == null || totalSlots <= 0) {
            return;
        }

        List<Hex> hexes = playerData.getHexes();
        Ref<EntityStore> anchorRef = playerData.getAnchorRef();
        if (anchorRef == null || !anchorRef.isValid()) {
            return;
        }

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(pedestal.getLocation());
        List<Vector3f> offsets = RadialPositionUtil.calculateOffsets(totalSlots, PREVIEW_RADIUS, 0, new Vector3f(0, -1.5f, 0));
        List<Ref<EntityStore>> spawnedRefs = new ArrayList<>();

        Ref<EntityStore> playerRefFlag = pedestal.isPerPlayer() ? playerRef : null;

        for (int i = 0; i < totalSlots; i++) {
            Vector3f offset = offsets.get(i);

            if (i < hexes.size()) {
                Ref<EntityStore> hexRef = AnchorEntity.spawnFilledSlot(buffer, hexes.get(i), anchorRef, anchorPos,
                        offset, playerRefFlag);
                spawnedRefs.add(hexRef);
            } else {
                Ref<EntityStore> emptyRef = AnchorEntity.spawnEmptySlot(buffer, anchorRef, anchorPos, offset,
                        playerRefFlag);
                spawnedRefs.add(emptyRef);
            }
        }

        playerData.setHexPreviewRefs(spawnedRefs);
    }

    public static void enterCrafting(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            PedestalBlockComponent pedestal, PedestalPlayerData playerData, Ref<EntityStore> selectedHexRef) {

        List<Ref<EntityStore>> refs = playerData.getHexPreviewRefs();
        if (refs == null || refs.isEmpty()) {
            return;
        }

        for (Ref<EntityStore> ref : refs) {
            if (ref == null || !ref.isValid() || ref.equals(selectedHexRef)) {
                continue;
            }

            HexComponent hexComp = buffer.getComponent(ref, HexComponent.getComponentType());
            if (hexComp != null) {
                Map<String, Ref<EntityStore>> childRefs = hexComp.getChildGlyphRefs();
                if (childRefs != null) {
                    for (Ref<EntityStore> glyphRef : childRefs.values()) {
                        if (glyphRef != null && glyphRef.isValid()) {
                            buffer.removeEntity(glyphRef, RemoveReason.REMOVE);
                        }
                    }
                }
            }

            buffer.removeEntity(ref, RemoveReason.REMOVE);
        }

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(pedestal.getLocation());
        Vector3d activePos = new Vector3d(
                anchorPos.x + ACTIVE_HEX_OFFSET.x,
                anchorPos.y + ACTIVE_HEX_OFFSET.y,
                anchorPos.z + ACTIVE_HEX_OFFSET.z);
        buffer.putComponent(selectedHexRef, TransformComponent.getComponentType(),
                new TransformComponent(activePos, new Vector3f(0, 0, 0)));
        if (buffer.getComponent(selectedHexRef, MountedComponent.getComponentType()) != null) {
            buffer.removeComponent(selectedHexRef, MountedComponent.getComponentType());
        }

        playerData.setHexPreviewRefs(List.of(selectedHexRef));
        playerData.setActiveHexEntityRef(selectedHexRef);

        World world = buffer.getExternalData().getWorld();
        UnbreakableBlockComponent.protect(world, pedestal.getLocation());

        updateState(buffer, pedestal, playerData, world, PedestalState.CRAFTING);
    }

    public static void handleEssencePlacement(CommandBuffer<EntityStore> buffer,
            Player player, ItemStack essenceItem, HexSlot slot,
            PedestalBlockComponent pedestalComponent, Vector3i blockPos) {

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(blockPos);

        UUIDComponent uuidComp = buffer.getComponent(player.getReference(), UUIDComponent.getComponentType());
        PedestalPlayerData playerData = pedestalComponent.getPlayerData(uuidComp.getUuid().toString());

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
            Vector3i blockPos) {

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(blockPos);

        UUIDComponent uuidComp = buffer.getComponent(player.getReference(), UUIDComponent.getComponentType());
        PedestalPlayerData playerData = pedestalComponent.getPlayerData(uuidComp.getUuid().toString());

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

        UUIDComponent uuidComp = buffer.getComponent(player.getReference(), UUIDComponent.getComponentType());
        PedestalPlayerData playerData = pedestalComponent.getPlayerData(uuidComp.getUuid().toString());

        Integer bookSlots = playerData.getBookSlots();
        logger.atInfo().log("pedestal: SpawnHexPreviews at %s, bookSlots=%s",
                pedestalComponent.getLocation(), bookSlots);

        PedestalSystem.SpawnHexPreviews(buffer, player.getReference(), pedestalComponent, playerData);

        List<Pair<Vector3i, ObeliskBlockComponent>> obeliskPairs = ObeliskBlockUtil
                .getObelisks(pedestalComponent.getLocation(), pedestalComponent.getObeliskRange(), world);

        if (obeliskPairs.size() > pedestalComponent.getMaxObelisks()) {
            obeliskPairs = obeliskPairs.subList(0, pedestalComponent.getMaxObelisks());
        }

        List<Vector3i> obelisks = new ArrayList<>();

        for (Pair<Vector3i, ObeliskBlockComponent> obeliskPair : obeliskPairs) {
            // add obelisk
            obelisks.add(obeliskPair.getFirst());

            // TODO: Implement obelisk functionality here
        }

        List<Vector3i> removedObelisks = pedestalComponent.setActiveObelisks(obelisks);
        ObeliskSystem.CleanupObelisks(buffer, world, removedObelisks);

        updateState(buffer, pedestalComponent, playerData, world, PedestalState.SELECTING);
    }

    public static void enterIdle(CommandBuffer<EntityStore> buffer,
            Player player, PedestalBlockComponent pedestalComponent,
            World world) {

        UUIDComponent uuidComp = buffer.getComponent(player.getReference(), UUIDComponent.getComponentType());
        PedestalPlayerData playerData = pedestalComponent.getPlayerData(uuidComp.getUuid().toString());

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

        updateState(buffer, pedestalComponent, playerData, world, PedestalState.IDLE);
    }

    public static void handleReady(CommandBuffer<EntityStore> accessor, PedestalPlayerData playerData,
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
            PedestalPlayerData playerData, World world,
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

        boolean canSwitch = canSwitchState(pedestal, state);

        if (canSwitch) {
            PedestalBlockUtil.changeBlockState(world, blockPos, blockState);
        }
    }

    private static boolean canSwitchState(PedestalBlockComponent pedestal, PedestalState newState) {
        if (!pedestal.isPerPlayer()) {
            return true; // always change state if not per-player
        }

        List<PedestalState> states = pedestal.getStates();
        if (states == null) {
            return true;
        }

        boolean containsCrafting = states.contains(PedestalState.CRAFTING);
        boolean containsReady = states.contains(PedestalState.READY);
        boolean containsSelecting = states.contains(PedestalState.SELECTING);

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
