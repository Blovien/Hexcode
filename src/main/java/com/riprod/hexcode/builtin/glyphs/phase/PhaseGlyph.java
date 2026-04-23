package com.riprod.hexcode.builtin.glyphs.phase;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.construct.system.HexConstructSpawner;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class PhaseGlyph implements GlyphHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
public String getId() { return ID; };

public static final String ID = "Phase";

    private static final double DEFAULT_DURATION = 60.0;
    private static final double DEFAULT_INTENSITY = 5.0;
    private static final double MIN_DURATION = 10.0;
    private static final double MAX_DURATION = 200.0;
    private static final double MIN_INTENSITY = 1.0;
    private static final double MAX_INTENSITY = 15.0;

    @Override
    public boolean consumeVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;

        double intensity = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot(PhaseGlyphSlots.INTENSITY, hexContext), DEFAULT_INTENSITY),
                MIN_INTENSITY, MAX_INTENSITY);
        float intensityScale = (float) Math.max(1.0, intensity / DEFAULT_INTENSITY);

        float cost = VolatilityTracker.computeGlyphCost(glyph) * intensityScale;
        return tracker.consumeVolatility(cost);
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
        HexVar targets = glyph.readSlot(PhaseGlyphSlots.TARGET, hexContext);
        if (targets == null) {
            LOGGER.atInfo().log("phase: no targets provided");
            HexExecuter.fail(hexContext);
            return;
        }

        double duration = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot(PhaseGlyphSlots.DURATION, hexContext), DEFAULT_DURATION),
                MIN_DURATION, MAX_DURATION);
        double intensity = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot(PhaseGlyphSlots.INTENSITY, hexContext), DEFAULT_INTENSITY),
                MIN_INTENSITY, MAX_INTENSITY);

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        World world = accessor.getExternalData().getWorld();

        Vector3i pos = resolveBlockPosition(targets, hexContext);
        if (pos == null) {
            LOGGER.atInfo().log("phase: could not resolve block position");
            HexExecuter.fail(hexContext);
            return;
        }

        int blockId = world.getBlock(pos.x, pos.y, pos.z);
        if (blockId == BlockType.EMPTY_ID) {
            LOGGER.atInfo().log("phase: target block is empty");
            HexExecuter.fail(hexContext);
            return;
        }

        BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
        if (blockType == null) {
            HexExecuter.fail(hexContext);
            return;
        }

        int quality = getBlockQuality(blockType);
        if (quality > intensity) {
            LOGGER.atInfo().log("phase: block quality %d exceeds intensity %.1f", quality, intensity);
            HexExecuter.fail(hexContext);
            return;
        }

        String typeId = blockType.getId();
        int rotationIndex = world.getBlockRotationIndex(pos.x, pos.y, pos.z);

        List<PhasedBlock> phasedBlocks = new ArrayList<>();
        phasedBlocks.add(new PhasedBlock(pos, typeId, rotationIndex));

        world.setBlock(pos.x, pos.y, pos.z, "Empty");

        Vector3d blockCenter = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
        PhaseStyle.renderPhaseOut(blockCenter, hexContext.getColors(), accessor);

        glyph.writeOutput(new BlockVar(pos), hexContext);

        List<String> nextLinks = glyph.getNextLinks();

        Holder<EntityStore> holder = HexConstructSpawner.create(
                accessor, hexContext, glyph,
                "phase", (float) duration, 0f,
                null, null, nextLinks,
                blockCenter);

        holder.addComponent(PhaseComponent.getComponentType(),
                new PhaseComponent(phasedBlocks));

        Ref<EntityStore> phaseRef = accessor.addEntity(holder, AddReason.SPAWN);

        RootGlyph execComp = accessor.getComponent(
                hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
        if (execComp != null) {
            execComp.addDependent(phaseRef);
        }

        LOGGER.atInfo().log("phase: phased block for %.1f seconds", duration);
    }

    private Vector3i resolveBlockPosition(HexVar targets, HexContext hexContext) {
        BlockVar blockVar = SpellVarUtil.resolveBlockVar(targets, hexContext);
        if (blockVar != null) return blockVar.getValue();
        PositionVar posVar = SpellVarUtil.resolvePositionVar(targets, hexContext);
        if (posVar != null) {
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

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
