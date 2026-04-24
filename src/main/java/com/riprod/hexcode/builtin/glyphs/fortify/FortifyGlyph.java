package com.riprod.hexcode.builtin.glyphs.fortify;

import java.time.Instant;

import com.hypixel.hytale.component.CommandBuffer;
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
import com.riprod.hexcode.builtin.glyphs.fortify.component.FortifyComponent;
import com.riprod.hexcode.builtin.glyphs.fortify.style.FortifyStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class FortifyGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
public String getId() { return ID; };

public static final String ID = "Fortify";

    private static final String FORTIFY_EFFECT_ID = "Hexcode_Fortify";
    private static final double DEFAULT_AMOUNT = 5.0;
    private static final double DEFAULT_DURATION = 20.0;
    private static final double MIN_AMOUNT = 1.0;
    private static final double MAX_AMOUNT = 20.0;
    private static final float REDUCTION_SCALE = 0.5f;
    private static final float BLOCK_HEAL_SCALE = 0.05f;

    @Override
    public boolean consumeVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;

        double amount = SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot(FortifyGlyphSlots.AMOUNT, hexContext), DEFAULT_AMOUNT);
        float amountScale = (float) Math.max(1.0, amount / DEFAULT_AMOUNT);

        float cost = VolatilityTracker.computeGlyphCost(glyph) * amountScale;
        return tracker.consumeVolatility(cost);
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
        HexVar targets = glyph.readSlot(FortifyGlyphSlots.TARGET, hexContext);
        if (targets == null) {
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        double amount = Math.max(MIN_AMOUNT, Math.min(MAX_AMOUNT,
                SpellVarUtil.resolveNumberOrDefault(
                        glyph.readSlot(FortifyGlyphSlots.AMOUNT, hexContext), DEFAULT_AMOUNT)));
        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot(FortifyGlyphSlots.DURATION, hexContext), DEFAULT_DURATION);
        float damageReduction = (float) (amount * REDUCTION_SCALE);

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        EntityVar entityVar = SpellVarUtil.resolveEntityVar(targets, hexContext);
        if (entityVar != null) {
            applyToEntities(glyph, entityVar, damageReduction, (float) duration, hexContext, accessor);
        } else {
            BlockVar blockVar = SpellVarUtil.resolveBlockVar(targets, hexContext);
            if (blockVar != null) applyToBlocks(blockVar, amount, hexContext, accessor);
        }

        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }

    private void applyToEntities(Glyph glyph, EntityVar entityVar, float damageReduction,
            float durationSeconds, HexContext hexContext, CommandBuffer<EntityStore> accessor) {
        Ref<EntityStore> ref = entityVar.getRef(accessor);
        if (ref == null || !ref.isValid()) return;

        EntityEffect fortifyEffect = EntityEffect.getAssetMap().getAsset(FORTIFY_EFFECT_ID);
        if (fortifyEffect == null) {
            LOGGER.atWarning().log("fortify: %s effect asset not found", FORTIFY_EFFECT_ID);
            return;
        }

        FortifyComponent existing = accessor.getComponent(ref, FortifyComponent.getComponentType());
        if (existing != null) {
            existing.setDamageReduction(damageReduction);
            existing.setRemainingDuration(durationSeconds);
        } else {
            GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
            VolatilityTracker tracker = hexContext.getVolatilityTracker();
            float castMultiplier = (tracker != null) ? tracker.getMagicPowerMultiplier() : 1.0f;
            float baseCost = ((asset != null) ? asset.getManaConsumption() : 1.0f)
                    * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

            float manaCost = baseCost * castMultiplier * damageReduction;
            accessor.addComponent(ref, FortifyComponent.getComponentType(),
                    new FortifyComponent(damageReduction, durationSeconds, hexContext.copy(), manaCost));
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

    private void applyToBlocks(BlockVar blockVar, double amount,
            HexContext hexContext, CommandBuffer<EntityStore> accessor) {
        Vector3i pos = blockVar.getValue();
        if (pos == null) return;

        World world = accessor.getExternalData().getWorld();
        int blockId = world.getBlock(pos.x, pos.y, pos.z);
        if (blockId == BlockType.EMPTY_ID) return;

        ChunkStore chunkStore = world.getChunkStore();
        ComponentType<ChunkStore, BlockHealthChunk> bhcType = BlockHealthModule.get()
                .getBlockHealthChunkComponentType();

        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
        Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);
        if (chunkRef == null || !chunkRef.isValid()) return;

        BlockHealthChunk bhc = chunkStore.getStore().getComponent(chunkRef, bhcType);
        if (bhc == null) return;

        TimeResource timeResource = world.getEntityStore().getStore()
                .getResource(TimeResource.getResourceType());
        Instant now = timeResource.getNow();
        float healAmount = (float) (amount * BLOCK_HEAL_SCALE);

        bhc.damageBlock(now, world, pos, -healAmount);

        Vector3d blockCenter = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
        FortifyStyle.renderBlockHit(blockCenter, hexContext.getColors(), accessor);

        LOGGER.atInfo().log("fortify: healed block at %s by %.2f", pos, healAmount);
    }
}
