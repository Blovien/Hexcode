package com.riprod.hexcode.core.common.imbuement.block;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.HexCastEvent;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.imbuement.component.ImbuedBlockComponent;
import com.riprod.hexcode.core.common.imbuement.component.ImbuementData;
import com.riprod.hexcode.core.common.imbuement.utils.ImbuementUtils;
import com.riprod.hexcode.core.state.execution.component.BlockHexRoot;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.core.state.execution.events.CastingEventData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ImbuedBlockActivator {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public enum ActivationStatus {
        READY_FROM_SLOT,
        READY_FROM_ESSENCE,
        NO_HEX,
        NO_SLOT_NO_ESSENCE,
        EXECUTION_FAILED
    }

    private ImbuedBlockActivator() {
    }

    @Nonnull
    public static ActivationOutcome tryConsume(@Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull World world, @Nonnull Vector3i blockPos) {

        ImbuedBlockComponent comp = BlockModule.getComponent(
                ImbuedBlockComponent.getComponentType(), world,
                blockPos.x, blockPos.y, blockPos.z);
        if (comp == null) return ActivationOutcome.noHex();

        ImbuementData base = comp.read(ImbuementUtils.DEFAULT_SLOT);
        if (base == null) return ActivationOutcome.noHex();

        BlockImbuementCapacity.Capacity capacity = capacityAt(world, blockPos);
        if (capacity == null) return ActivationOutcome.noHex();

        ImbuementData mergedData;
        ActivationStatus status;

        if (comp.getSlotsReady() > 0) {
            comp.setSlotsReady(comp.getSlotsReady() - 1);
            mergedData = base.copy();
            status = ActivationStatus.READY_FROM_SLOT;
        } else {
            ImbuementData overlay = EssenceRefill.tryRefill(world, blockPos);
            if (overlay == null) return ActivationOutcome.noSlotNoEssence();
            mergedData = mergeForCast(base, overlay);
            status = ActivationStatus.READY_FROM_ESSENCE;
        }

        boolean fired = fireBlockHex(buffer, blockPos, mergedData, capacity);
        if (!fired) {
            return new ActivationOutcome(ActivationStatus.EXECUTION_FAILED, mergedData);
        }
        return new ActivationOutcome(status, mergedData);
    }

    private static boolean fireBlockHex(@Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull Vector3i blockPos, @Nonnull ImbuementData data,
            @Nonnull BlockImbuementCapacity.Capacity capacity) {
        try {
            Hex hex = ImbuementUtils.resolveHex(data);
            if (hex == null) {
                LOGGER.atWarning().log("[hexcode] block at %s has unresolvable hex", blockPos);
                return false;
            }

            BlockHexRoot hexRoot = new BlockHexRoot(blockPos, capacity);
            float volatilityMax = hexRoot.resolveVolatility(buffer);
            float volatilityMultiplier = data.getVolatilityMultiplier();

            VolatilityTracker tracker = new VolatilityTracker(volatilityMax, volatilityMultiplier, 1.0f);
            CastingEventData castData = new CastingEventData(hex, null, 0f, hexRoot, data.getColors(), tracker);
            castData.setDefaultVariable(new BlockVar(blockPos));

            buffer.invoke(new HexCastEvent(null, castData));
            return true;
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] failed to fire block hex at %s: %s", blockPos, e.getMessage());
            return false;
        }
    }

    @Nullable
    public static BlockImbuementCapacity.Capacity capacityAt(
            @Nonnull World world, @Nonnull Vector3i pos) {
        int blockId = world.getBlock(pos.x, pos.y, pos.z);
        if (blockId == 0) return null;
        BlockType type = BlockType.getAssetMap().getAsset(blockId);
        return BlockImbuementCapacity.tryFor(type);
    }

    @Nonnull
    static ImbuementData mergeForCast(
            @Nonnull ImbuementData base, @Nullable ImbuementData overlay) {
        ImbuementData out = base.copy();
        if (overlay == null) return out;
        if (overlay.getColors() != null) out.setColors(overlay.getColors());
        if (overlay.getManaOverride() >= 0f) out.setManaOverride(overlay.getManaOverride());
        if (overlay.getManaMultiplier() != 1.0f) {
            out.setManaMultiplier(out.getManaMultiplier() * overlay.getManaMultiplier());
        }
        if (overlay.getVolatilityOverride() >= 0f) {
            out.setVolatilityOverride(overlay.getVolatilityOverride());
        }
        if (overlay.getVolatilityMultiplier() != 1.0f) {
            out.setVolatilityMultiplier(out.getVolatilityMultiplier() * overlay.getVolatilityMultiplier());
        }
        if (overlay.getPowerOverride() >= 0f) out.setPowerOverride(overlay.getPowerOverride());
        if (overlay.getPowerMultiplier() != 1.0f) {
            out.setPowerMultiplier(out.getPowerMultiplier() * overlay.getPowerMultiplier());
        }
        return out;
    }

    public static final class ActivationOutcome {
        private final ActivationStatus status;
        @Nullable private final ImbuementData castData;

        ActivationOutcome(ActivationStatus status, @Nullable ImbuementData castData) {
            this.status = status;
            this.castData = castData;
        }

        @Nonnull public ActivationStatus getStatus() { return status; }
        @Nullable public ImbuementData getCastData() { return castData; }
        public boolean isReady() {
            return status == ActivationStatus.READY_FROM_SLOT
                    || status == ActivationStatus.READY_FROM_ESSENCE;
        }

        static ActivationOutcome noHex() {
            return new ActivationOutcome(ActivationStatus.NO_HEX, null);
        }
        static ActivationOutcome noSlotNoEssence() {
            return new ActivationOutcome(ActivationStatus.NO_SLOT_NO_ESSENCE, null);
        }
    }
}
