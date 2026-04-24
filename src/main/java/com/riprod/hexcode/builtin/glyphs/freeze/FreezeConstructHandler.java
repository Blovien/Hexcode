package com.riprod.hexcode.builtin.glyphs.freeze;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.freeze.component.FrozenBlock;
import com.riprod.hexcode.builtin.glyphs.freeze.style.FreezeStyle;
import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;

public class FreezeConstructHandler implements ConstructHandler<FreezeState> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FREEZE_EFFECT_ID = "Hexcode_Freeze";

    @Override
    public boolean onTick(float dt, HexStatus<FreezeState> status, ConstructTickContext ctx) {
        FreezeState state = status.getState();
        if (state == null) return true;
        state.tick(dt);
        return state.isExpired();
    }

    @Override
    public void onCleanup(HexStatus<FreezeState> status, ConstructTickContext ctx) {
        FreezeState state = status.getState();
        if (state == null) return;

        CommandBuffer<EntityStore> buffer = ctx.getBuffer();
        Ref<EntityStore> frozenRef = ctx.getEntityRef();

        // remove the Hytale entity effect off the frozen target
        if (frozenRef != null && frozenRef.isValid()) {
            EffectControllerComponent controller = buffer.getComponent(
                    frozenRef, EffectControllerComponent.getComponentType());
            if (controller != null) {
                int effectIndex = EntityEffect.getAssetMap().getIndex(FREEZE_EFFECT_ID);
                if (effectIndex != Integer.MIN_VALUE) {
                    controller.removeEffect(frozenRef, effectIndex, buffer);
                }
            }
        }

        // restore the ice blocks to whatever was there before
        World world = buffer.getExternalData().getWorld();
        for (FrozenBlock block : state.getFrozenBlocks()) {
            Vector3i pos = block.getPosition();
            Vector3d blockCenter = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
            world.setBlock(pos.x, pos.y, pos.z, block.getBlockTypeId());
            FreezeStyle.renderMelt(blockCenter, status.getHexContext().getColors(), buffer);
        }

        LOGGER.atInfo().log("freeze: cleanup restored %d blocks", state.getFrozenBlocks().size());
    }
}
