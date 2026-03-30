package com.riprod.hexcode.core.state.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
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
import com.riprod.hexcode.core.state.crafting.handlers.DetailsHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeRouter;
import com.riprod.hexcode.core.state.crafting.handlers.node.Slot.SlotNodeHandler;
import com.riprod.hexcode.core.state.crafting.system.CraftingStateSystem;
import com.riprod.hexcode.core.state.crafting.utils.CraftingPositionUtil;
import com.riprod.hexcode.core.state.crafting.utils.GravityUtil;
import com.riprod.hexcode.core.state.crafting.utils.CraftingDataUtil;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.state.HexcodeManager;
import com.riprod.hexcode.utils.CleanupUtils;

public class CraftingSystem extends HexcodeManager {

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
                craftingComp.clearCraftingState();
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
                craftingComp.clearCraftingState();
            }
            return;
        }

        // owner leaving: full cleanup

        Ref<EntityStore> rootNodeRef = playerData.getAnchorNodeRef();
        if (rootNodeRef != null && rootNodeRef.isValid()) {
            buffer.tryRemoveComponent(rootNodeRef, MountedComponent.getComponentType());
            buffer.tryRemoveEntity(rootNodeRef, RemoveReason.REMOVE);
        }

        if (craftingComp != null) {
            craftingComp.clearCraftingState();
        }

        Vector3i dropPos = playerData.getPedestalLocation();
        CraftingDataUtil.dropContents(buffer, pedestal, playerData, dropPos);

        List<Ref<EntityStore>> allRefs = playerData.getAllRefs();
        allRefs.forEach(r -> {
            if (r != null && r.isValid()) {
                buffer.tryRemoveComponent(r, MountedComponent.getComponentType());
                buffer.tryRemoveEntity(r, RemoveReason.REMOVE);
            }
        });

        // kick collaborators
        if (pedestal != null) {
            for (Ref<EntityStore> activeRef : pedestal.getActivePlayerRefs()) {
                if (activeRef == null || !activeRef.isValid() || activeRef.equals(ref))
                    continue;
                HexcasterComponent hexcaster = buffer.getComponent(activeRef, HexcasterComponent.getComponentType());
                if (hexcaster != null) {
                    hexcaster.requestStateChange(HexState.IDLE);
                }
            }
            pedestal.getActivePlayerRefs().clear();
        }

        playerData.setOwnerRef(null);

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
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid())
            return;

        Store<EntityStore> store = ref.getStore();

        HexcasterCraftingComponent craftingComp = store.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp != null) {
            Ref<EntityStore> headAnchor = craftingComp.getHeadAnchorRef();
            if (headAnchor != null && headAnchor.isValid()) {
                store.removeEntity(headAnchor, RemoveReason.REMOVE);
            }
        }

        // get pedestal data from the anchor entity
        if (craftingComp == null)
            return;

        Vector3i blockPos = craftingComp.getPedestalLocation();

        if (blockPos == null) {
            return;
        }

        PedestalBlockComponent blockComp = BlockModule.getComponent(
                PedestalBlockComponent.getComponentType(),
                store.getExternalData().getWorld(),
                blockPos.getX(), blockPos.getY(), blockPos.getZ());
        CraftingData playerData = blockComp.getCraftingDataComponent();
        if (playerData == null)
            return;

        if (!playerData.isOwner(ref)) {
            return;
        }

        PedestalSystem.saveHexToBook(store, ref, playerData);

        // owner leaving: clean up pedestal entities
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
                GlyphComponent glyph = store.getComponent(childRef, GlyphComponent.getComponentType());
                if (glyph != null && glyph.getNodeRef() != null && glyph.getNodeRef().isValid()) {
                    store.removeEntity(glyph.getNodeRef(), RemoveReason.REMOVE);
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
    }
}
