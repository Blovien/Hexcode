package com.riprod.hexcode.builtin.glyphs.effect.arc.system;

import java.util.List;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.arc.component.ArcComponent;
import com.riprod.hexcode.builtin.glyphs.effect.arc.style.ArcStyle;
import com.riprod.hexcode.builtin.glyphs.effect.arc.utils.ArcUtils;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphRegistry;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class ArcSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public Query<EntityStore> getQuery() {
        return ArcComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        try {
            ArcComponent arc = chunk.getComponent(index, ArcComponent.getComponentType());
            Ref<EntityStore> entityRef = chunk.getReferenceTo(index);

            if (arc == null) return;

            if (!arc.hasFired()) {
                fireBranch(arc, entityRef, buffer);
                return;
            }

            float timer = arc.incrementTimer(dt);
            if (timer < arc.getDelay()) return;

            jump(arc, entityRef, buffer);
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] ArcSystem failed: %s", e.getMessage());
        }
    }

    private void fireBranch(ArcComponent arc, Ref<EntityStore> entityRef,
            CommandBuffer<EntityStore> buffer) {
        String branch = arc.getCurrentBranch();
        if (branch == null) {
            cleanup(arc, entityRef, buffer);
            return;
        }

        HexSignal signal = buffer.getComponent(entityRef, HexSignal.getComponentType());
        if (signal == null || signal.getEntries().isEmpty()) {
            cleanup(arc, entityRef, buffer);
            return;
        }

        for (HexSignal.SignalEntry entry : signal.getEntries()) {
            Ref<EntityStore> hexEntityRef = entry.getHexEntityRef();
            if (hexEntityRef == null || !hexEntityRef.isValid()) continue;

            RootGlyph execComp = buffer.getComponent(hexEntityRef, RootGlyph.getComponentType());
            if (execComp == null) continue;

            HexRoot root = execComp.getRoot();
            if (root == null || !root.isAlive()) continue;

            HexContext ctx = entry.getHexContext().copy();
            ctx.UpdateAccessor(buffer);

            UUIDComponent uuid = buffer.getComponent(entityRef, UUIDComponent.getComponentType());
            if (uuid != null && entry.getSourceGlyph() != null) {
                EntityVar targetVar = new EntityVar(EntityVar.createRef(uuid.getUuid(), entityRef));
                entry.getSourceGlyph().writeSlot("result", targetVar, ctx);
            }

            Executor.continueExecution(List.of(branch), ctx);
        }

        arc.advanceBranch();
        arc.setHasFired(true);
        arc.resetTimer();

        if (!arc.hasMoreBranches()) {
            cleanup(arc, entityRef, buffer);
            return;
        }

        LOGGER.atInfo().log("arc: hop %d fired, %d branches remaining",
                arc.getBranchIndex(), arc.getBranches().size() - arc.getBranchIndex());
    }

    private void jump(ArcComponent arc, Ref<EntityStore> entityRef,
            CommandBuffer<EntityStore> buffer) {
        HexSignal signal = buffer.getComponent(entityRef, HexSignal.getComponentType());
        if (signal == null || signal.getEntries().isEmpty()) {
            cleanup(arc, entityRef, buffer);
            return;
        }

        HexSignal.SignalEntry entry = signal.getPrimary();
        HexContext ctx = entry.getHexContext();
        ctx.UpdateAccessor(buffer);

        GlyphHandler handler = GlyphRegistry.get(arc.getArcGlyph().getGlyphId());
        if (handler != null && !handler.canExecute(arc.getArcGlyph(), ctx)) {
            LOGGER.atInfo().log("arc: fizzled at branch %d (volatility/mana)", arc.getBranchIndex());
            cleanup(arc, entityRef, buffer);
            return;
        }

        TransformComponent tc = buffer.getComponent(entityRef, TransformComponent.getComponentType());
        if (tc == null) {
            LOGGER.atInfo().log("arc: current entity has no transform, ending chain");
            cleanup(arc, entityRef, buffer);
            return;
        }
        Vector3d fromPos = tc.getPosition();

        Ref<EntityStore> nextTarget = ArcUtils.getNextArcTarget(
                fromPos, arc.getMaxJumpDistance(), arc.getVisitedEntities(),
                ctx.getCasterRef(), buffer);

        if (nextTarget == null) {
            LOGGER.atInfo().log("arc: no valid targets, ending chain at branch %d",
                    arc.getBranchIndex());
            cleanup(arc, entityRef, buffer);
            return;
        }

        UUIDComponent nextUuid = buffer.getComponent(nextTarget, UUIDComponent.getComponentType());
        if (nextUuid != null) {
            arc.getVisitedEntities().add(nextUuid.getUuid());
        }

        TransformComponent nextTc = buffer.getComponent(nextTarget, TransformComponent.getComponentType());
        Vector3d nextPos = nextTc != null ? nextTc.getPosition() : fromPos;

        HexColors colors = ctx.getColors();
        World world = buffer.getExternalData().getWorld();
        ArcStyle.renderArc(buffer, world, fromPos, nextPos, colors);
        ArcUtils.applyShockEffect(buffer, nextTarget);
        ArcStyle.renderHit(buffer, nextPos, colors);

        ArcComponent nextHop = arc.createNextHop();
        HexSignal nextSignal = signal.clone();

        for (HexSignal.SignalEntry sigEntry : signal.getEntries()) {
            Ref<EntityStore> hexRef = sigEntry.getHexEntityRef();
            if (hexRef == null || !hexRef.isValid()) continue;
            RootGlyph rootGlyph = buffer.getComponent(hexRef, RootGlyph.getComponentType());
            if (rootGlyph != null) {
                rootGlyph.removeDependent(entityRef);
                rootGlyph.addDependent(nextTarget);
            }
        }

        buffer.removeComponent(entityRef, ArcComponent.getComponentType());
        buffer.removeComponent(entityRef, HexSignal.getComponentType());
        buffer.addComponent(nextTarget, ArcComponent.getComponentType(), nextHop);
        buffer.addComponent(nextTarget, HexSignal.getComponentType(), nextSignal);

        LOGGER.atInfo().log("arc: jumped to next entity, branch %d", arc.getBranchIndex());
    }

    private void cleanup(ArcComponent arc, Ref<EntityStore> entityRef,
            CommandBuffer<EntityStore> buffer) {
        HexSignal signal = buffer.getComponent(entityRef, HexSignal.getComponentType());

        TransformComponent tc = buffer.getComponent(entityRef, TransformComponent.getComponentType());
        if (tc != null) {
            HexColors colors = signal != null && signal.getPrimary() != null
                    ? signal.getPrimary().getHexContext().getColors() : null;
            ArcStyle.renderFizzle(buffer, tc.getPosition(), colors);
        }

        if (signal != null) {
            signal.removeDependentFromAllRoots(buffer, entityRef);
        }

        buffer.removeComponent(entityRef, ArcComponent.getComponentType());
        buffer.removeComponent(entityRef, HexSignal.getComponentType());
    }
}
