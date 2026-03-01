package com.riprod.hexcode.core.crafting.system;

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
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.crafting.events.ObeliskBlockEvent;
import com.riprod.hexcode.core.crafting.registry.PedestalBlockComponent;
import com.riprod.hexcode.core.crafting.spawners.AnchorSpawner;
import com.riprod.hexcode.core.crafting.spawners.PedestalSpawner;
import com.riprod.hexcode.core.crafting.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.crafting.utils.PedestalItemUtil;
import com.riprod.hexcode.core.crafting.utils.PedestalState;
import com.riprod.hexcode.core.crafting.utils.RadialPositionUtil;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.hexes.component.Hex;
import com.riprod.hexcode.core.hexes.component.HexComponent;
import com.riprod.hexcode.state.HexState;

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

        Vector3d anchorPos = PedestalSpawner.getAnchorPosition(pedestal.getLocation());
        List<Vector3f> offsets = RadialPositionUtil.calculateOffsets(totalSlots, PREVIEW_RADIUS, 0);
        List<Ref<EntityStore>> spawnedRefs = new ArrayList<>();

        for (int i = 0; i < totalSlots; i++) {
            Vector3f offset = offsets.get(i);

            if (i < hexes.size()) {
                Ref<EntityStore> hexRef = AnchorSpawner.spawnFilledSlot(buffer, hexes.get(i), anchorRef, anchorPos,
                        offset);
                spawnedRefs.add(hexRef);
            } else {
                Ref<EntityStore> emptyRef = AnchorSpawner.spawnEmptySlot(buffer, anchorRef, anchorPos, offset);
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

        Vector3d anchorPos = PedestalSpawner.getAnchorPosition(pedestal.getLocation());
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
        ObeliskBlockEvent.protect(pedestal.getLocation());

        updateState(buffer, pedestal, buffer.getExternalData().getWorld(), PedestalState.CRAFTING);
    }

    public static void handleEssencePlacement(CommandBuffer<EntityStore> buffer,
            Player player, ItemStack essenceItem, PedestalBlockComponent pedestalComponent, Vector3i blockPos) {

        Vector3d anchorPos = PedestalSpawner.getAnchorPosition(blockPos);

        Ref<EntityStore> oldEssenceRef = pedestalComponent.getEssenceDisplayRef();
        if (oldEssenceRef != null && oldEssenceRef.isValid()) {
            buffer.removeEntity(oldEssenceRef, RemoveReason.REMOVE);
        }

        Ref<EntityStore> newEssenceRef = PedestalSpawner.spawnEssenceDisplay(
                buffer, pedestalComponent, anchorPos, essenceItem.getItem(), pedestalComponent.getReferenceHolder());
        pedestalComponent.setEssenceDisplayRef(newEssenceRef);
        pedestalComponent.setEssenceItemId(essenceItem.getItem().getId());

        logger.atInfo().log("pedestal: spawned essence display=%s for item=%s",
                newEssenceRef, essenceItem.getItem().getId());

        consumeOneFromHand(player);
    }

    public static void handleBookPlacement(CommandBuffer<EntityStore> buffer,
            Player player, ItemStack bookItem, PedestalBlockComponent pedestalComponent, Vector3i blockPos) {

        Vector3d anchorPos = PedestalSpawner.getAnchorPosition(blockPos);

        ItemStack prepared = PedestalItemUtil.ensureHexBookComponent(bookItem);
        pedestalComponent.setStoredBook(prepared);

        Ref<EntityStore> oldBookDisplay = pedestalComponent.getBookDisplayRef();
        if (oldBookDisplay != null && oldBookDisplay.isValid()) {
            buffer.removeEntity(oldBookDisplay, RemoveReason.REMOVE);
        }

        Ref<EntityStore> newBookDisplayRef = PedestalSpawner.spawnBookDisplay(
                buffer, pedestalComponent, anchorPos, bookItem.getItem());
        pedestalComponent.setBookDisplayRef(newBookDisplayRef);

        logger.atInfo().log("pedestal: spawned book display=%s for item=%s, anchor=%s",
                newBookDisplayRef, bookItem.getItem().getId(), pedestalComponent);

        consumeOneFromHand(player);
    }

    public static void toggleActivation(CommandBuffer<EntityStore> buffer,
            Player player, PedestalBlockComponent pedestalComponent,
            World world) {

        PedestalState state = pedestalComponent.getState();
        boolean shouldDeactivate = state == PedestalState.SELECTING
                || state == PedestalState.CRAFTING
                || pedestalComponent.getEssenceItemId() == null
                || pedestalComponent.getStoredBook() == null
                || pedestalComponent.getStoredBook().isEmpty();

        if (shouldDeactivate) {
            handleDeactivation(buffer, player, pedestalComponent, world);
        } else {
            handleActivation(pedestalComponent, world, buffer);
        }
    }

    public static void handleActivation(PedestalBlockComponent pedestalComponent,
            World world, CommandBuffer<EntityStore> buffer) {

        Integer bookSlots = pedestalComponent.getBookSlots();
        logger.atInfo().log("pedestal: SpawnHexPreviews at %s, bookSlots=%s",
                pedestalComponent.getLocation(), bookSlots);

        PedestalSystem.SpawnHexPreviews(buffer, pedestalComponent);

        updateState(buffer, pedestalComponent, world, PedestalState.SELECTING);
    }

    public static void handleDeactivation(CommandBuffer<EntityStore> buffer,
            Player player, PedestalBlockComponent pedestalComponent,
            World world) {

        AnchorSpawner.DespawnHexPreviews(buffer, pedestalComponent);

        List<Ref<EntityStore>> activePlayers = pedestalComponent.getActivePlayerRefs();
        for (int i = 0; i < activePlayers.size(); i++) {
            Ref<EntityStore> activePlayerRef = activePlayers.get(i);
            if (activePlayerRef == null || !activePlayerRef.isValid()) {
                continue;
            }
            HexcasterComponent hexcaster = buffer.getComponent(activePlayerRef, HexcasterComponent.getComponentType());
            if (hexcaster != null && hexcaster.getState() == HexState.CRAFTING) {
                hexcaster.requestStateChange(HexState.IDLE);
                logger.atInfo().log("pedestal: kicking player %s from CRAFTING on deactivation", activePlayerRef);
            }
        }
        activePlayers.clear();

        Vector3i blockPos = pedestalComponent.getLocation();

        ObeliskBlockEvent.unprotect(blockPos);

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

        logger.atInfo().log("pedestal: activating at %s, essence=%s, book=%s",
                pedestal.getLocation(), pedestal.getEssenceItemId(),
                pedestal.getStoredBook() != null ? pedestal.getStoredBook().getItem().getId()
                        : "null");

        pedestal.setState(state);
    }
}
