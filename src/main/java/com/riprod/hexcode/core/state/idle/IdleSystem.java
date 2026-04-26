package com.riprod.hexcode.core.state.idle;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.HexCastEvent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.common.hexcaster.utils.PlayerUtils;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.utils.HexUtils;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffAsset;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexcasterIdleComponent;
import com.riprod.hexcode.core.state.execution.component.PlayerHexRoot;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.core.state.execution.events.CastingEventData;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.state.HexcodeManager;
import com.riprod.hexcode.utils.CleanupUtils;
import com.riprod.hexcode.utils.HexSlot;
import com.riprod.hexcode.utils.SpellMana;

public class IdleSystem extends HexcodeManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HOLD_STALE_KEY = "idle_hold_stale";
    // if tickInteraction hasn't fired in this many seconds, consider LMB released
    private static final float HOLD_STALE_THRESHOLD = 0.15f;

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

        buffer.ensureComponent(ref, HexcasterIdleComponent.getComponentType());
        comp.setTickLength(HOLD_STALE_KEY, 0f);
    }

    @Override
    public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
            HexState nextState) {
        HexcasterIdleComponent idleComp = buffer.getComponent(ref, HexcasterIdleComponent.getComponentType());
        if (idleComp == null) return;
        idleComp.setHoldingPrimary(false);
        comp.setTickLength(HOLD_STALE_KEY, 0f);
    }

    @Override
    public void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        // release detection: if tickInteraction stops firing, clear the holding flag
        HexcasterIdleComponent idleComp = buffer.ensureAndGetComponent(ref,
                HexcasterIdleComponent.getComponentType());
        if (idleComp.isHoldingPrimary()) {
            comp.incrementTickLength(HOLD_STALE_KEY, dt);
            if (comp.getTickLength(HOLD_STALE_KEY) > HOLD_STALE_THRESHOLD) {
                idleComp.setHoldingPrimary(false);
            }
        }
    }

    @Override
    public void onPlayerJoin(Ref<EntityStore> playerRef, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
    }

    @Override
    public void onPlayerLeave(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        HexcasterIdleComponent idleComp = buffer.getComponent(ref, HexcasterIdleComponent.getComponentType());
        if (idleComp == null) return;
        idleComp.cancelAll(ref);
    }

    @Override
    public InteractionState enterAbility(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
            HexcasterComponent comp, InteractionType inputType) {
        HexcasterIdleComponent idleComp = buffer.getComponent(ref, HexcasterIdleComponent.getComponentType());
        if (inputType == InteractionType.Ability1) {
            int count = idleComp != null ? idleComp.getActiveCount() : 0;
            if (idleComp != null) {
                idleComp.cancelAll(ref);
            }
            PlayerRef pr = buffer.getComponent(ref, PlayerRef.getComponentType());
            if (pr != null && count > 0) {
                pr.sendMessage(Message.raw("dispelled " + count + " active spell(s)"));
            }
            return InteractionState.Finished;
        }
        return InteractionState.Finished;
    }

    @Override
    public InteractionState enterInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref,
            HexcasterComponent comp) {
        HexcasterIdleComponent idleComp = accessor.getComponent(ref, HexcasterIdleComponent.getComponentType());
        if (idleComp == null) {
            LOGGER.atWarning().log("no idle component on hexcaster, cannot execute");
            return InteractionState.Finished;
        }

        Hex activeHex = idleComp.getActiveHex();
        if (activeHex == null) {
            LOGGER.atInfo().log("no active spell on staff, nothing to execute");
            return InteractionState.Finished;
        }

        Hex hexClone = activeHex.clone();
        HexUtils.validate(hexClone);

        PlayerHexRoot hexRoot = new PlayerHexRoot(ref);

        int maxCharges = (int) hexRoot.resolveMaxMagicCharges(accessor);
        if (maxCharges <= 0) {
            PlayerRef pr = accessor.getComponent(ref, PlayerRef.getComponentType());
            if (pr != null) {
                pr.sendMessage(Message.raw("no spell slots available"));
            }
            return InteractionState.Finished;
        }
        int activeCount = idleComp.getActiveCount();
        while (activeCount >= maxCharges) {
            idleComp.evictOldest();
            activeCount--;
        }

        float volatilityMax = hexRoot.resolveVolatility(accessor);
        float startingBudget = Math.max(0, volatilityMax - idleComp.getCumulativeDecay());

        HexStaffComponent staff = CasterInventory.getHexStaffComponent(accessor, ref);
        if (staff != null) {
            idleComp.advanceCast(staff.getCastDecayRate(), volatilityMax);
            CasterInventory.saveHexStaffComponent(accessor, ref, staff);
        }

        float baseMana = SpellMana.computeTotalMana(hexClone);
        float resolvedPower = hexRoot.resolveSpellPower(accessor);

        HexColors colors;
        HexStaffAsset staffAsset = CasterInventory.getHexStaffAsset(
                PlayerUtils.getHandItem(accessor, ref, HexSlot.MainHand));
        if (staffAsset != null && staffAsset.getColors() != null) {
            colors = staffAsset.getColors().clone();
        } else {
            colors = null;
        }

        idleComp.setHoldingPrimary(true);
        comp.setTickLength(HOLD_STALE_KEY, 0f);

        VolatilityTracker tracker = new VolatilityTracker(startingBudget, 1.0f, resolvedPower);
        CastingEventData castData = new CastingEventData(hexClone, ref, baseMana, hexRoot, colors, tracker);
        idleComp.registerActiveTracker(tracker);

        accessor.invoke(new HexCastEvent(ref, castData));
        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState tickInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, float dt,
            HexcasterComponent comp) {
        HexcasterIdleComponent idleComp = buffer.ensureAndGetComponent(ref,
                HexcasterIdleComponent.getComponentType());
        if (idleComp == null) return InteractionState.Finished;

        idleComp.setHoldingPrimary(true);
        comp.setTickLength(HOLD_STALE_KEY, 0f);
        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState exitInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
            HexcasterComponent comp) {
        HexcasterIdleComponent idleComp = buffer.ensureAndGetComponent(ref,
                HexcasterIdleComponent.getComponentType());
        if (idleComp == null) return InteractionState.Finished;

        idleComp.setHoldingPrimary(false);
        comp.setTickLength(HOLD_STALE_KEY, 0f);
        return InteractionState.Finished;
    }
}
