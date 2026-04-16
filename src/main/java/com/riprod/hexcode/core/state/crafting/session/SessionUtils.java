package com.riprod.hexcode.core.state.crafting.session;

import java.util.Set;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.common.pedestal.events.PedestalSystem;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.entity.AnchorEntity;
import com.riprod.hexcode.core.state.crafting.handlers.node.Slot.SlotNodeHandler;
import com.riprod.hexcode.core.state.crafting.utils.PedestalItemUtil;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.utils.CleanupUtils;

import java.util.UUID;

public class SessionUtils {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public static HexcodeSessionComponent createSession(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestal, Vector3i pedestalLocation,
            Ref<EntityStore> ownerRef, boolean isOpen) {

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        holder.addComponent(UUIDComponent.getComponentType(),
                new UUIDComponent(UUID.randomUUID()));
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        HexcodeSessionComponent session = new HexcodeSessionComponent(pedestalLocation, ownerRef, isOpen);

        Ref<EntityStore> anchorRef = pedestal.getAnchorRef();
        if (anchorRef != null && anchorRef.isValid()) {
            session.setAnchorEntityRef(anchorRef);
        }

        holder.addComponent(HexcodeSessionComponent.getComponentType(), session);

        Ref<EntityStore> sessionRef = buffer.addEntity(holder, AddReason.SPAWN);

        pedestal.setSessionRef(sessionRef);

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ownerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp != null) {
            craftingComp.setSessionRef(sessionRef);
        }

        logger.atInfo().log("session created id=%s at %s, open=%s", session.getSessionId(), pedestalLocation, isOpen);
        return session;
    }

    public static void joinSession(CommandBuffer<EntityStore> buffer,
            Ref<EntityStore> sessionRef, Ref<EntityStore> participantRef) {

        HexcodeSessionComponent session = buffer.getComponent(sessionRef,
                HexcodeSessionComponent.getComponentType());
        if (session == null) return;

        session.addParticipant(participantRef);

        HexcasterCraftingComponent craftingComp = buffer.getComponent(participantRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp != null) {
            craftingComp.setSessionRef(sessionRef);
        }

        logger.atInfo().log("player joined session id=%s", session.getSessionId());
    }

    public static void leaveSession(CommandBuffer<EntityStore> buffer, Ref<EntityStore> participantRef,
            World world) {

        HexcasterCraftingComponent craftingComp = buffer.getComponent(participantRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null || !craftingComp.hasActiveSession()) return;

        Ref<EntityStore> sessionRef = craftingComp.getSessionRef();
        HexcodeSessionComponent session = buffer.getComponent(sessionRef,
                HexcodeSessionComponent.getComponentType());
        if (session == null) {
            craftingComp.clear(buffer);
            return;
        }

        session.removeParticipant(participantRef);
        craftingComp.clear(buffer);

        boolean isOwner = session.isOwner(participantRef);
        boolean noParticipants = session.getParticipantRefs().isEmpty();

        if (isOwner || noParticipants) {
            endSession(buffer, sessionRef, world);
        }
    }

    public static void endSession(CommandBuffer<EntityStore> buffer, Ref<EntityStore> sessionRef,
            World world) {

        if (sessionRef == null || !sessionRef.isValid()) return;

        HexcodeSessionComponent session = buffer.getComponent(sessionRef,
                HexcodeSessionComponent.getComponentType());
        if (session == null) return;

        logger.atInfo().log("ending session id=%s at %s", session.getSessionId(), session.getPedestalLocation());

        Set<Ref<EntityStore>> participants = session.getParticipantRefs();
        for (Ref<EntityStore> pRef : participants) {
            if (pRef == null || !pRef.isValid()) continue;
            HexcasterComponent hexcaster = buffer.getComponent(pRef, HexcasterComponent.getComponentType());
            if (hexcaster != null
                    && (hexcaster.getState() == HexState.CRAFTING || hexcaster.getState() == HexState.DRAWING)) {
                hexcaster.requestStateChange(HexState.IDLE);
            }
            HexcasterCraftingComponent craftingComp = buffer.getComponent(pRef,
                    HexcasterCraftingComponent.getComponentType());
            if (craftingComp != null) {
                craftingComp.clear(buffer);
            }
        }
        participants.clear();

        SlotNodeHandler.INSTANCE.despawn(buffer, session);

        AnchorEntity.DespawnHexPreviews(buffer, session);

        Ref<EntityStore> anchorNodeRef = session.getAnchorNodeRef();
        if (anchorNodeRef != null && anchorNodeRef.isValid()) {
            buffer.tryRemoveEntity(anchorNodeRef, RemoveReason.REMOVE);
            session.setAnchorNodeRef(null);
        }

        Ref<EntityStore> ownerRef = session.getOwnerRef();
        ItemStack bookStack = session.getStoredBook();
        if (bookStack != null && !bookStack.isEmpty()) {
            if (ownerRef != null && ownerRef.isValid()) {
                PedestalItemUtil.returnBookToPlayer(buffer, ownerRef, bookStack, session.getBookSourceSlot());
            } else {
                PedestalItemUtil.dropBookAtPosition(buffer, bookStack, session.getPedestalLocation());
            }
            session.setStoredBook(ItemStack.EMPTY);

            if (ownerRef != null && ownerRef.isValid()) {
                HexcasterCraftingComponent ownerCrafting = buffer.getComponent(ownerRef,
                        HexcasterCraftingComponent.getComponentType());
                if (ownerCrafting != null) {
                    ownerCrafting.setPersistedBookSnapshot(null);
                    ownerCrafting.setPersistedBookSourceSlot(null);
                }
            }
        }

        Ref<EntityStore> bookRef = session.getBookDisplayRef();
        if (bookRef != null && bookRef.isValid()) {
            buffer.removeEntity(bookRef, RemoveReason.REMOVE);
            session.setBookDisplayRef(null);
        }

        Ref<EntityStore> essenceRef = session.getEssenceDisplayRef();
        if (essenceRef != null && essenceRef.isValid()) {
            buffer.removeEntity(essenceRef, RemoveReason.REMOVE);
            session.setEssenceDisplayRef(null);
        }

        Vector3i pedestalLoc = session.getPedestalLocation();
        PedestalBlockComponent pedestal = BlockModule.getComponent(
                PedestalBlockComponent.getComponentType(), world,
                pedestalLoc.x, pedestalLoc.y, pedestalLoc.z);
        if (pedestal != null) {
            PedestalSystem.updateState(buffer, pedestal, session, world, PedestalState.IDLE);
            pedestal.setSessionRef(null);
            pedestal.setBookAssetId(null);
        }

        session.setEssence(null);
        session.setOwnerRef(null);
        session.setActiveSlotIndex(-1);
        session.setAnchorEntityRef(null);

        if (sessionRef.isValid()) {
            buffer.tryRemoveEntity(sessionRef, RemoveReason.REMOVE);
        }
    }

    @Nullable
    public static HexcodeSessionComponent resolveSession(PedestalBlockComponent pedestal,
            ComponentAccessor<EntityStore> accessor) {
        Ref<EntityStore> sessionRef = pedestal.getSessionRef();
        if (sessionRef == null || !sessionRef.isValid()) return null;
        return accessor.getComponent(sessionRef, HexcodeSessionComponent.getComponentType());
    }

    @Nullable
    public static Ref<EntityStore> getSessionRef(PedestalBlockComponent pedestal) {
        Ref<EntityStore> sessionRef = pedestal.getSessionRef();
        if (sessionRef == null || !sessionRef.isValid()) return null;
        return sessionRef;
    }

    @Nullable
    public static HexcodeSessionComponent resolveSessionByPlayer(Ref<EntityStore> playerRef,
            ComponentAccessor<EntityStore> accessor) {
        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null || !craftingComp.hasActiveSession()) return null;
        return accessor.getComponent(craftingComp.getSessionRef(), HexcodeSessionComponent.getComponentType());
    }

    @Nullable
    public static Ref<EntityStore> getSessionRefByPlayer(Ref<EntityStore> playerRef,
            ComponentAccessor<EntityStore> accessor) {
        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null || !craftingComp.hasActiveSession()) return null;
        return craftingComp.getSessionRef();
    }
}
