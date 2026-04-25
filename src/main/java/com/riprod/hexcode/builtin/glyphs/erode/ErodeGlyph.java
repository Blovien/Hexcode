package com.riprod.hexcode.builtin.glyphs.erode;

import java.time.Instant;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
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
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.builtin.glyphs.erode.style.ErodeStyle;
import com.riprod.hexcode.core.common.construct.state.ConstructStateUtil;
import com.riprod.hexcode.core.common.construct.system.HexConstructSpawner;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.HexDirectionUtil;
import com.riprod.hexcode.utils.HexVarUtil;

public class ErodeGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
public String getId() { return ID; };

public static final String ID = "Erode";

    private static final String ERODE_EFFECT_ID = "Hexcode_Erode";
    private static final double DEFAULT_AMOUNT = 5.0;
    private static final double DEFAULT_DURATION = 100.0;
    private static final double MIN_AMOUNT = 1.0;
    private static final double MAX_AMOUNT = 20.0;
    private static final float VULNERABILITY_SCALE = 0.05f;
    private static final float BLOCK_DAMAGE_SCALE = 0.05f;
    private static final float FRAGILE_HP_THRESHOLD = 0.5f;

    @Override
    public boolean consumeVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;

        double amount = HexVarUtil.numberOrDefault(
                glyph.readSlot(ErodeGlyphSlots.AMOUNT, hexContext), DEFAULT_AMOUNT);
        float amountScale = (float) Math.max(1.0, amount / DEFAULT_AMOUNT);

        int repeatCount = tracker.getGlyphUsage(glyph.getId());
        float cost = VolatilityTracker.computeGlyphCost(glyph, repeatCount) * amountScale;
        return tracker.consumeVolatility(cost);
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.readSlot(ErodeGlyphSlots.TARGET, hexContext);
        if (targets == null) {
            LOGGER.atWarning().log("Erode: target required");
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Erode: target required");
            return;
        }

        double amount = Math.max(MIN_AMOUNT, Math.min(MAX_AMOUNT,
                HexVarUtil.numberOrDefault(
                        glyph.readSlot(ErodeGlyphSlots.AMOUNT, hexContext), DEFAULT_AMOUNT)));
        double duration = HexVarUtil.numberOrDefault(
                glyph.readSlot(ErodeGlyphSlots.DURATION, hexContext), DEFAULT_DURATION);
        float vulnerabilityMultiplier = (float) (amount * VULNERABILITY_SCALE);
        float durationSeconds = (float) duration;

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        EntityVar entityVar = HexVarUtil.resolveEntityVar(targets, hexContext);
        if (entityVar != null) {
            applyToEntities(entityVar, vulnerabilityMultiplier, durationSeconds, glyph, hexContext, accessor);
            // entity branch fires Next on construct expiry, not now
        } else {
            BlockVar blockVar = HexVarUtil.resolveBlockVar(targets, hexContext);
            if (blockVar != null) applyToBlocks(blockVar, amount, durationSeconds, hexContext, accessor);
            // block branch is one-shot; fire Next immediately
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
        }
    }

    private void applyToEntities(EntityVar entityVar, float vulnerabilityMultiplier,
            float durationSeconds, Glyph glyph, HexContext hexContext,
            CommandBuffer<EntityStore> accessor) {
        Ref<EntityStore> ref = entityVar.getRef(accessor);
        if (ref == null || !ref.isValid()) return;

        EntityEffect erodeEffect = EntityEffect.getAssetMap().getAsset(ERODE_EFFECT_ID);
        if (erodeEffect == null) {
            LOGGER.atWarning().log("erode: %s effect asset not found", ERODE_EFFECT_ID);
            return;
        }

        EffectControllerComponent controller = accessor.getComponent(
                ref, EffectControllerComponent.getComponentType());
        if (controller != null) {
            controller.addEffect(ref, erodeEffect, durationSeconds,
                    OverlapBehavior.OVERWRITE, accessor);
        }

        // refresh existing Erode status if present, else apply a fresh one
        ErodeState existing = ConstructStateUtil.findState(
                accessor, ref, ErodeGlyph.ID, ErodeState.class);
        if (existing != null) {
            existing.setVulnerabilityMultiplier(vulnerabilityMultiplier);
            existing.setRemainingDuration(durationSeconds);
            existing.setNextGlyphIds(glyph.getNextLinks());
        } else {
            ErodeState state = new ErodeState(vulnerabilityMultiplier, durationSeconds, glyph.getNextLinks());
            HexConstructSpawner.applyWithState(
                    accessor, ref, hexContext, glyph, ErodeGlyph.ID, state);
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
