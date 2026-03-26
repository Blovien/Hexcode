package com.riprod.hexcode.builtin.glyphs.effect.fortify;

import java.time.Instant;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.blockhealth.BlockHealthChunk;
import com.hypixel.hytale.server.core.modules.blockhealth.BlockHealthModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.fortify.component.FortifyComponent;
import com.riprod.hexcode.builtin.glyphs.effect.fortify.style.FortifyStyle;
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

public class FortifyGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Fortify";

    private static final String FORTIFY_EFFECT_ID = "Hexcode_Fortify";
    private static final double DEFAULT_AMOUNT = 5.0;
    private static final double DEFAULT_DURATION = 100.0;
    private static final double MIN_AMOUNT = 1.0;
    private static final double MAX_AMOUNT = 20.0;
    private static final float REDUCTION_SCALE = 0.5f;
    private static final float BLOCK_HEAL_SCALE = 0.05f;

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        double amount = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("amount", hexContext), DEFAULT_AMOUNT);
        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION);
        HexVar targets = glyph.resolveInput("target", hexContext);
        int targetCount = (targets != null) ? Math.max(1, targets.size()) : 1;

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;

        float amountScale = (float) (amount / DEFAULT_AMOUNT);
        float durationScale = (float) (duration / DEFAULT_DURATION);
        float finalCost = baseCost * castMultiplier * amountScale * durationScale * targetCount;

        boolean consumed = hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
        if (!consumed) {
            LOGGER.atInfo().log("fortify: insufficient mana, need %.1f", finalCost);
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
        int extraRolls = 0;

        if (targets instanceof BlockVar blockVar) {
            extraRolls = computeBlockVolatilityRolls(blockVar, amountFactor, hexContext);
        } else {
            extraRolls = Math.max(0, (int) (amountFactor - 1));
        }

        if (!tracker.rollAndIncrement(glyph)) {
            LOGGER.atInfo().log("fortify: fizzled on primary volatility roll");
            return false;
        }

        for (int i = 0; i < extraRolls; i++) {
            if (!tracker.rollAndIncrement(glyph)) {
                LOGGER.atInfo().log("fortify: fizzled on extra volatility roll %d/%d", i + 1, extraRolls);
                return false;
            }
        }

        return true;
    }

    private int computeBlockVolatilityRolls(BlockVar blockVar, float amountFactor,
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

        return (int) ((1 + maxQuality) * amountFactor) - 1;
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
        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION);
        float damageReduction = (float) (amount * REDUCTION_SCALE);
        float durationSeconds = (float) (duration / 20.0);

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        if (targets instanceof EntityVar entityVar) {
            applyToEntities(entityVar, damageReduction, durationSeconds, hexContext, accessor);
        } else if (targets instanceof BlockVar blockVar) {
            applyToBlocks(blockVar, amount, hexContext, accessor);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }

    private void applyToEntities(EntityVar entityVar, float damageReduction,
            float durationSeconds, HexContext hexContext, CommandBuffer<EntityStore> accessor) {
        EntityEffect fortifyEffect = EntityEffect.getAssetMap().getAsset(FORTIFY_EFFECT_ID);
        if (fortifyEffect == null) {
            LOGGER.atWarning().log("fortify: %s effect asset not found", FORTIFY_EFFECT_ID);
            return;
        }

        for (int i = 0; i < entityVar.size(); i++) {
            Ref<EntityStore> ref = entityVar.getRef(i, accessor);
            if (ref == null || !ref.isValid()) continue;

            FortifyComponent existing = accessor.getComponent(ref, FortifyComponent.getComponentType());
            if (existing != null) {
                existing.setDamageReduction(damageReduction);
                existing.setRemainingDuration(durationSeconds);
            } else {
                accessor.addComponent(ref, FortifyComponent.getComponentType(),
                        new FortifyComponent(damageReduction, durationSeconds));
            }

            EffectControllerComponent controller = accessor.getComponent(
                    ref, EffectControllerComponent.getComponentType());
            if (controller != null) {
                controller.addEffect(ref, fortifyEffect, durationSeconds,
                        OverlapBehavior.OVERWRITE, accessor);
            }

            TransformComponent tc = accessor.getComponent(ref, TransformComponent.getComponentType());
            if (tc != null) {
                FortifyStyle.renderEntityHit(tc.getPosition(), hexContext.getColors(), accessor);
            }

            LOGGER.atInfo().log("fortify: applied %.2f flat reduction for %.1fs to entity",
                    damageReduction, durationSeconds);
        }
    }

    private void applyToBlocks(BlockVar blockVar, double amount,
            HexContext hexContext, CommandBuffer<EntityStore> accessor) {
        World world = accessor.getExternalData().getWorld();
        ChunkStore chunkStore = world.getChunkStore();
        ComponentType<ChunkStore, BlockHealthChunk> bhcType =
                BlockHealthModule.get().getBlockHealthChunkComponentType();

        TimeResource timeResource = world.getEntityStore().getStore()
                .getResource(TimeResource.getResourceType());
        Instant now = timeResource.getNow();
        float healAmount = (float) (amount * BLOCK_HEAL_SCALE);

        for (int i = 0; i < blockVar.size(); i++) {
            Vector3i pos = blockVar.getAt(i);
            if (pos == null) continue;

            int blockId = world.getBlock(pos.x, pos.y, pos.z);
            if (blockId == BlockType.EMPTY_ID) continue;

            long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
            Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);
            if (chunkRef == null || !chunkRef.isValid()) continue;

            BlockHealthChunk bhc = chunkStore.getStore().getComponent(chunkRef, bhcType);
            if (bhc == null) continue;

            bhc.damageBlock(now, world, pos, -healAmount);

            Vector3d blockCenter = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
            FortifyStyle.renderBlockHit(blockCenter, hexContext.getColors(), accessor);

            LOGGER.atInfo().log("fortify: healed block at %s by %.2f", pos, healAmount);
        }
    }
}
