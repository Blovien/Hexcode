package com.riprod.hexcode.builtin.glyphs.effect.shatter;

import java.util.List;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.builtin.glyphs.effect.shatter.component.ShatterComponent;
import com.riprod.hexcode.builtin.glyphs.effect.shatter.style.ShatterStyle;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.trigger.TriggerHandler;
import com.riprod.hexcode.core.common.trigger.component.TriggerComponent;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class ShatterTriggerHandler implements TriggerHandler {

    private static final double HIT_IDENTIFICATION_RADIUS = 1.5;

    @Override
    public boolean onTick(float dt, Ref<EntityStore> entityRef,
            ArchetypeChunk<EntityStore> chunk, int index,
            TriggerComponent trigger, HexSignal signal,
            CommandBuffer<EntityStore> buffer) {

        ShatterComponent shatter = chunk.getComponent(index, ShatterComponent.getComponentType());
        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        StandardPhysicsProvider physics = chunk.getComponent(index,
                StandardPhysicsProvider.getComponentType());

        if (shatter == null || transform == null || physics == null) return true;

        if (signal == null || signal.getEntries().isEmpty()) return true;
        if (!signal.hasLiveEntries(buffer)) return true;

        StandardPhysicsProvider.STATE state = physics.getState();
        Vector3d currentPos = transform.getPosition();
        HexContext hexContext = signal.getPrimary().getHexContext();

        if (state == StandardPhysicsProvider.STATE.INACTIVE) {
            Vector3d contactPos = physics.getContactPosition();
            Ref<EntityStore> hitEntity = findEntityAtPosition(contactPos, shatter.getCasterRef(),
                    entityRef, buffer);

            HexVar resultVar = null;
            if (hitEntity != null) {
                UUIDComponent uuidComp = buffer.getComponent(hitEntity, UUIDComponent.getComponentType());
                if (uuidComp != null) {
                    resultVar = new EntityVar(uuidComp.getUuid(), hitEntity);
                }
            } else {
                resultVar = new BlockVar(contactPos.toVector3i());
            }

            ShatterStyle.renderShardHit(contactPos, hexContext.getColors(), buffer);
            continueShardExecution(signal, resultVar, buffer);
            return true;
        }

        if (state == StandardPhysicsProvider.STATE.RESTING) {
            Vector3d contactPos = physics.getContactPosition();
            HexVar resultVar = new BlockVar(contactPos.toVector3i());

            ShatterStyle.renderShardHit(contactPos, hexContext.getColors(), buffer);
            continueShardExecution(signal, resultVar, buffer);
            return true;
        }

        double distanceTraveled = new Vector3d(currentPos).subtract(shatter.getSpawnPosition()).length();
        if (distanceTraveled >= shatter.getMaxDistance()) {
            ShatterStyle.renderMiss(currentPos, hexContext.getColors(), buffer);
            continueShardExecution(signal, null, buffer);
            return true;
        }

        return false;
    }

    private Ref<EntityStore> findEntityAtPosition(Vector3d position, Ref<EntityStore> casterRef,
            Ref<EntityStore> shardRef, CommandBuffer<EntityStore> buffer) {
        List<Ref<EntityStore>> nearby = TargetUtil.getAllEntitiesInSphere(
                position, HIT_IDENTIFICATION_RADIUS, buffer);

        Ref<EntityStore> closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Ref<EntityStore> ref : nearby) {
            if (ref == null || !ref.isValid()) continue;
            if (ref.equals(casterRef) || ref.equals(shardRef)) continue;
            if (buffer.getComponent(ref, ShatterComponent.getComponentType()) != null) continue;

            TransformComponent tc = buffer.getComponent(ref, TransformComponent.getComponentType());
            UUIDComponent uuid = buffer.getComponent(ref, UUIDComponent.getComponentType());
            if (tc == null || uuid == null) continue;

            double dist = new Vector3d(position).subtract(tc.getPosition()).squaredLength();
            if (dist < closestDist) {
                closestDist = dist;
                closest = ref;
            }
        }

        return closest;
    }

    private void continueShardExecution(HexSignal signal, HexVar resultVar,
            CommandBuffer<EntityStore> buffer) {
        if (signal == null) return;

        ComponentAccessor<ChunkStore> chunkAccessor = buffer.getExternalData().getWorld()
                .getChunkStore().getStore();

        for (HexSignal.SignalEntry entry : signal.getEntries()) {
            if (entry.getNextGlyphIds() == null || entry.getNextGlyphIds().isEmpty()) continue;

            Ref<EntityStore> hexEntityRef = entry.getHexEntityRef();
            if (hexEntityRef == null || !hexEntityRef.isValid()) continue;

            RootGlyph execComp = buffer.getComponent(hexEntityRef, RootGlyph.getComponentType());
            if (execComp == null) continue;

            HexRoot root = execComp.getRoot();
            if (root == null || !root.isAlive()) continue;

            HexContext ctx = entry.getHexContext().copy();
            ctx.UpdateAccessor(buffer);
            ctx.UpdateChunkAccessor(chunkAccessor);

            if (resultVar != null && entry.getSourceGlyph() != null) {
                entry.getSourceGlyph().writeSlot("result", resultVar, ctx);
            }

            Executor.continueExecution(entry.getNextGlyphIds(), ctx);
        }
    }
}
