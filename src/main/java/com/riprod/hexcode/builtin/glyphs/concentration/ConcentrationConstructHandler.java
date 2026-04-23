package com.riprod.hexcode.builtin.glyphs.concentration;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.concentration.component.ConcentrationTriggerComponent;
import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.state.NoState;
import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexcasterExecutionComponent;

public class ConcentrationConstructHandler implements ConstructHandler<NoState> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public boolean onTick(float dt, HexStatus<NoState> status, ConstructTickContext ctx) {
        ConcentrationTriggerComponent marker = ctx.getChunk().getComponent(
                ctx.getIndex(), ConcentrationTriggerComponent.getComponentType());
        if (marker == null) return true;

        Ref<EntityStore> casterRef = marker.getCasterRef();
        if (casterRef == null || !casterRef.isValid()) return true;

        CommandBuffer<EntityStore> buffer = ctx.getBuffer();
        HexcasterExecutionComponent execComp = buffer.getComponent(
                casterRef, HexcasterExecutionComponent.getComponentType());
        if (execComp == null || !execComp.isHoldingPrimary()) {
            return true;
        }
        return false;
    }

    @Override
    public void onCleanup(HexStatus<NoState> status, ConstructTickContext ctx) {
        try {
            if (status.getTriggeringGlyph() != null) {
                HexExecuter.continueExecution(
                        status.getTriggeringGlyph().getNextLinks(), status.getHexContext());
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("concentration: cleanup fire failed: %s", e.getMessage());
        }

        HexExecuter.fail(status.getHexContext());
    }
}
