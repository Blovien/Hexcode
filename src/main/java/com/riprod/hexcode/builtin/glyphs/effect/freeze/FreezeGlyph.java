package com.riprod.hexcode.builtin.glyphs.effect.freeze;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.freeze.component.FreezeComponent;
import com.riprod.hexcode.builtin.glyphs.effect.freeze.component.FrozenBlock;
import com.riprod.hexcode.builtin.glyphs.effect.freeze.style.FreezeStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class FreezeGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Freeze";

    private static final double DEFAULT_DURATION = 3.0;
    private static final int TICKS_PER_SECOND = 20;

    // -- volatility tuning --
    // every N seconds of duration adds an extra volatility roll
    private static final double VOLATILITY_ROLL_INTERVAL = 2.0;

    // -- mana tuning --
    // duration at which mana cost equals the base JSON cost
    private static final double MANA_REFERENCE_DURATION = 3.0;

    @Override
    public boolean resolveVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;

        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION);

        int extraRolls = Math.max(0, (int) (duration / VOLATILITY_ROLL_INTERVAL) - 1);

        if (!tracker.rollAndIncrement(glyph)) {
            LOGGER.atInfo().log("freeze: fizzled on primary volatility roll");
            return false;
        }

        for (int i = 0; i < extraRolls; i++) {
            if (!tracker.rollAndIncrement(glyph)) {
                LOGGER.atInfo().log("freeze: fizzled on extra volatility roll %d/%d", i + 1, extraRolls);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        HexVar targets = glyph.resolveInput("target", hexContext);
        int targetCount = (targets != null) ? Math.max(targets.size(), 1) : 1;

        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION);
        float durationScale = (float) (duration / MANA_REFERENCE_DURATION);

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;
        float finalCost = baseCost * castMultiplier * durationScale * targetCount;

        boolean consumed = hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
        if (!consumed) {
            float currentMana = hexContext.getRoot().getCurrentMana(hexContext.getAccessor());
            LOGGER.atInfo().log("freeze: needs %.1f mana (duration=%.1f, targets=%d), has %.1f",
                    finalCost, duration, targetCount, currentMana);
        }
        return consumed;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.resolveInput("target", hexContext);

        if (targets == null || targets.size() == 0) {
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION);

        EntityEffect freezeEffect = EntityEffect.getAssetMap().getAsset("Freeze");
        if (freezeEffect == null) {
            LOGGER.atWarning().log("freeze: Freeze effect asset not found");
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        World world = accessor.getExternalData().getWorld();
        List<FrozenBlock> frozenBlocks = new ArrayList<>();

        if (targets instanceof EntityVar entityVar) {
            for (int i = 0; i < entityVar.size(); i++) {
                Ref<EntityStore> ref = entityVar.getRef(i, accessor);
                if (ref == null || !ref.isValid()) continue;

                try {
                    EffectControllerComponent controller = accessor.getComponent(
                            ref, EffectControllerComponent.getComponentType());
                    if (controller == null) continue;

                    controller.addEffect(ref, freezeEffect, (float) duration,
                            OverlapBehavior.OVERWRITE, accessor);

                    TransformComponent tc = accessor.getComponent(ref,
                            TransformComponent.getComponentType());
                    if (tc != null) {
                        Vector3d pos = tc.getPosition();
                        placeIceBlock(world, pos, frozenBlocks);
                        FreezeStyle.renderFreeze(pos, hexContext.getColors(), accessor);
                    }
                } catch (Exception e) {
                    LOGGER.atWarning().log("freeze: failed on entity %s: %s",
                            entityVar.getAt(i).getUuid(), e.getMessage());
                }
            }
        }

        if (!frozenBlocks.isEmpty()) {
            int durationTicks = (int) (duration * TICKS_PER_SECOND);
            spawnFreezeTracker(hexContext, frozenBlocks, durationTicks, accessor);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }

    private static void placeIceBlock(World world, Vector3d pos, List<FrozenBlock> frozenBlocks) {
        int footX = (int) Math.floor(pos.getX());
        int footY = (int) Math.floor(pos.getY()) - 1;
        int footZ = (int) Math.floor(pos.getZ());

        int blockId = world.getBlock(footX, footY, footZ);
        if (blockId == BlockType.EMPTY_ID) return;

        BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
        if (blockType == null) return;

        String typeId = blockType.getId();
        int rotationIndex = world.getBlockRotationIndex(footX, footY, footZ);
        frozenBlocks.add(new FrozenBlock(new Vector3i(footX, footY, footZ), typeId, rotationIndex));

        world.setBlock(footX, footY, footZ, "Rock_Ice");
    }

    private void spawnFreezeTracker(HexContext hexContext, List<FrozenBlock> frozenBlocks,
            int durationTicks, CommandBuffer<EntityStore> accessor) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.ensureComponent(UUIDComponent.getComponentType());
        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(accessor.getExternalData().takeNextNetworkId()));
        holder.addComponent(HexSignal.getComponentType(),
                new HexSignal(hexContext.copy(), hexContext.getRoot().getRootEntityRef(),
                        null, null, null));
        holder.addComponent(FreezeComponent.getComponentType(),
                new FreezeComponent(frozenBlocks, durationTicks));

        accessor.addEntity(holder, AddReason.SPAWN);

        RootGlyph execComp = accessor.getComponent(
                hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
        if (execComp != null) {
            execComp.incrementExternalWaiters();
        }
    }
}
