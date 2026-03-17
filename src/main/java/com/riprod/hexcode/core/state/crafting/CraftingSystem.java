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
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
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
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalDataComponent;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.handlers.CraftingDragHandler;
import com.riprod.hexcode.core.state.crafting.handlers.DetailsHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeRouter;
import com.riprod.hexcode.core.state.crafting.system.CraftingStateSystem;
import com.riprod.hexcode.core.state.crafting.system.SelectingStateSystem;
import com.riprod.hexcode.core.state.crafting.utils.CraftingPositionUtil;
import com.riprod.hexcode.core.state.crafting.utils.GravityUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalDataUtil;
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

        Ref<EntityStore> anchorRef = comp.consumePendingPedestalRef();

        if (anchorRef == null || !anchorRef.isValid()) {
            comp.requestStateChange(HexState.IDLE);
            return;
        }

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());

        if (craftingComp == null) {
            craftingComp = new HexcasterCraftingComponent();
            buffer.putComponent(ref, HexcasterCraftingComponent.getComponentType(), craftingComp);
        }
        craftingComp.setPedestalEntityRef(anchorRef);
        GravityUtil.enterFly(buffer, ref);
    }

    @Override
    public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
            HexState nextState) {

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(buffer, ref);
        if (craftingComp == null) {
            comp.clearCraftingState();
            return;
        }

        DetailsHandler.closeDetails(buffer, playerData.getSlotNodeRefs());

        CraftingDragHandler.endDrag(buffer, craftingComp.getDraggingRef(),
                craftingComp.getHeadAnchorRef());

        if (nextState == HexState.DRAWING) {
            return; //
        }

        // exiting crafting mode for real
        GravityUtil.exitFly(buffer, ref);

        if (!playerData.isPerPlayer()) {
            return; // will be cleaned up by pedestal exit
        }

        Ref<EntityStore> rootNodeRef = playerData.getAnchorNodeRef();
        if (rootNodeRef != null && rootNodeRef.isValid()) {
            buffer.tryRemoveEntity(rootNodeRef, RemoveReason.REMOVE);
        }
        craftingComp.clearCraftingState();
        comp.clearCraftingState();

        PedestalDataUtil.dropContents(buffer, null, playerData, null);

        List<Ref<EntityStore>> allRefs = playerData.getAllRefs();
        allRefs.forEach(r -> {
            if (r != null && r.isValid()) {
                buffer.tryRemoveEntity(r, RemoveReason.REMOVE);
            }
        });

    }

    @Override
    public void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
        if (pedestal == null) {
            return;
        }

        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(buffer, ref);

        switch (playerData.getState()) {
            case SELECTING:
                SelectingStateSystem.tickSelecting(buffer, dt, ref, pedestal, playerData);
                break;
            case CRAFTING:
                CraftingStateSystem.tickCrafting(buffer, dt, ref, pedestal);
                HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                        HexcasterCraftingComponent.getComponentType());
                if (craftingComp != null && craftingComp.getDraggingRef() == null
                        && craftingComp.getHeadAnchorRef() != null
                        && craftingComp.getHeadAnchorRef().isValid()) {

                    buffer.tryRemoveEntity(craftingComp.getHeadAnchorRef(), RemoveReason.REMOVE);
                    craftingComp.setHeadAnchorRef(buffer, null);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public InteractionState enterInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
            HexcasterComponent comp) {

        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
        if (pedestal == null) {
            return InteractionState.Failed;
        }
        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(buffer, ref);

        switch (playerData.getState()) {
            case SELECTING:
                return SelectingStateSystem.enterInteraction(ref, comp, buffer);
            case CRAFTING:
                return CraftingStateSystem.enterInteraction(ref, comp, buffer);
            default:
                break;
        }

        return InteractionState.Finished;
    }

    @Override
    public InteractionState enterAbility(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref,
            HexcasterComponent comp, InteractionType inputType) {
    
        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(accessor, ref);

        if (playerData.getState() != PedestalState.CRAFTING) {
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

        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(buffer, ref);

        if (playerData.getState() == PedestalState.CRAFTING) {
            CraftingStateSystem.tickInteraction(buffer, dt, ref, pedestal);
        }

        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState exitInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref,
            HexcasterComponent comp) {
        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(accessor, ref);

        if (playerData.getState() != PedestalState.CRAFTING) {
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

        PedestalDataComponent playerData = store.getComponent(ref, PedestalDataComponent.getComponentType());
        if (playerData == null)
            return;

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
    }
}
