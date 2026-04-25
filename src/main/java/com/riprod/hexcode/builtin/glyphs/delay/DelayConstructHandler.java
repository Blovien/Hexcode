package com.riprod.hexcode.builtin.glyphs.delay;

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
    public void onCleanup(HexStatus<DelayState> status, ConstructTickContext ctx) {
        DelayState state = status.getState();
        CommandBuffer<EntityStore> buffer = ctx.getBuffer();

        // only fire Next on natural expiry. counterspell (requestKill) and
        // volatility-budget exhaustion both route through onCleanup but must
        // NOT release the chain — the budget is the duration-pricing mechanism
        // and must have teeth.
        if (state != null && state.isExpired()) {
            TransformComponent tc = buffer.getComponent(
                    ctx.getEntityRef(), TransformComponent.getComponentType());
            if (tc != null) {
                DelayStyle.renderExpiry(tc.getPosition(), state.getColors(), buffer);
            }
            status.getHexContext().UpdateAccessor(buffer);
            HexExecuter.continueExecution(state.getNextGlyphIds(), status.getHexContext());
            LOGGER.atInfo().log("delay: naturally expired, firing %d next glyphs",
                    state.getNextGlyphIds().size());
        } else {
            LOGGER.atInfo().log("delay: terminated early (killRequested or budget exhausted); chain suppressed");
        }

        // always despawn the carrier entity regardless of cleanup cause
        buffer.tryRemoveEntity(ctx.getEntityRef(), RemoveReason.REMOVE);
    }
}
