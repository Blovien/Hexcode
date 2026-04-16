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
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.common.pedestal.events.PedestalSystem;
import com.riprod.hexcode.core.common.pedestal.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.session.HexcodeSessionComponent;
import com.riprod.hexcode.core.state.crafting.session.SessionUtils;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.handlers.CraftingDragHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Container.ContainerNodeHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Slot.SlotNodeHandler;
import com.riprod.hexcode.core.state.crafting.system.CraftingStateSystem;
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

        if (craftingComp == null || !craftingComp.hasActiveSession()) {
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
        HexcodeSessionComponent session = pedestal != null ? SessionUtils.resolveSession(pedestal, buffer) : null;

        if (craftingComp == null || session == null) {
            if (craftingComp != null) {
                craftingComp.clear(buffer);
            }
            GravityUtil.exitFly(buffer, ref);
            return;
        }

        SlotNodeHandler.INSTANCE.despawn(buffer, session);

        CraftingDragHandler.endDrag(buffer, craftingComp.getDraggingRef(),
                craftingComp.getHeadAnchorRef());

        if (nextState == HexState.DRAWING) {
            return;
        }

        GravityUtil.exitFly(buffer, ref);

        boolean isOwner = session.isOwner(ref);

        if (!isOwner) {
            session.removeParticipant(ref);
            if (craftingComp != null) {
                craftingComp.clear(buffer);
            }
            return;
        }

        World world = buffer.getExternalData().getWorld();
        PedestalSystem.exitCrafting(buffer, ref, pedestal, session);
        Ref<EntityStore> sessionRef = SessionUtils.getSessionRef(pedestal);
        if (sessionRef != null) {
            SessionUtils.endSession(buffer, sessionRef, world);
        }
    }

    @Override
    public void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

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

        HexcodeSessionComponent session = SessionUtils.resolveSession(pedestal, buffer);
        if (session != null && session.getPendingReenterSlot() >= 0) {
            int slot = session.getPendingReenterSlot();
            session.setPendingReenterSlot(-1);
            List<Ref<EntityStore>> previewRefs = session.getHexPreviewRefs();
            if (slot < previewRefs.size()) {
                ContainerNodeHandler.INSTANCE.enter(buffer, previewRefs.get(slot), ref);
            }
            return;
        }
        if (session != null && session.getPendingImportHex() != null) {
            int savedSlot = session.getActiveSlotIndex();
            Hex importedHex = session.getPendingImportHex();
            session.setPendingImportHex(null);

            PedestalSystem.exitCrafting(buffer, ref, pedestal, session);

            com.riprod.hexcode.core.common.hexbook.component.HexBookComponent bookComp = session
                    .getStoredBookComponent();
            if (bookComp != null) {
                List<Hex> hexes = bookComp.getHexes();
                while (hexes.size() <= savedSlot)
                    hexes.add(new Hex());
                hexes.set(savedSlot, importedHex);
                session.setStoredBookComponent(bookComp);
            }

            Player player = buffer.getComponent(ref, Player.getComponentType());
            World world = buffer.getExternalData().getWorld();
            PedestalSystem.enterSelecting(pedestal, player, world, buffer);

            session.setPendingReenterSlot(savedSlot);
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
        HexcodeSessionComponent session = SessionUtils.resolveSession(blockComp, accessor);

        if (session == null || session.getState() != PedestalState.CRAFTING) {
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

        HexcodeSessionComponent session = SessionUtils.resolveSession(pedestal, buffer);

        if (session == null) {
            return InteractionState.NotFinished;
        }

        if (session.getState() == PedestalState.CRAFTING) {
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
        HexcodeSessionComponent session = SessionUtils.resolveSession(pedestal, accessor);
        if (session == null || session.getState() != PedestalState.CRAFTING) {
            return InteractionState.Finished;
        }
        return CraftingStateSystem.exitInteraction(accessor, ref);
    }

    @Override
    public void onPlayerJoin(Holder<EntityStore> holder, HexcasterComponent comp) {
    }

    @Override
    public void onPlayerLeave(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null || !craftingComp.hasActiveSession()) return;

        Ref<EntityStore> sessionRef = craftingComp.getSessionRef();
        HexcodeSessionComponent session = buffer.getComponent(sessionRef,
                HexcodeSessionComponent.getComponentType());
        if (session == null) {
            craftingComp.clear(buffer);
            return;
        }

        CleanupUtils.safeRemoveEntity(buffer, craftingComp.getHeadAnchorRef());

        if (!session.isOwner(ref)) {
            session.removeParticipant(ref);
            craftingComp.clear(buffer);
            return;
        }

        PedestalSystem.saveHexToBook(store, ref, session);

        for (Ref<EntityStore> previewRef : session.getHexPreviewRefs()) {
            if (previewRef == null || !previewRef.isValid()) continue;
            HexComponent hexComp = buffer.getComponent(previewRef, HexComponent.getComponentType());
            if (hexComp == null) continue;
            Map<String, Ref<EntityStore>> children = hexComp.getChildGlyphRefs();
            if (children == null) continue;
            for (Ref<EntityStore> childRef : children.values()) {
                if (childRef == null || !childRef.isValid()) continue;
                GlyphComponent glyphComp = buffer.getComponent(childRef, GlyphComponent.getComponentType());
                if (glyphComp != null) {
                    CleanupUtils.safeRemoveEntities(buffer, glyphComp.getSlotEntityRefs());
                    glyphComp.getSlotEntityRefs().clear();
                }
                CleanupUtils.safeRemoveEntity(buffer, childRef);
            }
        }

        CleanupUtils.safeRemoveEntities(buffer, session.getAllRefs());

        World world = store.getExternalData().getWorld();
        SessionUtils.endSession(buffer, sessionRef, world);
    }
}
