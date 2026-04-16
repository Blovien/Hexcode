package com.riprod.hexcode.builtin.glyphs.effect.concentration;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.concentration.component.ConcentrationTriggerComponent;
import com.riprod.hexcode.core.common.construct.ConstructHandler;
import com.riprod.hexcode.core.common.construct.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexConstruct;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexcasterExecutionComponent;

public class ConcentrationConstructHandler implements ConstructHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public boolean onTick(float dt, HexConstruct construct, ConstructTickContext ctx) {
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
    public void onCleanup(HexConstruct construct, ConstructTickContext ctx) {
        if (construct.getHexContext() == null) return;

        try {
            if (construct.getTriggeringGlyph() != null) {
                ctx.fireBranch(construct.getTriggeringGlyph().getNextLinks());
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("concentration: cleanup fire failed: %s", e.getMessage());
        }

        Executor.fail(construct.getHexContext());
    }
}
