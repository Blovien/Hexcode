package com.riprod.hexcode.builtin.glyphs.effect.erode;

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
import com.hypixel.hytale.server.core.modules.blockhealth.BlockHealth;
import com.hypixel.hytale.server.core.modules.blockhealth.BlockHealthChunk;
import com.hypixel.hytale.server.core.modules.blockhealth.BlockHealthModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.erode.component.ErodeComponent;
import com.riprod.hexcode.builtin.glyphs.effect.erode.style.ErodeStyle;
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

public class ErodeGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Erode";

    private static final String ERODE_EFFECT_ID = "Hexcode_Erode";
    private static final double DEFAULT_AMOUNT = 5.0;
    private static final double DEFAULT_DURATION = 100.0;
    private static final double MIN_AMOUNT = 1.0;
    private static final double MAX_AMOUNT = 20.0;
    private static final float VULNERABILITY_SCALE = 0.05f;
    private static final float BLOCK_DAMAGE_SCALE = 0.05f;
    private static final float FRAGILE_HP_THRESHOLD = 0.5f;

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        double amount = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveSlot("amount", hexContext), DEFAULT_AMOUNT);
        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveSlot("duration", hexContext), DEFAULT_DURATION);

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;

        float amountScale = (float) (amount / DEFAULT_AMOUNT);
        float durationScale = (float) (duration / DEFAULT_DURATION);
        float finalCost = baseCost * castMultiplier * amountScale * durationScale;

        boolean consumed = hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
        if (!consumed) {
            LOGGER.atInfo().log("erode: insufficient mana, need %.1f", finalCost);
        }
        return consumed;
    }

    @Override
    public boolean resolveVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;

        double amount = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveSlot("amount", hexContext), DEFAULT_AMOUNT);
        HexVar targets = glyph.resolveSlot("target", hexContext);

        float amountFactor = (float) (amount / DEFAULT_AMOUNT);
        int extraRolls = 0;

        if (targets instanceof BlockVar blockVar) {
            extraRolls = computeBlockVolatilityRolls(blockVar, amountFactor, hexContext);
        } else {
            extraRolls = Math.max(0, (int) (amountFactor - 1));
        }

        if (!tracker.rollAndIncrement(glyph)) {
            LOGGER.atInfo().log("erode: fizzled on primary volatility roll");
            return false;
        }

        for (int i = 0; i < extraRolls; i++) {
            if (!tracker.rollAndIncrement(glyph)) {
                LOGGER.atInfo().log("erode: fizzled on extra volatility roll %d/%d", i + 1, extraRolls);
                return false;
            }
        }

        return true;
    }

    private int computeBlockVolatilityRolls(BlockVar blockVar, float amountFactor,
            HexContext hexContext) {
        Vector3i pos = blockVar.getValue();
        if (pos == null) return 0;

        World world = hexContext.getAccessor().getExternalData().getWorld();
        int blockId = world.getBlock(pos.x, pos.y, pos.z);
        if (blockId == BlockType.EMPTY_ID) return 0;

        BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
        if (blockType == null) return 0;

        BlockGathering gathering = blockType.getGathering();
        if (gathering == null) return 0;

        int quality = 0;
        var breaking = gathering.getBreaking();
        if (breaking != null) {
            quality = breaking.getQuality();
        }

        return (int) ((1 + quality) * amountFactor) - 1;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.resolveSlot("target", hexContext);
        if (targets == null) {
            Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        double amount = Math.max(MIN_AMOUNT, Math.min(MAX_AMOUNT,
                SpellVarUtil.resolveNumberOrDefault(
                        glyph.resolveSlot("amount", hexContext), DEFAULT_AMOUNT)));
        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveSlot("duration", hexContext), DEFAULT_DURATION);
        float vulnerabilityMultiplier = (float) (amount * VULNERABILITY_SCALE);
        float durationSeconds = (float) duration;

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        if (targets instanceof EntityVar entityVar) {
            applyToEntities(entityVar, vulnerabilityMultiplier, durationSeconds, hexContext, accessor);
        } else if (targets instanceof BlockVar blockVar) {
            applyToBlocks(blockVar, amount, durationSeconds, hexContext, accessor);
        }

        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }

    private void applyToEntities(EntityVar entityVar, float vulnerabilityMultiplier,
            float durationSeconds, HexContext hexContext, CommandBuffer<EntityStore> accessor) {
        Ref<EntityStore> ref = entityVar.getRef(accessor);
        if (ref == null || !ref.isValid()) return;

        EntityEffect erodeEffect = EntityEffect.getAssetMap().getAsset(ERODE_EFFECT_ID);
        if (erodeEffect == null) {
            LOGGER.atWarning().log("erode: %s effect asset not found", ERODE_EFFECT_ID);
            return;
        }

        ErodeComponent existing = accessor.getComponent(ref, ErodeComponent.getComponentType());
        if (existing != null) {
            existing.setVulnerabilityMultiplier(vulnerabilityMultiplier);
            existing.setRemainingDuration(durationSeconds);
        } else {
            accessor.addComponent(ref, ErodeComponent.getComponentType(),
                    new ErodeComponent(vulnerabilityMultiplier, durationSeconds));
        }

        EffectControllerComponent controller = accessor.getComponent(
                ref, EffectControllerComponent.getComponentType());
        if (controller != null) {
            controller.addEffect(ref, erodeEffect, durationSeconds,
                    OverlapBehavior.OVERWRITE, accessor);
        }

        TransformComponent tc = accessor.getComponent(ref, TransformComponent.getComponentType());
        if (tc != null) {
            ErodeStyle.renderEntityHit(tc.getPosition(), hexContext.getColors(), accessor);
        }

        LOGGER.atInfo().log("erode: applied %.0f%% vulnerability for %.1fs to entity",
                vulnerabilityMultiplier * 100, durationSeconds);
    }

    private void applyToBlocks(BlockVar blockVar, double amount, float durationSeconds,
            HexContext hexContext, CommandBuffer<EntityStore> accessor) {
        Vector3i pos = blockVar.getValue();
        if (pos == null) return;

        World world = accessor.getExternalData().getWorld();
        int blockId = world.getBlock(pos.x, pos.y, pos.z);
        if (blockId == BlockType.EMPTY_ID) return;

        ChunkStore chunkStore = world.getChunkStore();
        ComponentType<ChunkStore, BlockHealthChunk> bhcType =
                BlockHealthModule.get().getBlockHealthChunkComponentType();

        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
        Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);
        if (chunkRef == null || !chunkRef.isValid()) return;

        BlockHealthChunk bhc = chunkStore.getStore().getComponent(chunkRef, bhcType);
        if (bhc == null) return;

        TimeResource timeResource = world.getEntityStore().getStore()
                .getResource(TimeResource.getResourceType());
        Instant now = timeResource.getNow();
        float damage = (float) (amount * BLOCK_DAMAGE_SCALE);

        BlockHealth result = bhc.damageBlock(now, world, pos, damage);

        if (!result.isDestroyed() && result.getHealth() <= FRAGILE_HP_THRESHOLD) {
            bhc.makeBlockFragile(pos, durationSeconds);
        }

        Vector3d blockCenter = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
        ErodeStyle.renderBlockHit(blockCenter, hexContext.getColors(), accessor);

        LOGGER.atInfo().log("erode: damaged block at %s (hp=%.2f, fragile=%s)",
                pos, result.getHealth(), result.getHealth() <= FRAGILE_HP_THRESHOLD);
    }
}
