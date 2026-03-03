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
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.block.component.UnbreakableBlockComponent;
import com.riprod.hexcode.core.common.block.event.BlockBreakEvent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.state.crafting.component.ObeliskBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.entity.AnchorEntity;
import com.riprod.hexcode.core.state.crafting.entity.PedestalEntity;
import com.riprod.hexcode.core.state.crafting.utils.ObeliskBlockUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalItemUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalState;
import com.riprod.hexcode.core.state.crafting.utils.RadialPositionUtil;
import com.riprod.hexcode.state.HexState;

import io.sentry.util.Pair;

public class PedestalSystem {

    public static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    public static final float PREVIEW_RADIUS = 3.5f;
    public static final Vector3f ACTIVE_HEX_OFFSET = new Vector3f(0, 1.3f, 0);

    public static void SpawnHexPreviews(CommandBuffer<EntityStore> buffer, PedestalBlockComponent pedestal) {
        Integer totalSlots = pedestal.getBookSlots();
        if (totalSlots == null || totalSlots <= 0) {
            return;
        }

        List<Hex> hexes = pedestal.getHexes();
        Ref<EntityStore> anchorRef = pedestal.getAnchorRef();
        if (anchorRef == null || !anchorRef.isValid()) {
            return;
        }

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(pedestal.getLocation());
        List<Vector3f> offsets = RadialPositionUtil.calculateOffsets(totalSlots, PREVIEW_RADIUS, 0);
        List<Ref<EntityStore>> spawnedRefs = new ArrayList<>();

        for (int i = 0; i < totalSlots; i++) {
            Vector3f offset = offsets.get(i);

            if (i < hexes.size()) {
                Ref<EntityStore> hexRef = AnchorEntity.spawnFilledSlot(buffer, hexes.get(i), anchorRef, anchorPos,
                        offset);
                spawnedRefs.add(hexRef);
            } else {
                Ref<EntityStore> emptyRef = AnchorEntity.spawnEmptySlot(buffer, anchorRef, anchorPos, offset);
                spawnedRefs.add(emptyRef);
            }
        }

        pedestal.setHexPreviewRefs(spawnedRefs);
    }

    public static void ActivateHexSelection(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestal, Ref<EntityStore> selectedHexRef) {

        List<Ref<EntityStore>> refs = pedestal.getHexPreviewRefs();
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

        pedestal.setHexPreviewRefs(List.of(selectedHexRef));
        pedestal.setActiveHexEntityRef(selectedHexRef);
        World world = buffer.getExternalData().getWorld();
        UnbreakableBlockComponent.protect(world, pedestal.getLocation());

        updateState(buffer, pedestal, world, PedestalState.CRAFTING);
    }

    public static void handleEssencePlacement(CommandBuffer<EntityStore> buffer,
            Player player, ItemStack essenceItem, PedestalBlockComponent pedestalComponent, Vector3i blockPos) {

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(blockPos);

        Ref<EntityStore> oldEssenceRef = pedestalComponent.getEssenceDisplayRef();
        if (oldEssenceRef != null && oldEssenceRef.isValid()) {
            buffer.removeEntity(oldEssenceRef, RemoveReason.REMOVE);
        }

        Ref<EntityStore> newEssenceRef = PedestalEntity.spawnEssenceDisplay(
                buffer, pedestalComponent, anchorPos, essenceItem.getItem(), pedestalComponent.getReferenceHolder());
        pedestalComponent.setEssenceDisplayRef(newEssenceRef);
        pedestalComponent.setEssenceItemId(essenceItem.getItem().getId());

        consumeOneFromHand(player);
    }

    public static void handleBookPlacement(CommandBuffer<EntityStore> buffer,
            Player player, ItemStack bookItem, PedestalBlockComponent pedestalComponent, Vector3i blockPos) {

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(blockPos);

        ItemStack prepared = PedestalItemUtil.ensureHexBookComponent(bookItem);
        pedestalComponent.setStoredBook(prepared);

        Ref<EntityStore> oldBookDisplay = pedestalComponent.getBookDisplayRef();
        if (oldBookDisplay != null && oldBookDisplay.isValid()) {
            buffer.removeEntity(oldBookDisplay, RemoveReason.REMOVE);
        }

        Ref<EntityStore> newBookDisplayRef = PedestalEntity.spawnBookDisplay(
                buffer, pedestalComponent, anchorPos, bookItem.getItem());
        pedestalComponent.setBookDisplayRef(newBookDisplayRef);
        consumeOneFromHand(player);
    }

    public static void handleActivation(PedestalBlockComponent pedestalComponent,
            World world, CommandBuffer<EntityStore> buffer) {

        Integer bookSlots = pedestalComponent.getBookSlots();
        logger.atInfo().log("pedestal: SpawnHexPreviews at %s, bookSlots=%s",
                pedestalComponent.getLocation(), bookSlots);

        PedestalSystem.SpawnHexPreviews(buffer, pedestalComponent);

        List<Pair<Vector3i, ObeliskBlockComponent>> obeliskPairs = ObeliskBlockUtil.getObelisks(pedestalComponent.getLocation(), pedestalComponent.getObeliskRange(), world);

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

        updateState(buffer, pedestalComponent, world, PedestalState.SELECTING);
    }

    public static void handleDeactivation(CommandBuffer<EntityStore> buffer,
            Player player, PedestalBlockComponent pedestalComponent,
            World world) {

        AnchorEntity.DespawnHexPreviews(buffer, pedestalComponent);

        Set<Ref<EntityStore>> activePlayers = pedestalComponent.getActivePlayerRefs();
        for (Ref<EntityStore> activePlayerRef : activePlayers) {
            if (activePlayerRef == null || !activePlayerRef.isValid()) {
                continue;
            }
            HexcasterComponent hexcaster = buffer.getComponent(activePlayerRef, HexcasterComponent.getComponentType());
            if (hexcaster != null && hexcaster.getState() == HexState.CRAFTING || hexcaster.getState() == HexState.DRAWING) {
                hexcaster.requestStateChange(HexState.IDLE);
                logger.atInfo().log("pedestal: kicking player from %s on deactivation", hexcaster.getState().toString());
            }
        }
        activePlayers.clear();

        Vector3i blockPos = pedestalComponent.getLocation();

        UnbreakableBlockComponent.unprotect(world, blockPos);

        ItemStack bookStack = pedestalComponent.getStoredBook();
        if (bookStack != null && !bookStack.isEmpty()) {
            PedestalItemUtil.returnBookToPlayer(player, bookStack);
            pedestalComponent.setStoredBook(ItemStack.EMPTY);
        }

        Ref<EntityStore> bookRef = pedestalComponent.getBookDisplayRef();
        if (bookRef != null && bookRef.isValid()) {
            buffer.removeEntity(bookRef, RemoveReason.REMOVE);
            pedestalComponent.setBookDisplayRef(null);
        }

        Ref<EntityStore> essenceRef = pedestalComponent.getEssenceDisplayRef();
        if (essenceRef != null && essenceRef.isValid()) {
            buffer.removeEntity(essenceRef, RemoveReason.REMOVE);
            pedestalComponent.setEssenceDisplayRef(null);
        }
        pedestalComponent.setEssenceItemId(null);

        updateState(buffer, pedestalComponent, world, PedestalState.IDLE);
    }

    public static void handleReady(CommandBuffer<EntityStore> accessor, PedestalBlockComponent pedestal, World world) {
        if (pedestal.getStoredBook() == null || pedestal.getEssenceItemId() == null || pedestal.getStoredBook().isEmpty()) {
            // updateState(accessor, pedestal, world, PedestalState.IDLE);
            return;
        }

        // Update the state
        updateState(accessor, pedestal, world, PedestalState.READY);
    }

    public static void consumeOneFromHand(Player player) {
        ItemStack current = player.getInventory().getItemInHand();
        if (current == null || current.isEmpty()) {
            return;
        }

        short activeSlot = player.getInventory().getActiveHotbarSlot();
        if (current.getQuantity() <= 1) {
            player.getInventory().getHotbar().setItemStackForSlot(activeSlot, ItemStack.EMPTY);
        } else {
            player.getInventory().getHotbar().setItemStackForSlot(activeSlot,
                    current.withQuantity(current.getQuantity() - 1));
        }
    }

    public static void updateState(CommandBuffer<EntityStore> accessor, PedestalBlockComponent pedestal, World world,
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

        PedestalBlockUtil.changeBlockState(world, blockPos, blockState);

        Ref<EntityStore> bookRef = pedestal.getBookDisplayRef();
        if (bookRef != null && bookRef.isValid()) {
            AnimationUtils.playAnimation(bookRef, AnimationSlot.Action, blockState, accessor);
        }

        // play essence animation
        Ref<EntityStore> essenceRef = pedestal.getEssenceDisplayRef();
        if (essenceRef != null && essenceRef.isValid()) {
            AnimationUtils.playAnimation(essenceRef, AnimationSlot.Action, blockState, accessor);
        }

        logger.atInfo().log("pedestal: transitioning to %s essence=%s, book=%s",
                blockState, pedestal.getEssenceItemId(),
                pedestal.getStoredBook() != null ? pedestal.getStoredBook().getItem().getId()
                        : "null");

        pedestal.setState(state);
    }
}
