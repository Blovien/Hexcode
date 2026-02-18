package com.riprod.hexcode.core.execution;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.execution.component.ExecutionComponent;
import com.riprod.hexcode.core.execution.component.HexGraph;
import com.riprod.hexcode.core.execution.component.PlayerHexRoot;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.state.HexcodeManager;

import java.util.UUID;

public class ExecutionSystem extends HexcodeManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void firstTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
    }

    @Override
    public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
    }

    @Override
    public void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
    }

    @Override
    public void onPlayerJoin(Holder<EntityStore> holder, HexcasterComponent comp) {
    }

    @Override
    public void onPlayerLeave(PlayerRef playerRef) {
    }

    @Override
    public InteractionState onPrimaryEnter(Ref<EntityStore> ref, HexcasterComponent comp,
            ComponentAccessor<EntityStore> accessor) {

        HexStaffComponent hexStaff = CasterInventory.getHexStaffComponent(accessor, ref);
        if (hexStaff == null) {
            LOGGER.atWarning().log("no hex staff component found, cannot execute spell");
            return InteractionState.Failed;
        }

        HexGraph activeHex = hexStaff.getActiveSpell();
        if (activeHex == null) {
            LOGGER.atWarning().log("no active spell on staff, nothing to execute");
            return InteractionState.Finished;
        }

        HexGraph spellClone = activeHex.clone();

        ExecutionComponent execComp = new ExecutionComponent();
        execComp.setSpellGraph(spellClone);
        execComp.setNeedsInitialExecution(true);

        Holder<EntityStore> holder = buildHexEntityHolder(accessor, ref, execComp);
        Ref<EntityStore> hexEntityRef = accessor.addEntity(holder, AddReason.SPAWN);

        PlayerHexRoot root = new PlayerHexRoot(ref, hexEntityRef);
        execComp.setRoot(root);

        return InteractionState.Finished;
    }

    @Override
    public InteractionState onPrimaryTick(Ref<EntityStore> ref, HexcasterComponent comp,
            ComponentAccessor<EntityStore> accessor) {
        return InteractionState.Finished;
    }

    @Override
    public InteractionState onPrimaryExit(Ref<EntityStore> ref, HexcasterComponent comp,
            ComponentAccessor<EntityStore> accessor) {
        return InteractionState.Finished;
    }

    private Holder<EntityStore> buildHexEntityHolder(ComponentAccessor<EntityStore> accessor,
            Ref<EntityStore> playerRef, ExecutionComponent execComp) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        TransformComponent playerTransform = accessor.getComponent(playerRef, TransformComponent.getComponentType());
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(playerTransform.getPosition(), new Vector3f(0, 0, 0)));

        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));
        holder.addComponent(ExecutionComponent.getComponentType(), execComp);
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        int networkId = accessor.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

        return holder;
    }
}
