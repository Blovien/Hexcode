package com.riprod.hexcode.core.common.imbuement;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.HexcodeEvents;
import com.riprod.hexcode.api.event.ImbuementCastEvent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.stats.HexcodeEntityStatTypes;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;

public class ImbuementExecutor {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ImbuementExecutor() {
    }

    public static boolean execute(Request request) {
        try {
            return executeInternal(request);
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] imbuement execution failed: %s", e.getMessage());
            return false;
        }
    }

    private static boolean executeInternal(Request request) {
        ImbuementCastEvent event = new ImbuementCastEvent(
                request.wielderRef, request.targetRef, request.hex);
        event.setPowerModifier(request.data.getPowerModifier());
        event.setManaCostMultiplier(request.data.getManaCostMultiplier());
        event = HexcodeEvents.dispatch(event);
        if (event.isCancelled()) return false;

        applyOverrides(request.hex, request.data);

        RootGlyph rootGlyph = new RootGlyph();
        rootGlyph.setHex(request.hex);
        rootGlyph.setNeedsInitialExecution(false);
        rootGlyph.setManaCostMultiplier(event.getManaCostMultiplier());
        rootGlyph.setVolatilityMultiplier(event.getVolatilityMultiplier());
        rootGlyph.setPowerModifier(event.getPowerModifier());

        Holder<EntityStore> holder = buildHexEntityHolder(
                request.buffer, request.wielderRef, rootGlyph);
        Ref<EntityStore> hexEntityRef = request.buffer.addEntity(holder, AddReason.SPAWN);

        ImbuedHexRoot root = new ImbuedHexRoot(request.wielderRef, hexEntityRef);
        rootGlyph.setRoot(root);

        com.hypixel.hytale.component.ComponentAccessor<ChunkStore> chunkAccessor =
                request.buffer.getExternalData().getWorld().getChunkStore().getStore();
        HexContext hexContext = new HexContext(root, request.buffer, chunkAccessor, request.hex);

        UUIDComponent targetUuid = request.buffer.getComponent(
                request.targetRef, UUIDComponent.getComponentType());
        if (targetUuid == null) {
            LOGGER.atWarning().log("[hexcode] imbuement target has no uuid, aborting");
            request.buffer.removeEntity(hexEntityRef, EntityStore.REGISTRY.newHolder(), RemoveReason.REMOVE);
            return false;
        }
        EntityVar targetVar = new EntityVar(
                EntityVar.createRef(targetUuid.getUuid(), request.targetRef));
        hexContext.setVariable("1", targetVar);

        float startingBudget = resolveVolatilityBudget(request);
        VolatilityTracker tracker = new VolatilityTracker(
                startingBudget,
                event.getVolatilityMultiplier(),
                event.getManaCostMultiplier(),
                event.getPowerModifier());
        hexContext.setVolatilityTracker(tracker);

        if (request.colors != null) {
            hexContext.setColors(request.colors.clone());
        }

        rootGlyph.setOriginContext(hexContext);
        Executor.registerActiveHex(hexContext);

        Executor.continueExecution(
                List.of(request.hex.getFirstGlyphId()), hexContext);

        if (!rootGlyph.hasDependents()) {
            Executor.unregisterActiveHex(hexContext);
            request.buffer.removeEntity(
                    hexEntityRef, EntityStore.REGISTRY.newHolder(), RemoveReason.REMOVE);
        }

        return true;
    }

    private static float resolveVolatilityBudget(Request request) {
        float override = request.data.getVolatilityBudgetOverride();
        if (override >= 0) return override;

        EntityStatMap statMap = request.buffer.getComponent(
                request.wielderRef, EntityStatMap.getComponentType());
        if (statMap != null) {
            int volIndex = HexcodeEntityStatTypes.getVolatility();
            if (volIndex != Integer.MIN_VALUE) {
                EntityStatValue volStat = statMap.get(volIndex);
                if (volStat != null) return volStat.getMax();
            }
        }
        return 0f;
    }

    private static void applyOverrides(Hex hex, ImbuementData data) {
        if (data.getVolatilityOverride() < 0 && data.getEfficiencyOverride() < 0) return;
        for (Glyph glyph : hex.getGlyphs()) {
            if (data.getVolatilityOverride() >= 0) {
                glyph.setVolatility(data.getVolatilityOverride());
            }
            if (data.getEfficiencyOverride() >= 0) {
                glyph.setEfficiency(data.getEfficiencyOverride());
            }
        }
    }

    private static Holder<EntityStore> buildHexEntityHolder(
            CommandBuffer<EntityStore> buffer,
            Ref<EntityStore> wielderRef,
            RootGlyph rootGlyph) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        TransformComponent wielderTransform = buffer.getComponent(
                wielderRef, TransformComponent.getComponentType());
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(wielderTransform.getPosition(), new Vector3f(0, 0, 0)));

        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));
        holder.addComponent(RootGlyph.getComponentType(), rootGlyph);
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        int networkId = buffer.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

        return holder;
    }

    public static class Request {
        final Ref<EntityStore> wielderRef;
        final Ref<EntityStore> targetRef;
        final Hex hex;
        final ImbuementData data;
        final CommandBuffer<EntityStore> buffer;
        @Nullable final HexColors colors;

        public Request(
                Ref<EntityStore> wielderRef,
                Ref<EntityStore> targetRef,
                Hex hex,
                ImbuementData data,
                CommandBuffer<EntityStore> buffer,
                @Nullable HexColors colors) {
            this.wielderRef = wielderRef;
            this.targetRef = targetRef;
            this.hex = hex;
            this.data = data;
            this.buffer = buffer;
            this.colors = colors;
        }
    }
}
