package com.riprod.hexcode.core.state.idle;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.state.HexcodeManager;
import com.riprod.hexcode.utils.CleanupUtils;

public class IdleSystem extends HexcodeManager {

        @Override
        public void firstTick(Ref<EntityStore> ref, HexcasterComponent comp,
                        Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
                        HexState previousState) {

                HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                                HexcasterCraftingComponent.getComponentType());
                if (craftingComp != null) {
                        Ref<EntityStore> headAnchor = craftingComp.getHeadAnchorRef();
                        if (headAnchor != null && headAnchor.isValid()) {
                                CleanupUtils.safeRemoveEntity(buffer, headAnchor);
                        }
                        craftingComp.clear(buffer);
                }
        }

        @Override
        public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
                        Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
                        HexState nextState) {
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
        public InteractionState enterInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, HexcasterComponent comp) {

                return InteractionState.NotFinished;
        }

        @Override
        public InteractionState tickInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, float dt,
                        HexcasterComponent comp) {

                return InteractionState.NotFinished;
        }
}
