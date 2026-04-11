package com.riprod.hexcode.core.state.crafting;

import java.util.List;
import java.util.Map;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.common.pedestal.events.PedestalSystem;
import com.riprod.hexcode.core.common.pedestal.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.CraftingData;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.handlers.CraftingDragHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeRouter;
import com.riprod.hexcode.core.state.crafting.handlers.node.Container.ContainerNodeHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Slot.SlotNodeHandler;
import com.riprod.hexcode.core.state.crafting.system.CraftingStateSystem;
import com.riprod.hexcode.core.state.crafting.utils.CraftingPositionUtil;
import com.riprod.hexcode.core.state.crafting.utils.GravityUtil;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.state.HexcodeManager;
import com.riprod.hexcode.utils.CleanupUtils;

public class CraftingSystem extends HexcodeManager {
    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    @Override
    public void firstTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
            HexState previousState) {

        if (previousState == HexState.DRAWING) {
            return;
        }
        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());

        if (craftingComp == null || craftingComp.getPedestalLocation() == null) {
            comp.requestStateChange(HexState.IDLE);
            return;
        }

        GravityUtil.enterFly(buffer, ref);
    }

    @Override
    public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
            HexState nextState) {

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
        CraftingData playerData = pedestal != null ? pedestal.getCraftingDataComponent() : null;

        if (craftingComp == null || playerData == null) {
            if (craftingComp != null) {
                craftingComp.clear(buffer);
            }
            GravityUtil.exitFly(buffer, ref);
            return;
        }

        SlotNodeHandler.INSTANCE.despawn(buffer, playerData);

        CraftingDragHandler.endDrag(buffer, craftingComp.getDraggingRef(),
                craftingComp.getHeadAnchorRef());

        if (nextState == HexState.DRAWING) {
            return;
        }

        GravityUtil.exitFly(buffer, ref);

        boolean isOwner = playerData != null && playerData.isOwner(ref);

        if (!isOwner) {
            // collaborator leaving: clean up their interaction state only
            if (pedestal != null) {
                pedestal.getActivePlayerRefs().remove(ref);
            }
            if (craftingComp != null) {
                craftingComp.clear(buffer);
            }
            return;
        }

        // owner leaving: full cleanup — delegate to canonical exit path
        PedestalSystem.exitCrafting(buffer, ref, pedestal, playerData);
        PedestalSystem.enterIdle(buffer, ref, pedestal, buffer.getExternalData().getWorld());
    }

    @Override
    public void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        // cleanup the head anchor if it exists but isn't being dragged (can happen if
        // player leaves mid-drag)
        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp != null && craftingComp.getDraggingRef() == null
                && craftingComp.getHeadAnchorRef() != null
                && craftingComp.getHeadAnchorRef().isValid()) {

            buffer.tryRemoveComponent(craftingComp.getHeadAnchorRef(), MountedComponent.getComponentType());
            buffer.tryRemoveEntity(craftingComp.getHeadAnchorRef(), RemoveReason.REMOVE);
            craftingComp.setHeadAnchorRef(buffer, null);
        }

        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
        if (pedestal == null) {
            return;
        }

        CraftingData playerData = pedestal.getCraftingDataComponent();
        if (playerData != null && playerData.getPendingReenterSlot() >= 0) {
            int slot = playerData.getPendingReenterSlot();
            playerData.setPendingReenterSlot(-1);
            List<Ref<EntityStore>> previewRefs = playerData.getHexPreviewRefs();
            if (slot < previewRefs.size()) {
                ContainerNodeHandler.INSTANCE.enter(buffer, previewRefs.get(slot), ref);
            }
            return;
        }
        if (playerData != null && playerData.getPendingImportHex() != null) {
            int savedSlot = playerData.getActiveSlotIndex();
            Hex importedHex = playerData.getPendingImportHex();
            playerData.setPendingImportHex(null);

            PedestalSystem.exitCrafting(buffer, ref, pedestal, playerData);

            HexBookComponent bookComp = playerData.getStoredBookComponent();
            if (bookComp != null) {
                List<Hex> hexes = bookComp.getHexes();
                while (hexes.size() <= savedSlot)
                    hexes.add(new Hex());
                hexes.set(savedSlot, importedHex);
                playerData.setStoredBookComponent(bookComp);
            }

            Player player = buffer.getComponent(ref, Player.getComponentType());
            World world = buffer.getExternalData().getWorld();
            PedestalSystem.enterSelecting(pedestal, player, world, buffer);

            playerData.setPendingReenterSlot(savedSlot);
            return;
        }

        Vector3i pedestalLoc = pedestal.getLocation();
        TransformComponent transform = buffer.getComponent(ref, TransformComponent.getComponentType());
        if (pedestalLoc != null && transform != null) {
            Vector3d playerPos = transform.getPosition();
            if (playerPos != null) {
                Vector3d center = new Vector3d(
                        pedestalLoc.getX() + 0.5,
                        pedestalLoc.getY() + 0.5,
                        pedestalLoc.getZ() + 0.5);
                double distSq = playerPos.distanceSquaredTo(center);
                int maxRadius = pedestal.getMaxRadius();
                double maxSq = (double) maxRadius * maxRadius;
                if (distSq > maxSq) {
                    comp.requestStateChange(HexState.IDLE);
                    return;
                }
            }
        }

        CraftingStateSystem.tickCrafting(buffer, dt, ref, pedestal);
    }

    @Override
    public InteractionState enterInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
            HexcasterComponent comp) {

        return CraftingStateSystem.enterInteraction(ref, comp, buffer);
    }

    @Override
    public InteractionState enterAbility(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref,
            HexcasterComponent comp, InteractionType inputType) {

        PedestalBlockComponent blockComp = PedestalBlockUtil.resolvePedestal(ref, accessor);
        if (blockComp == null) {
            return InteractionState.Finished;
        }
        CraftingData playerData = blockComp.getCraftingDataComponent();

        if (playerData == null || playerData.getState() != PedestalState.CRAFTING) {
            return InteractionState.Finished;
        }

        return CraftingStateSystem.enterAbility(accessor, ref, inputType);
    }

    @Override
    public InteractionState tickInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, float dt,
            HexcasterComponent comp) {

        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
        if (pedestal == null)
            return InteractionState.NotFinished;

        CraftingData playerData = pedestal.getCraftingDataComponent();

        if (playerData == null) {
            return InteractionState.NotFinished;
        }

        if (playerData.getState() == PedestalState.CRAFTING) {
            CraftingStateSystem.tickInteraction(buffer, dt, ref, pedestal);
        }

        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState exitInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref,
            HexcasterComponent comp) {
        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, accessor);
        if (pedestal == null) {
            return InteractionState.Finished;
        }
        CraftingData playerData = pedestal.getCraftingDataComponent();
        if (playerData == null || playerData.getState() != PedestalState.CRAFTING) {
            return InteractionState.Finished;
        }
        return CraftingStateSystem.exitInteraction(accessor, ref);
    }

    @Override
    public void onPlayerJoin(Holder<EntityStore> holder, HexcasterComponent comp) {
    }

    @Override
    public void onPlayerLeave(PlayerRef playerRef) {
        HexcasterCraftingComponent craftingComp = null;
        Store<EntityStore> store = null;
        Ref<EntityStore> ref = playerRef.getReference();
        boolean refValid = ref != null && ref.isValid();

        if (refValid) {
            store = ref.getStore();
            craftingComp = store.getComponent(ref, HexcasterCraftingComponent.getComponentType());
        } else {
            Holder<EntityStore> holder = playerRef.getHolder();
            if (holder != null) {
                craftingComp = holder.getComponent(HexcasterCraftingComponent.getComponentType());
            }
        }

        if (craftingComp == null)
            return;

        Ref<EntityStore> headAnchor = craftingComp.getHeadAnchorRef();
        if (headAnchor != null && headAnchor.isValid()) {
            if (store == null)
                store = headAnchor.getStore();
            store.removeEntity(headAnchor, RemoveReason.REMOVE);
        }

        if (!refValid)
            return;

        Vector3i blockPos = craftingComp.getPedestalLocation();
        if (blockPos == null)
            return;

        PedestalBlockComponent blockComp = BlockModule.getComponent(
                PedestalBlockComponent.getComponentType(),
                store.getExternalData().getWorld(),
                blockPos.getX(), blockPos.getY(), blockPos.getZ());
        CraftingData playerData = blockComp.getCraftingDataComponent();
        if (playerData == null)
            return;

        if (!playerData.isOwner(ref)) {
            blockComp.getActivePlayerRefs().remove(ref);
            return;
        }

        PedestalSystem.saveHexToBook(store, ref, playerData);

        for (Ref<EntityStore> previewRef : playerData.getHexPreviewRefs()) {
            if (previewRef == null || !previewRef.isValid())
                continue;
            HexComponent hexComp = store.getComponent(previewRef, HexComponent.getComponentType());
            if (hexComp == null)
                continue;
            Map<String, Ref<EntityStore>> children = hexComp.getChildGlyphRefs();
            if (children == null)
                continue;
            for (Ref<EntityStore> childRef : children.values()) {
                if (childRef == null || !childRef.isValid())
                    continue;
                GlyphComponent glyphComp = store.getComponent(childRef, GlyphComponent.getComponentType());
                if (glyphComp != null) {
                    for (Ref<EntityStore> slotRef : glyphComp.getSlotEntityRefs()) {
                        if (slotRef != null && slotRef.isValid()) {
                            store.removeEntity(slotRef, RemoveReason.REMOVE);
                        }
                    }
                    glyphComp.getSlotEntityRefs().clear();
                }
                store.removeEntity(childRef, RemoveReason.REMOVE);
            }
        }

        for (Ref<EntityStore> entityRef : playerData.getAllRefs()) {
            if (entityRef != null && entityRef.isValid()) {
                store.removeEntity(entityRef, RemoveReason.REMOVE);
            }
        }

        playerData.setOwnerRef(null);

        for (Ref<EntityStore> activeRef : blockComp.getActivePlayerRefs()) {
            if (activeRef == null || !activeRef.isValid() || activeRef.equals(ref))
                continue;
            HexcasterComponent hexcaster = store.getComponent(activeRef, HexcasterComponent.getComponentType());
            if (hexcaster != null) {
                hexcaster.requestStateChange(HexState.IDLE);
            }
        }
        blockComp.getActivePlayerRefs().clear();
    }
}
