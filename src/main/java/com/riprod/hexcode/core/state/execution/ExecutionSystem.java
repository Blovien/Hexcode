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
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.utils.HexUtils;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.utils.HexStaffUtil;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexcasterExecutionComponent;
import com.riprod.hexcode.core.state.execution.component.PlayerHexRoot;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.utils.CleanupUtils;
import com.riprod.hexcode.api.event.HexcodeEvents;
import com.riprod.hexcode.api.event.SpellCastEvent;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.state.HexcodeManager;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
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
        HexcasterExecutionComponent execComp = buffer.ensureAndGetComponent(ref, HexcasterExecutionComponent.getComponentType());
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
    public void onPlayerJoin(Holder<EntityStore> holder, HexcasterComponent comp) {
    }

    @Override
    public void onPlayerLeave(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        for (HexContext ctx : new ArrayList<>(comp.getActiveContexts())) {
            if (ctx.getRoot() != null) {
                CleanupUtils.safeRemoveConstruct(buffer, ctx.getRoot().getRootEntityRef());
            }
        }
        comp.cancelAll();
    }

    @Override
    public InteractionState enterAbility(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
            HexcasterComponent comp, InteractionType inputType) {
        if (inputType == InteractionType.Ability1) {
            int count = comp.getActiveCount();
            comp.cancelAll();
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
        HexcasterExecutionComponent execComp = buffer.ensureAndGetComponent(ref, HexcasterExecutionComponent.getComponentType());
        if (execComp == null) return InteractionState.Finished;

        execComp.setHoldingPrimary(false);
        comp.setTickLength(HOLD_STALE_KEY, 0f);
        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState tickInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, float dt,
            HexcasterComponent comp) {
        HexcasterExecutionComponent execComp = buffer.ensureAndGetComponent(ref, HexcasterExecutionComponent.getComponentType());
        if (execComp == null) return InteractionState.Finished;

        execComp.setHoldingPrimary(true);
        comp.setTickLength(HOLD_STALE_KEY, 0f);
        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState enterInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref,
            HexcasterComponent comp) {

        HexcasterExecutionComponent execComp = accessor.getComponent(ref, HexcasterExecutionComponent.getComponentType());
        if (execComp == null) {
            LOGGER.atWarning().log("no execution component found on hexcaster, cannot execute");
            return InteractionState.Finished;
        }

        Hex activeHex = execComp.getActiveHex();
        if (activeHex == null) {
            LOGGER.atWarning().log("no active spell on staff, nothing to execute");
            return InteractionState.Finished;
        }

        int sigIndex = DefaultEntityStatTypes.getSignatureEnergy();
        if (sigIndex != Integer.MIN_VALUE) {
            EntityStatMap statMap = accessor.getComponent(ref, EntityStatMap.getComponentType());
            if (statMap != null) {
                EntityStatValue sigStat = statMap.get(sigIndex);
                if (sigStat != null) {
                    int cap = (int) sigStat.getMax();
                    if (cap > 0 && comp.getActiveCount() >= cap) {
                        PlayerRef pr = accessor.getComponent(ref, PlayerRef.getComponentType());
                        if (pr != null) {
                            pr.sendMessage(Message.raw("at maximum active spells (" + cap + ")"));
                        }
                        return InteractionState.Finished;
                    }
                }
            }
        }

        Hex hexClone = activeHex.clone();
        HexUtils.validate(hexClone);

        SpellCastEvent spellCastEvent = HexcodeEvents.dispatch(new SpellCastEvent(ref, hexClone));
        if (spellCastEvent.isCancelled())
            return InteractionState.Failed;

        RootGlyph rootGlyph = new RootGlyph();
        rootGlyph.setHex(hexClone);
        rootGlyph.setNeedsInitialExecution(true);
        rootGlyph.setManaCostMultiplier(spellCastEvent.getManaCostMultiplier());
        rootGlyph.setVolatilityMultiplier(spellCastEvent.getVolatilityMultiplier());

        Holder<EntityStore> holder = buildHexEntityHolder(accessor, ref, rootGlyph);
        Ref<EntityStore> hexEntityRef = accessor.addEntity(holder, AddReason.SPAWN);

        PlayerHexRoot root = new PlayerHexRoot(ref, hexEntityRef);
        rootGlyph.setRoot(root);

        // keep the interaction alive so tickInteraction/exitInteraction fire while LMB
        // is held
        execComp.setHoldingPrimary(true);
        comp.setTickLength(HOLD_STALE_KEY, 0f);
        return InteractionState.NotFinished;
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
