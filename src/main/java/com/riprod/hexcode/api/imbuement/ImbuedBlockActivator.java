package com.riprod.hexcode.api.imbuement;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.api.event.HexCastEvent;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.imbuement.ImbuementMetadata;
import com.riprod.hexcode.core.common.imbuement.asset.EssenceAsset;
import com.riprod.hexcode.core.common.imbuement.block.BlockImbuementCapacity;
import com.riprod.hexcode.core.common.imbuement.block.EssenceRefill;
import com.riprod.hexcode.core.common.imbuement.component.ImbuedBlockComponent;
import com.riprod.hexcode.core.common.imbuement.component.ImbuementData;
import com.riprod.hexcode.core.common.imbuement.utils.ImbuementUtils;
import com.riprod.hexcode.core.state.execution.component.BlockHexRoot;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;

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

    /**
     * Activate the imbuement on the block at {@code blockPos} from any context.
     *
     * <p>Slot/essence consumption runs synchronously and the returned outcome reflects that.
     * The HexCastEvent dispatch is queued onto the world's task queue via
     * {@link World#execute(Runnable)} and fires on the EntityStore bus next tick — this hop
     * is required because {@code HexCastEventSystem} is registered on EntityStore only.
     *
     * <p>Safe from any caller context (ChunkStore tick, EntityStore tick, command handler,
     * packet adapter, etc.). Callers on the EntityStore tick incur a 1-tick latency vs an
     * inline dispatch — acceptable for block hex effects.
     *
     * <p>Cast configuration follows the essence-overlay model: the essence asset (if used to
     * refill the slot) supplies defaults via {@code applyNonDefaultsFrom(essence.getDefaults())},
     * then the imbuement's own per-block overrides win via
     * {@code applyNonDefaultsFrom(base.getOverrides())}.
     *
     * <p>If the world is shutting down at the moment of dispatch, the queued task is rejected;
     * the slot is already consumed but the cast is logged and dropped (matches the
     * {@code ImbuedBlockPlacementHandler} precedent for deferred block-side mutations).
     *
     * <p>Not safe for every-tick polling — the essence-refill branch performs neighbour scans
     * (O(6 chunk reads + container iteration)). Call only on explicit external triggers.
     */
    @Nonnull
    public static ActivationOutcome tryConsume(@Nonnull World world, @Nonnull Vector3i blockPos) {
        ImbuedBlockComponent comp = BlockModule.getComponent(
                ImbuedBlockComponent.getComponentType(), world,
                blockPos.x, blockPos.y, blockPos.z);
        if (comp == null) return ActivationOutcome.noHex();

        ImbuementData base = comp.read(ImbuementMetadata.DEFAULT_SLOT);
        if (base == null) return ActivationOutcome.noHex();

        BlockImbuementCapacity.Capacity capacity = capacityAt(world, blockPos);
        if (capacity == null) return ActivationOutcome.noHex();

        EssenceAsset essence = null;
        ActivationStatus status;
        if (comp.getSlotsReady() > 0) {
            comp.setSlotsReady(comp.getSlotsReady() - 1);
            markDirty(world, blockPos);
            status = ActivationStatus.READY_FROM_SLOT;
        } else {
            essence = EssenceRefill.tryConsume(world, blockPos);
            if (essence == null) return ActivationOutcome.noSlotNoEssence();
            status = ActivationStatus.READY_FROM_ESSENCE;
        }

        Hex hex = ImbuementUtils.resolveHex(base);
        if (hex == null) {
            LOGGER.atWarning().log("[hexcode] block at %s has unresolvable hex", blockPos);
            return new ActivationOutcome(ActivationStatus.EXECUTION_FAILED, base);
        }

        // snapshot — block may be broken before next tick; capture immutable copy
        ImbuementData baseSnapshot = base.copy();
        BlockHexRoot hexRoot = new BlockHexRoot(blockPos, capacity);
        VolatilityTracker tracker = new VolatilityTracker(hexRoot.resolveVolatility(), 1.0f, 1.0f);
        HexContext context = new HexContext(hex, 0f, hexRoot, null, tracker);
        if (essence != null) context.applyNonDefaultsFrom(essence.getDefaults());
        context.applyNonDefaultsFrom(base.getOverrides());
        context.setDefaultVariable(new BlockVar(blockPos));

        try {
            world.execute(() -> {
                try {
                    world.getEntityStore().getStore().invoke(new HexCastEvent(context));
                } catch (Exception e) {
                    LOGGER.atSevere().log(
                            "[hexcode] deferred block hex fire failed at %s: %s",
                            blockPos, e.getMessage());
                }
            });
        } catch (RuntimeException shuttingDown) {
            LOGGER.atWarning().log(
                    "[hexcode] block hex at %s dropped: world not accepting tasks",
                    blockPos);
            return new ActivationOutcome(ActivationStatus.EXECUTION_FAILED, baseSnapshot);
        }

        return new ActivationOutcome(status, baseSnapshot);
    }

    private static void markDirty(@Nonnull World world, @Nonnull Vector3i pos) {
        BlockModule.BlockStateInfo info = BlockModule.getComponent(
                BlockModule.BlockStateInfo.getComponentType(), world, pos.x, pos.y, pos.z);
        if (info != null) info.markNeedsSaving();
    }

    @Nullable
    private static BlockImbuementCapacity.Capacity capacityAt(
            @Nonnull World world, @Nonnull Vector3i pos) {
        int blockId = world.getBlock(pos.x, pos.y, pos.z);
        if (blockId == 0) return null;
        BlockType type = BlockType.getAssetMap().getAsset(blockId);
        return BlockImbuementCapacity.tryFor(type);
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
