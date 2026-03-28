package com.riprod.hexcode.builtin.glyphs.effect.growth;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import com.hypixel.hytale.builtin.adventure.farming.states.FarmingBlock;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.FarmingData;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.FarmingStageData;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.growth.style.GrowthStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class GrowthGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Growth";

    private static final String GROWTH_EFFECT_ID = "Hexcode_Growth";
    private static final double DEFAULT_AMOUNT = 5.0;
    private static final double DEFAULT_DURATION = 100.0;
    private static final double MIN_AMOUNT = 1.0;
    private static final double MAX_AMOUNT = 20.0;
    private static final int BONEMEAL_RADIUS = 2;
    private static final float BONEMEAL_CHANCE = 0.35f;

    private static final String[] VEGETATION_BLOCKS = {
            "Plant_Grass_Short", "Plant_Grass_Tall",
            "Plant_Flower_Daisy", "Plant_Flower_Poppy",
            "Plant_Fern"
    };

    private static final String[] GRASS_DIRT_PREFIXES = {
            "Soil_Grass", "Soil_Dirt"
    };

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        double amount = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("amount", hexContext), DEFAULT_AMOUNT);
        HexVar targets = glyph.resolveInput("target", hexContext);
        int targetCount = (targets != null) ? Math.max(1, targets.size()) : 1;

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;

        float amountScale = (float) (amount / DEFAULT_AMOUNT);
        float finalCost = baseCost * castMultiplier * amountScale * targetCount * 1.5f;

        boolean consumed = hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
        if (!consumed) {
            LOGGER.atInfo().log("growth: insufficient mana, need %.1f", finalCost);
        }
        return consumed;
    }

    @Override
    public boolean resolveVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;

        double amount = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("amount", hexContext), DEFAULT_AMOUNT);
        HexVar targets = glyph.resolveInput("target", hexContext);

        float amountFactor = (float) (amount / DEFAULT_AMOUNT);
        int targetCount = (targets != null) ? Math.max(1, targets.size()) : 1;

        if (!tracker.rollAndIncrement(glyph)) {
            LOGGER.atInfo().log("growth: fizzled on primary volatility roll");
            return false;
        }

        int extraRolls = (int) ((targetCount - 1) * amountFactor);
        for (int i = 0; i < extraRolls; i++) {
            if (!tracker.rollAndIncrement(glyph)) {
                LOGGER.atInfo().log("growth: fizzled on extra volatility roll %d/%d", i + 1, extraRolls);
                return false;
            }
        }

        return true;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.resolveInput("target", hexContext);
        if (targets == null || targets.size() == 0) {
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        double amount = Math.max(MIN_AMOUNT, Math.min(MAX_AMOUNT,
                SpellVarUtil.resolveNumberOrDefault(
                        glyph.resolveInput("amount", hexContext), DEFAULT_AMOUNT)));

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        if (targets instanceof EntityVar entityVar) {
            applyToEntities(entityVar, amount, glyph, hexContext, accessor);
        } else if (targets instanceof BlockVar blockVar) {
            applyToBlocks(blockVar, amount, hexContext, accessor);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }

    private void applyToEntities(EntityVar entityVar, double amount,
            Glyph glyph, HexContext hexContext, CommandBuffer<EntityStore> accessor) {
        EntityEffect growthEffect = EntityEffect.getAssetMap().getAsset(GROWTH_EFFECT_ID);
        if (growthEffect == null) {
            LOGGER.atWarning().log("growth: %s effect asset not found", GROWTH_EFFECT_ID);
            return;
        }

        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION);
        float durationSeconds = (float) duration;

        for (int i = 0; i < entityVar.size(); i++) {
            Ref<EntityStore> ref = entityVar.getRef(i, accessor);
            if (ref == null || !ref.isValid()) continue;

            EffectControllerComponent controller = accessor.getComponent(
                    ref, EffectControllerComponent.getComponentType());
            if (controller != null) {
                controller.addEffect(ref, growthEffect, durationSeconds,
                        OverlapBehavior.OVERWRITE, accessor);
            }

            TransformComponent tc = accessor.getComponent(ref, TransformComponent.getComponentType());
            if (tc != null) {
                GrowthStyle.renderEntityHit(tc.getPosition(), hexContext.getColors(), accessor);
            }

            LOGGER.atInfo().log("growth: applied regen buff for %.1fs to entity", durationSeconds);
        }
    }

    private void applyToBlocks(BlockVar blockVar, double amount,
            HexContext hexContext, CommandBuffer<EntityStore> accessor) {
        World world = accessor.getExternalData().getWorld();

        for (int i = 0; i < blockVar.size(); i++) {
            Vector3i pos = blockVar.getAt(i);
            if (pos == null) continue;

            int blockId = world.getBlock(pos.x, pos.y, pos.z);
            if (blockId == BlockType.EMPTY_ID) continue;

            BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
            if (blockType == null) continue;

            if (tryAdvanceGrowth(world, pos, blockType, amount, hexContext, accessor)) {
                continue;
            }

            if (isGrassDirtBlock(blockType)) {
                applyBonemeal(world, pos, amount, hexContext, accessor);
            }
        }
    }

    private boolean tryAdvanceGrowth(World world, Vector3i pos, BlockType blockType,
            double amount, HexContext hexContext, CommandBuffer<EntityStore> accessor) {
        FarmingData farmingConfig = blockType.getFarming();
        if (farmingConfig == null || farmingConfig.getStages() == null) return false;

        WorldChunk worldChunk = world.getChunk(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        if (worldChunk == null) return false;

        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        Ref<ChunkStore> chunkRef = world.getChunkStore().getChunkReference(
                ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        if (chunkRef == null) return false;

        BlockComponentChunk blockComponentChunk = chunkStore.getComponent(
                chunkRef, BlockComponentChunk.getComponentType());
        if (blockComponentChunk == null) return false;

        int blockIndexColumn = ChunkUtil.indexBlockInColumn(pos.x, pos.y, pos.z);
        Ref<ChunkStore> blockRef = blockComponentChunk.getEntityReference(blockIndexColumn);
        if (blockRef == null || !blockRef.isValid()) return false;

        FarmingBlock farmingBlock = chunkStore.getComponent(blockRef, FarmingBlock.getComponentType());
        if (farmingBlock == null) return false;

        String stageSetName = farmingBlock.getCurrentStageSet();
        FarmingStageData[] stages = farmingConfig.getStages().get(stageSetName);
        if (stages == null || stages.length == 0) return false;

        int currentStage = (int) farmingBlock.getGrowthProgress();
        if (currentStage >= stages.length) currentStage = stages.length - 1;

        int stagesToAdvance = Math.max(1, (int) (amount / DEFAULT_AMOUNT));
        int newStage = Math.min(currentStage + stagesToAdvance, stages.length - 1);

        if (newStage <= currentStage) return true;

        WorldTimeResource worldTimeResource = world.getEntityStore().getStore()
                .getResource(WorldTimeResource.getResourceType());
        Instant now = worldTimeResource.getGameTime();

        FarmingStageData previousStage = (currentStage >= 0 && currentStage < stages.length)
                ? stages[currentStage] : null;

        farmingBlock.setGrowthProgress(newStage);
        farmingBlock.setExecutions(0);
        farmingBlock.setGeneration(farmingBlock.getGeneration() + 1);
        farmingBlock.setLastTickGameTime(now);

        Ref<ChunkStore> sectionRef = world.getChunkStore()
                .getChunkSectionReferenceAtBlock(pos.x, pos.y, pos.z);
        if (sectionRef != null && sectionRef.isValid()) {
            BlockSection blockSection = chunkStore.getComponent(
                    sectionRef, BlockSection.getComponentType());
            if (blockSection != null) {
                blockSection.scheduleTick(ChunkUtil.indexBlock(pos.x, pos.y, pos.z), now);
            }
            stages[newStage].apply(chunkStore, sectionRef, blockRef, pos.x, pos.y, pos.z, previousStage);
        }

        worldChunk.setTicking(pos.x, pos.y, pos.z, true);

        Vector3d blockCenter = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
        GrowthStyle.renderBlockHit(blockCenter, hexContext.getColors(), accessor);

        LOGGER.atInfo().log("growth: advanced crop at %s from stage %d to %d", pos, currentStage, newStage);
        return true;
    }

    private boolean isGrassDirtBlock(BlockType blockType) {
        String id = blockType.getId();
        if (id == null) return false;
        for (String prefix : GRASS_DIRT_PREFIXES) {
            if (id.startsWith(prefix)) return true;
        }
        return false;
    }

    private void applyBonemeal(World world, Vector3i pos, double amount,
            HexContext hexContext, CommandBuffer<EntityStore> accessor) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int attempts = Math.max(3, (int) (amount * 1.5));

        for (int a = 0; a < attempts; a++) {
            int dx = rng.nextInt(-BONEMEAL_RADIUS, BONEMEAL_RADIUS + 1);
            int dz = rng.nextInt(-BONEMEAL_RADIUS, BONEMEAL_RADIUS + 1);
            int tx = pos.x + dx;
            int tz = pos.z + dz;
            int ty = pos.y;

            int belowId = world.getBlock(tx, ty, tz);
            if (belowId == BlockType.EMPTY_ID) continue;

            int aboveId = world.getBlock(tx, ty + 1, tz);
            if (aboveId != BlockType.EMPTY_ID) continue;

            if (rng.nextFloat() > BONEMEAL_CHANCE) continue;

            String vegetation = VEGETATION_BLOCKS[rng.nextInt(VEGETATION_BLOCKS.length)];
            world.setBlock(tx, ty + 1, tz, vegetation);

            Vector3d effectPos = new Vector3d(tx + 0.5, ty + 1.5, tz + 0.5);
            GrowthStyle.renderBlockHit(effectPos, hexContext.getColors(), accessor);
        }

        LOGGER.atInfo().log("growth: applied bonemeal around %s", pos);
    }
}
