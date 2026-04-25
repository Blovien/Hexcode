package com.riprod.hexcode.core.state.execution;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
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
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.stats.HexcodeEntityStatTypes;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.common.hexcaster.utils.PlayerUtils;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.utils.HexUtils;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffAsset;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.core.common.imbuement.ImbuementData;
import com.riprod.hexcode.utils.HexStaffUtil;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexcasterExecutionComponent;
import com.riprod.hexcode.core.state.execution.component.PlayerHexRoot;
import com.riprod.hexcode.utils.CleanupUtils;
import com.riprod.hexcode.utils.HexSlot;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.api.event.HexCastEvent;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.core.state.execution.events.CastingEventData;
import com.riprod.hexcode.utils.SpellMana;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.state.HexcodeManager;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExecutionSystem extends HexcodeManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String EQUIP_CHECK_KEY = "exec_equip";
    private static final float EQUIP_CHECK_INTERVAL = 0.25f;
    private static final String HOLD_STALE_KEY = "exec_hold_stale";
    // if tickInteraction hasn't fired in this many seconds, consider LMB released
    private static final float HOLD_STALE_THRESHOLD = 0.15f;

    @Override
    public void firstTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
            HexState previousState) {
        buffer.ensureComponent(ref, HexcasterExecutionComponent.getComponentType());

    }

    @Override
    public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
            HexState nextState) {
        HexcasterExecutionComponent execComp = buffer.getComponent(ref, HexcasterExecutionComponent.getComponentType());
        if (execComp == null)
            return;
        execComp.setActiveHex(null);
        execComp.setHoldingPrimary(false);
        comp.setTickLength(HOLD_STALE_KEY, 0f);
    }

    @Override
    public void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        // release detection: if tickInteraction stops firing, clear the holding flag
        HexcasterExecutionComponent execComp = buffer.ensureAndGetComponent(ref,
                HexcasterExecutionComponent.getComponentType());
        if (execComp.isHoldingPrimary()) {
            comp.incrementTickLength(HOLD_STALE_KEY, dt);
            if (comp.getTickLength(HOLD_STALE_KEY) > HOLD_STALE_THRESHOLD) {
                execComp.setHoldingPrimary(false);
            }
        }

        comp.incrementTickLength(EQUIP_CHECK_KEY, dt);
        if (comp.getTickLength(EQUIP_CHECK_KEY) < EQUIP_CHECK_INTERVAL)
            return;
        comp.setTickLength(EQUIP_CHECK_KEY, 0f);

        if (!HexStaffUtil.hasHexcodeEquipment(store, ref)) {
            comp.requestStateChange(HexState.IDLE);
        }
    }

    @Override
    public void onPlayerJoin(Ref<EntityStore> playerRef, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
    }

    @Override
    public void onPlayerLeave(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        HexcasterExecutionComponent execComp = buffer.getComponent(ref, HexcasterExecutionComponent.getComponentType());
        execComp.cancelAll(ref);
    }

    @Override
    public InteractionState enterAbility(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
            HexcasterComponent comp, InteractionType inputType) {
        HexcasterExecutionComponent execComp = buffer.getComponent(ref,
                HexcasterExecutionComponent.getComponentType());
        if (inputType == InteractionType.Ability1) {
            int count = execComp.getActiveCount();
            execComp.cancelAll(ref);
            PlayerRef pr = buffer.getComponent(ref, PlayerRef.getComponentType());
            if (pr != null && count > 0) {
                pr.sendMessage(Message.raw("dispelled " + count + " active spell(s)"));
            }
            return InteractionState.Finished;
        }
        return InteractionState.Finished;
    }

    @Override
    public InteractionState exitInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
            HexcasterComponent comp) {
        HexcasterExecutionComponent execComp = buffer.ensureAndGetComponent(ref,
                HexcasterExecutionComponent.getComponentType());
        if (execComp == null)
            return InteractionState.Finished;

        execComp.setHoldingPrimary(false);
        comp.setTickLength(HOLD_STALE_KEY, 0f);
        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState tickInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, float dt,
            HexcasterComponent comp) {
        HexcasterExecutionComponent execComp = buffer.ensureAndGetComponent(ref,
                HexcasterExecutionComponent.getComponentType());
        if (execComp == null)
            return InteractionState.Finished;

        execComp.setHoldingPrimary(true);
        comp.setTickLength(HOLD_STALE_KEY, 0f);
        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState enterInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref,
            HexcasterComponent comp) {

        HexcasterExecutionComponent execComp = accessor.getComponent(ref,
                HexcasterExecutionComponent.getComponentType());
        if (execComp == null) {
            LOGGER.atWarning().log("no execution component found on hexcaster, cannot execute");
            return InteractionState.Finished;
        }

        Hex activeHex = execComp.getActiveHex();
        if (activeHex == null) {
            LOGGER.atWarning().log("no active spell on staff, nothing to execute");
            return InteractionState.Finished;
        }

        Hex hexClone = activeHex.clone();
        HexUtils.validate(hexClone);

        // -- collecting data phase --

        PlayerHexRoot hexRoot = new PlayerHexRoot(ref);

        int maxCharges = (int) hexRoot.resolveMaxMagicCharges(accessor);
        if (maxCharges <= 0) {
            PlayerRef pr = accessor.getComponent(ref, PlayerRef.getComponentType());
            if (pr != null) {
                pr.sendMessage(Message.raw("no spell slots available"));
            }
            return InteractionState.Finished;
        }
        int activeCount = execComp.getActiveCount();
        while (activeCount >= maxCharges) {
            execComp.evictOldest();
            activeCount--;
        }

        float volatilityMax = hexRoot.resolveVolatility(accessor);

        float startingBudget = Math.max(0, volatilityMax - execComp.getCumulativeDecay());
        HexStaffComponent staff = CasterInventory.getHexStaffComponent(accessor, ref);
        if (staff != null) {
            execComp.advanceCast(staff.getCastDecayRate(), volatilityMax);
            CasterInventory.saveHexStaffComponent(accessor, ref, staff);
        }

        // Compute base mana cost by walking the glyphs (no variables yet resolved).
        float baseMana = SpellMana.computeTotalMana(hexClone);
        float resolvedPower = hexRoot.resolveSpellPower(accessor);

        // -- Getting the event built --

        // get the colors
        HexColors colors;
        HexStaffAsset staffAsset = CasterInventory.getHexStaffAsset(
                PlayerUtils.getHandItem(accessor,
                        ref, HexSlot.MainHand));
        if (staffAsset != null && staffAsset.getColors() != null) {
            colors = staffAsset.getColors().clone();
        } else {
            colors = null;
        }

        execComp.setHoldingPrimary(true);
        comp.setTickLength(HOLD_STALE_KEY, 0f);

        VolatilityTracker tracker = new VolatilityTracker(startingBudget, 1.0f, resolvedPower);
        CastingEventData castData = new CastingEventData(hexClone, ref, baseMana, hexRoot, colors, tracker);
        execComp.registerActiveTracker(tracker);

        var hexCastEvent = new HexCastEvent(ref, castData);

        accessor.invoke(hexCastEvent);
        return InteractionState.NotFinished;
    }
}
