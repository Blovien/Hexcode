package com.riprod.hexcode.core.crafting;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.crafting.component.PedestalComponent;
import com.riprod.hexcode.core.crafting.component.PedestalState;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.state.HexcodeManager;

public class CraftingSystem extends HexcodeManager {

    @Override
    public void firstTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        Ref<EntityStore> pedestalRef = comp.getPedestalRef();
        if (pedestalRef == null || !pedestalRef.isValid()) {
            // comp.requestStateChange(HexState.IDLE);
        }
    }

    @Override
    public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        comp.clearCraftingState();
    }

    @Override
    public void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        Ref<EntityStore> pedestalRef = comp.getPedestalRef();
        if (pedestalRef == null || !pedestalRef.isValid()) {
            // comp.requestStateChange(HexState.IDLE);
            return;
        }

        PedestalComponent pedestal = buffer.getComponent(pedestalRef,
                PedestalComponent.getComponentType());
        if (pedestal == null || pedestal.getPedestalState() != PedestalState.ON) {
            // comp.requestStateChange(HexState.IDLE);
            return;
        }

        TransformComponent playerTransform = buffer.getComponent(ref,
                TransformComponent.getComponentType());
        TransformComponent pedestalTransform = buffer.getComponent(pedestalRef,
                TransformComponent.getComponentType());

        if (playerTransform == null || pedestalTransform == null) {
            // comp.requestStateChange(HexState.IDLE);
            return;
        }

        double distSq = new Vector3d(playerTransform.getPosition())
                .subtract(pedestalTransform.getPosition()).squaredLength();
        double radiusSq = pedestal.getDetectionRadius() * pedestal.getDetectionRadius();

        if (distSq > radiusSq) {
            // comp.requestStateChange(HexState.IDLE);
        }
    }

    @Override
    public void onPlayerJoin(Holder<EntityStore> holder, HexcasterComponent comp) {
    }

    @Override
    public void onPlayerLeave(PlayerRef playerRef) {
    }

    @Override
    public InteractionState exitInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
            CommandBuffer<EntityStore> buffer) {
        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState enterInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
            CommandBuffer<EntityStore> buffer) {
        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState tickInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
            CommandBuffer<EntityStore> buffer) {
        return InteractionState.NotFinished;
    }
}
