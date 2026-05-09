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
import com.riprod.hexcode.core.common.hexes.registry.HexStyleAsset;
import com.riprod.hexcode.core.common.imbuement.ImbuementMetadata;
import com.riprod.hexcode.core.common.imbuement.asset.EssenceAsset;
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

    // call only on explicit external trigger (player interaction, glyph effect,
    // redstone edge). not safe for every-tick polling — neighbor scan during the
    // essence-refill branch is O(6 chunk reads + container iteration).
    @Nonnull
    public static ActivationOutcome tryConsume(@Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull World world, @Nonnull Vector3i blockPos) {

        ImbuedBlockComponent comp = BlockModule.getComponent(
                ImbuedBlockComponent.getComponentType(), world,
                blockPos.x, blockPos.y, blockPos.z);
        if (comp == null) return ActivationOutcome.noHex();

        ImbuementData base = comp.read(ImbuementMetadata.DEFAULT_SLOT);
        if (base == null) return ActivationOutcome.noHex();

        BlockImbuementCapacity.Capacity capacity = capacityAt(world, blockPos);
        if (capacity == null) return ActivationOutcome.noHex();

        HexStyleAsset style;
        float volatilityMultiplier;
        ActivationStatus status;

        if (comp.getSlotsReady() > 0) {
            comp.setSlotsReady(comp.getSlotsReady() - 1);
            markDirty(world, blockPos);
            style = styleFromImbuement(base);
            volatilityMultiplier = base.getVolatilityMultiplier();
            status = ActivationStatus.READY_FROM_SLOT;
        } else {
            EssenceAsset essence = EssenceRefill.tryConsume(world, blockPos);
            if (essence == null) return ActivationOutcome.noSlotNoEssence();
            style = essence.getStyle();
            volatilityMultiplier = essence.getVolatilityMultiplier();
            status = ActivationStatus.READY_FROM_ESSENCE;
        }

        boolean fired = fireBlockHex(buffer, blockPos, base, style, volatilityMultiplier, capacity);
        if (!fired) {
            return new ActivationOutcome(ActivationStatus.EXECUTION_FAILED, base);
        }
        return new ActivationOutcome(status, base);
    }

    private static void markDirty(@Nonnull World world, @Nonnull Vector3i pos) {
        BlockModule.BlockStateInfo info = BlockModule.getComponent(
                BlockModule.BlockStateInfo.getComponentType(), world, pos.x, pos.y, pos.z);
        if (info != null) info.markNeedsSaving();
    }

    private static boolean fireBlockHex(@Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull Vector3i blockPos, @Nonnull ImbuementData data,
            @Nullable HexStyleAsset style, float volatilityMultiplier,
            @Nonnull BlockImbuementCapacity.Capacity capacity) {
        try {
            Hex hex = ImbuementUtils.resolveHex(data);
            if (hex == null) {
                LOGGER.atWarning().log("[hexcode] block at %s has unresolvable hex", blockPos);
                return false;
            }

            BlockHexRoot hexRoot = new BlockHexRoot(blockPos, capacity);
            float volatilityMax = hexRoot.resolveVolatility(buffer);
            VolatilityTracker tracker = new VolatilityTracker(volatilityMax, volatilityMultiplier, 1.0f);
            CastingEventData castData = new CastingEventData(hex, null, 0f, hexRoot, style, tracker);
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

    // for the slot-ready path the base imbuement only carries colors, not a
    // HexStyleAsset reference — wrap them as a synthetic style to flow through
    // the same setStyle channel essences use. matches the colors-ctor on
    // CastingEventData (styleFromColors) and avoids a second style channel.
    @Nullable
    private static HexStyleAsset styleFromImbuement(@Nonnull ImbuementData data) {
        if (data.getColors() == null) return null;
        HexStyleAsset s = HexStyleAsset.empty();
        if (data.getColors().getPrimaryColor() != null) {
            s.setPrimaryColor(data.getColors().getPrimaryColor().clone());
        }
        if (data.getColors().getSecondaryColor() != null) {
            s.setSecondaryColor(data.getColors().getSecondaryColor().clone());
        }
        s.setAlpha(data.getColors().getPrimaryAlpha());
        return s;
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
