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

        HexVar targets = glyph.resolveInput("target", hexContext);
        int targetCount = (targets != null) ? Math.max(1, targets.size()) : 1;

        double duration = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION),
                MIN_DURATION, MAX_DURATION);

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;

        float blockScale = (float) (targetCount / 5.0);
        float durationScale = (float) Math.pow(duration / DEFAULT_DURATION, 1.5);
        float finalCost = baseCost * castMultiplier * blockScale * durationScale;

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

        HexVar targets = glyph.resolveInput("target", hexContext);
        double intensity = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("intensity", hexContext), DEFAULT_INTENSITY),
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

        for (int i = 0; i < blockVar.size(); i++) {
            Vector3i pos = blockVar.getAt(i);
            if (pos == null) continue;
            int blockId = world.getBlock(pos.x, pos.y, pos.z);
            if (blockId == BlockType.EMPTY_ID) continue;

            BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
            if (blockType == null) continue;

            BlockGathering gathering = blockType.getGathering();
            if (gathering == null) continue;

            var breaking = gathering.getBreaking();
            if (breaking != null) {
                maxQuality = Math.max(maxQuality, breaking.getQuality());
            }
        }

        float intensityFactor = (float) (intensity / DEFAULT_INTENSITY);
        return (int) ((1 + maxQuality) * intensityFactor) - 1;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.resolveInput("target", hexContext);
        if (targets == null || targets.size() == 0) {
            LOGGER.atInfo().log("phase: no targets provided");
            return;
        }

        double duration = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION),
                MIN_DURATION, MAX_DURATION);
        double intensity = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("intensity", hexContext), DEFAULT_INTENSITY),
                MIN_INTENSITY, MAX_INTENSITY);

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        World world = accessor.getExternalData().getWorld();

        List<PhasedBlock> phasedBlocks = collectAndPhaseBlocks(targets, intensity, world, hexContext, accessor);

        if (phasedBlocks.isEmpty()) {
            LOGGER.atInfo().log("phase: no blocks could be phased");
            return;
        }

        Integer outputSlot = glyph.resolveOutput("phased", hexContext);
        if (outputSlot != null) {
            List<Vector3i> positions = new ArrayList<>();
            for (PhasedBlock pb : phasedBlocks) {
                positions.add(pb.getPosition());
            }
            hexContext.setVariable(outputSlot, new BlockVar(positions));
        }

        spawnPhaseEntity(glyph, hexContext, phasedBlocks, (int) duration, accessor);

        LOGGER.atInfo().log("phase: phased %d blocks for %d ticks", phasedBlocks.size(), (int) duration);
    }

    private List<PhasedBlock> collectAndPhaseBlocks(HexVar targets, double intensity,
            World world, HexContext hexContext, CommandBuffer<EntityStore> accessor) {
        List<PhasedBlock> phased = new ArrayList<>();

        int count = targets.size();
        for (int i = 0; i < count; i++) {
            Vector3i pos = resolveBlockPosition(targets, i, hexContext, accessor);
            if (pos == null) continue;

            int blockId = world.getBlock(pos.x, pos.y, pos.z);
            if (blockId == BlockType.EMPTY_ID) continue;

            BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
            if (blockType == null) continue;

            int quality = getBlockQuality(blockType);
            if (quality > intensity) continue;

            String typeId = blockType.getId();
            int rotationIndex = world.getBlockRotationIndex(pos.x, pos.y, pos.z);

            phased.add(new PhasedBlock(pos, typeId, rotationIndex));

            world.setBlock(pos.x, pos.y, pos.z, "Empty");

            Vector3d blockCenter = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
            PhaseStyle.renderPhaseOut(blockCenter, hexContext.getColors(), accessor);
        }

        return phased;
    }

    private Vector3i resolveBlockPosition(HexVar targets, int index,
            HexContext hexContext, CommandBuffer<EntityStore> accessor) {
        if (targets instanceof BlockVar blockVar) {
            return blockVar.getAt(index);
        }
        if (targets instanceof PositionVar) {
            Vector3d pos = SpellVarUtil.resolvePositionAt(targets, index, accessor);
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
            List<PhasedBlock> phasedBlocks, int durationTicks,
            CommandBuffer<EntityStore> accessor) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.ensureComponent(UUIDComponent.getComponentType());
        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(accessor.getExternalData().takeNextNetworkId()));
        holder.addComponent(HexSignal.getComponentType(),
                new HexSignal(hexContext.copy(), hexContext.getRoot().getRootEntityRef(),
                        glyph, glyph.getNext(), null));
        holder.addComponent(PhaseComponent.getComponentType(),
                new PhaseComponent(phasedBlocks, durationTicks));

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
