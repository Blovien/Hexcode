package com.riprod.hexcode.builtin.glyphs.freeze;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.freeze.component.FreezeComponent;
import com.riprod.hexcode.builtin.glyphs.freeze.component.FrozenBlock;
import com.riprod.hexcode.builtin.glyphs.freeze.style.FreezeStyle;
import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;
import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.state.NoState;

public class FreezeConstructHandler implements ConstructHandler<NoState> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public boolean onTick(float dt, HexStatus<NoState> status, ConstructTickContext ctx) {
        FreezeComponent freeze = ctx.getChunk().getComponent(
                ctx.getIndex(), FreezeComponent.getComponentType());
        if (freeze == null) return true;

        return false;
    }

    @Override
    public void onCleanup(HexStatus<NoState> status, ConstructTickContext ctx) {
        FreezeComponent freeze = ctx.getChunk().getComponent(
                ctx.getIndex(), FreezeComponent.getComponentType());
        if (freeze == null) return;

        CommandBuffer<EntityStore> buffer = ctx.getBuffer();
        World world = buffer.getExternalData().getWorld();

        for (FrozenBlock block : freeze.getFrozenBlocks()) {
            Vector3i pos = block.getPosition();
            Vector3d blockCenter = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);

            world.setBlock(pos.x, pos.y, pos.z, block.getBlockTypeId());
            FreezeStyle.renderMelt(blockCenter, status.getHexContext().getColors(), buffer);
        }

        LOGGER.atInfo().log("freeze: restored %d blocks after %.1fs",
                freeze.getFrozenBlocks().size(), freeze.getDurationSeconds());
    }
}
