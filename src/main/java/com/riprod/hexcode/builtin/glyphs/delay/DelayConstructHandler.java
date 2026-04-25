package com.riprod.hexcode.builtin.glyphs.delay;

import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.delay.style.DelayStyle;
import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;
import com.riprod.hexcode.core.state.execution.HexExecuter;

public class DelayConstructHandler implements ConstructHandler<DelayState> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public boolean onTick(float dt, HexStatus<DelayState> status, ConstructTickContext ctx) {
        DelayState state = status.getState();
        if (state == null) return true;
        if (state.isExpired()) return true;
        state.tick(dt);
        return !drainSustain(dt, status);
    }

    @Override
    public void onEnd(HexStatus<DelayState> status, ConstructTickContext ctx) {
        DelayState state = status.getState();
        CommandBuffer<EntityStore> buffer = ctx.getBuffer();

        if (state != null) {
            TransformComponent tc = buffer.getComponent(
                    ctx.getEntityRef(), TransformComponent.getComponentType());
            if (tc != null) {
                DelayStyle.renderExpiry(tc.getPosition(), state.getColors(), buffer);
            }
            status.getHexContext().UpdateAccessor(buffer);
            HexExecuter.continueExecution(state.getNextGlyphIds(), status.getHexContext());
            LOGGER.atInfo().log("delay: ended, firing %d next glyphs",
                    state.getNextGlyphIds().size());
        }

        buffer.tryRemoveEntity(ctx.getEntityRef(), RemoveReason.REMOVE);
    }

    @Override
    public void onAbort(HexStatus<DelayState> status, ConstructTickContext ctx) {
        // counterspell / budget exhaustion: chain MUST be suppressed.
        // budget is the duration-pricing mechanism and must have teeth.
        LOGGER.atInfo().log("delay: terminated early; chain suppressed");
        ctx.getBuffer().tryRemoveEntity(ctx.getEntityRef(), RemoveReason.REMOVE);
    }

    @Override
    public List<String> getPendingNextGlyphIds(HexStatus<DelayState> status) {
        DelayState state = status.getState();
        return state != null ? state.getNextGlyphIds() : List.of();
    }

    @Override
    public void setPendingNextGlyphIds(HexStatus<DelayState> status, List<String> ids) {
        DelayState state = status.getState();
        if (state != null) state.setNextGlyphIds(ids);
    }
}
