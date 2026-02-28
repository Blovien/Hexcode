package com.riprod.hexcode.core.idle;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.state.HexcodeManager;

public class IdleSystem extends HexcodeManager {

        @Override
        public void firstTick(Ref<EntityStore> ref, HexcasterComponent comp,
                        Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
                comp.clearDrawingState();
                comp.clearCraftingState();

                HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                                HexcasterCraftingComponent.getComponentType());
                if (craftingComp != null) {
                        craftingComp.clearCraftingState();
                        buffer.removeComponent(ref, HexcasterCraftingComponent.getComponentType());
                }
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
