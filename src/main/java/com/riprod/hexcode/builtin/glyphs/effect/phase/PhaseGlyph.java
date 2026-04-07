package com.riprod.hexcode.builtin.glyphs.effect.phase;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.common.trigger.component.TriggerComponent;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class PhaseGlyph implements GlyphHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Phase";

    private static final double DEFAULT_DURATION = 60.0;
    private static final double DEFAULT_INTENSITY = 5.0;
    private static final double MIN_DURATION = 10.0;
    private static final double MAX_DURATION = 200.0;
    private static final double MIN_INTENSITY = 1.0;
    private static final double MAX_INTENSITY = 15.0;

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        double duration = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveSlot("duration", hexContext), DEFAULT_DURATION),
                MIN_DURATION, MAX_DURATION);

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;

        float durationScale = (float) Math.pow(duration / DEFAULT_DURATION, 1.5);
        float finalCost = baseCost * castMultiplier * durationScale;

        boolean consumed = hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
        if (!consumed) {
            LOGGER.atInfo().log("phase: insufficient mana, need %.1f", finalCost);
        }
        return consumed;
    }

    @Override
    public boolean resolveVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;

        HexVar targets = glyph.resolveSlot("target", hexContext);
        double intensity = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveSlot("intensity", hexContext), DEFAULT_INTENSITY),
                MIN_INTENSITY, MAX_INTENSITY);

        int extraRolls = 0;
        if (targets instanceof BlockVar blockVar) {
            extraRolls = computeBlockVolatilityRolls(blockVar, intensity, hexContext);
        } else {
            extraRolls = Math.max(0, (int) (intensity / DEFAULT_INTENSITY) - 1);
        }

        if (!tracker.rollAndIncrement(glyph)) {
            LOGGER.atInfo().log("phase: fizzled on primary volatility roll");
            return false;
        }

        for (int i = 0; i < extraRolls; i++) {
            if (!tracker.rollAndIncrement(glyph)) {
                LOGGER.atInfo().log("phase: fizzled on extra volatility roll %d/%d", i + 1, extraRolls);
                return false;
            }
        }

        return true;
    }

    private int computeBlockVolatilityRolls(BlockVar blockVar, double intensity,
            HexContext hexContext) {
        World world = hexContext.getAccessor().getExternalData().getWorld();
        int maxQuality = 0;

        Vector3i pos = blockVar.getValue();
        if (pos != null) {
            int blockId = world.getBlock(pos.x, pos.y, pos.z);
            if (blockId != BlockType.EMPTY_ID) {
                BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
                if (blockType != null) {
                    BlockGathering gathering = blockType.getGathering();
                    if (gathering != null) {
                        var breaking = gathering.getBreaking();
                        if (breaking != null) {
                            maxQuality = breaking.getQuality();
                        }
                    }
                }
            }
        }

        float intensityFactor = (float) (intensity / DEFAULT_INTENSITY);
        return (int) ((1 + maxQuality) * intensityFactor) - 1;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.resolveSlot("target", hexContext);
        if (targets == null) {
            LOGGER.atInfo().log("phase: no targets provided");
            return;
        }

        double duration = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveSlot("duration", hexContext), DEFAULT_DURATION),
                MIN_DURATION, MAX_DURATION);
        double intensity = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveSlot("intensity", hexContext), DEFAULT_INTENSITY),
                MIN_INTENSITY, MAX_INTENSITY);

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        World world = accessor.getExternalData().getWorld();

        Vector3i pos = resolveBlockPosition(targets, hexContext);
        if (pos == null) {
            LOGGER.atInfo().log("phase: could not resolve block position");
            return;
        }

        int blockId = world.getBlock(pos.x, pos.y, pos.z);
        if (blockId == BlockType.EMPTY_ID) {
            LOGGER.atInfo().log("phase: target block is empty");
            return;
        }

        BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
        if (blockType == null) return;

        int quality = getBlockQuality(blockType);
        if (quality > intensity) {
            LOGGER.atInfo().log("phase: block quality %d exceeds intensity %.1f", quality, intensity);
            return;
        }

        String typeId = blockType.getId();
        int rotationIndex = world.getBlockRotationIndex(pos.x, pos.y, pos.z);

        List<PhasedBlock> phasedBlocks = new ArrayList<>();
        phasedBlocks.add(new PhasedBlock(pos, typeId, rotationIndex));

        world.setBlock(pos.x, pos.y, pos.z, "Empty");

        Vector3d blockCenter = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
        PhaseStyle.renderPhaseOut(blockCenter, hexContext.getColors(), accessor);

        Integer outputSlot = glyph.getSlotIndex("phased", hexContext);
        if (outputSlot != null) {
            hexContext.setVariable(outputSlot, new BlockVar(pos));
        }

        spawnPhaseEntity(glyph, hexContext, phasedBlocks, (float) duration, accessor);

        LOGGER.atInfo().log("phase: phased block for %.1f seconds", duration);
    }

    private Vector3i resolveBlockPosition(HexVar targets, HexContext hexContext) {
        if (targets instanceof BlockVar blockVar) {
            return blockVar.getValue();
        }
        if (targets instanceof PositionVar posVar) {
            Vector3d pos = posVar.getValue();
            if (pos != null) {
                return new Vector3i((int) Math.floor(pos.x), (int) Math.floor(pos.y),
                        (int) Math.floor(pos.z));
            }
        }
        return null;
    }

    private int getBlockQuality(BlockType blockType) {
        BlockGathering gathering = blockType.getGathering();
        if (gathering == null) return 0;
        var breaking = gathering.getBreaking();
        if (breaking == null) return 0;
        return breaking.getQuality();
    }

    private void spawnPhaseEntity(Glyph glyph, HexContext hexContext,
            List<PhasedBlock> phasedBlocks, float durationSeconds,
            CommandBuffer<EntityStore> accessor) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.ensureComponent(UUIDComponent.getComponentType());
        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(accessor.getExternalData().takeNextNetworkId()));
        holder.addComponent(HexSignal.getComponentType(),
                new HexSignal(hexContext.copy(), hexContext.getRoot().getRootEntityRef(),
                        glyph, glyph.getNextLinks(), null));
        holder.addComponent(PhaseComponent.getComponentType(),
                new PhaseComponent(phasedBlocks));
        holder.addComponent(TriggerComponent.getComponentType(),
                new TriggerComponent("phase", durationSeconds, null));

        accessor.addEntity(holder, AddReason.SPAWN);

        RootGlyph execComp = accessor.getComponent(
                hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
        if (execComp != null) {
            execComp.incrementExternalWaiters();
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
