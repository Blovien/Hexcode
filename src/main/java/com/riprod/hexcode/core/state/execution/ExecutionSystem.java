package com.riprod.hexcode.core.state.execution;

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
import com.riprod.hexcode.core.common.glyphs.component.EffectComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.core.state.execution.component.PlayerHexRoot;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
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
    public InteractionState exitInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, HexcasterComponent comp) {
        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState tickInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, float dt,
            HexcasterComponent comp) {

        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState enterInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref, HexcasterComponent comp) {

        HexStaffComponent hexStaff = CasterInventory.getHexStaffComponent(accessor, ref);
        if (hexStaff == null) {
            LOGGER.atWarning().log("no hex staff component found, cannot execute spell");
            return InteractionState.Failed;
        }

        Hex activeHex = hexStaff.getActiveHex();
        if (activeHex == null) {
            LOGGER.atWarning().log("no active spell on staff, nothing to execute");
            return InteractionState.Finished;
        }

        Hex hexClone = activeHex.clone();

        RootGlyph execComp = new RootGlyph();
        execComp.setHex(hexClone);
        execComp.setNeedsInitialExecution(true);

        Holder<EntityStore> holder = buildHexEntityHolder(accessor, ref, execComp);
        Ref<EntityStore> hexEntityRef = accessor.addEntity(holder, AddReason.SPAWN);

        PlayerHexRoot root = new PlayerHexRoot(ref, hexEntityRef);
        execComp.setRoot(root);

        return InteractionState.Finished;
    }

    private Holder<EntityStore> buildHexEntityHolder(ComponentAccessor<EntityStore> accessor,
            Ref<EntityStore> playerRef, RootGlyph execComp) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        TransformComponent playerTransform = accessor.getComponent(playerRef, TransformComponent.getComponentType());
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(playerTransform.getPosition(), new Vector3f(0, 0, 0)));

        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));
        holder.addComponent(RootGlyph.getComponentType(), execComp);
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        int networkId = accessor.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

        return holder;
    }
}
