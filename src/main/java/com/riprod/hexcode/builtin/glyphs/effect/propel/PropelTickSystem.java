package com.riprod.hexcode.builtin.glyphs.effect.propel;

import java.util.List;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.RootGlyph;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.execution.component.HexRoot;
import com.riprod.hexcode.core.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;

public class PropelTickSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final double HIT_IDENTIFICATION_RADIUS = 1.5;

    @Override
    public Query<EntityStore> getQuery() {
        return PropelComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        PropelComponent propel = chunk.getComponent(index, PropelComponent.getComponentType());
        Ref<EntityStore> projectileRef = chunk.getReferenceTo(index);
        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        StandardPhysicsProvider physics = chunk.getComponent(index,
                StandardPhysicsProvider.getComponentType());

        if (propel == null || transform == null || physics == null) {
            removeProjectile(projectileRef, buffer);
            return;
        }

        Ref<EntityStore> hexEntityRef = propel.getHexEntityRef();
        if (hexEntityRef == null || !hexEntityRef.isValid()) {
            removeProjectile(projectileRef, buffer);
            return;
        }

        StandardPhysicsProvider.STATE state = physics.getState();
        Vector3d currentPos = transform.getPosition();

        if (state == StandardPhysicsProvider.STATE.INACTIVE) {
            // entity collision detected by physics system
            Vector3d contactPos = physics.getContactPosition();
            Ref<EntityStore> hitEntity = findEntityAtPosition(contactPos, propel.getCasterRef(),
                    projectileRef, buffer);

            if (hitEntity != null) {
                UUIDComponent uuidComp = buffer.getComponent(hitEntity, UUIDComponent.getComponentType());
                if (uuidComp != null) {
                    EntityVar resultVar = new EntityVar(uuidComp.getUuid(), hitEntity);
                    propel.getHexContext().setVariable(propel.getOutputSlot(), resultVar);
                }

                TransformComponent hitTransform = buffer.getComponent(hitEntity,
                        TransformComponent.getComponentType());
                Vector3d hitPos = hitTransform != null ? hitTransform.getPosition() : contactPos;
                PropelGlyphStyle.renderEntityHit(currentPos, hitPos, buffer);
            } else {
                PropelGlyphStyle.renderBlockHit(contactPos, buffer);
            }

            continuePropelExecution(propel, buffer);
            removeProjectile(projectileRef, buffer);
            return;
        }

        if (state == StandardPhysicsProvider.STATE.RESTING) {
            // block/ground collision detected by physics system
            Vector3d contactPos = physics.getContactPosition();
            BlockVar resultVar = new BlockVar(contactPos.toVector3i());
            propel.getHexContext().setVariable(propel.getOutputSlot(), resultVar);

            PropelGlyphStyle.renderBlockHit(contactPos, buffer);
            continuePropelExecution(propel, buffer);
            removeProjectile(projectileRef, buffer);
            return;
        }

        // still active — check max distance
        double distanceTraveled = new Vector3d(currentPos).subtract(propel.getSpawnPosition()).length();
        if (distanceTraveled >= propel.getMaxDistance()) {
            PropelGlyphStyle.renderMiss(currentPos, buffer);
            continuePropelExecution(propel, buffer);
            removeProjectile(projectileRef, buffer);
        }
    }

    private Ref<EntityStore> findEntityAtPosition(Vector3d position, Ref<EntityStore> casterRef,
            Ref<EntityStore> projectileRef, CommandBuffer<EntityStore> buffer) {
        List<Ref<EntityStore>> nearby = TargetUtil.getAllEntitiesInSphere(
                position, HIT_IDENTIFICATION_RADIUS, buffer);

        Ref<EntityStore> closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Ref<EntityStore> ref : nearby) {
            if (ref == null || !ref.isValid())
                continue;
            if (ref.equals(casterRef) || ref.equals(projectileRef))
                continue;
            if (buffer.getComponent(ref, PropelComponent.getComponentType()) != null)
                continue;

            TransformComponent tc = buffer.getComponent(ref, TransformComponent.getComponentType());
            UUIDComponent uuid = buffer.getComponent(ref, UUIDComponent.getComponentType());
            if (tc == null || uuid == null)
                continue;

            double dist = new Vector3d(position).subtract(tc.getPosition()).squaredLength();
            if (dist < closestDist) {
                closestDist = dist;
                closest = ref;
            }
        }

        return closest;
    }

    private void continuePropelExecution(PropelComponent propel, CommandBuffer<EntityStore> buffer) {
        Ref<EntityStore> hexEntityRef = propel.getHexEntityRef();
        if (hexEntityRef == null || !hexEntityRef.isValid())
            return;

        RootGlyph execComp = buffer.getComponent(hexEntityRef,
                RootGlyph.getComponentType());
        if (execComp == null)
            return;

        HexRoot root = execComp.getRoot();
        if (root == null || !root.isAlive())
            return;

        ComponentAccessor<ChunkStore> chunkAccessor = buffer.getExternalData().getWorld()
                .getChunkStore().getStore();

        HexContext hexContext = propel.getHexContext();

        hexContext.UpdateAccessor(buffer);
        hexContext.UpdateChunkAccessor(chunkAccessor);

        Executor.continueExecution(propel.getSourceGlyph().getNext(), hexContext);

        execComp.decrementExternalWaiters();
    }

    private void removeProjectile(Ref<EntityStore> projectileRef, CommandBuffer<EntityStore> buffer) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        buffer.removeEntity(projectileRef, holder, RemoveReason.REMOVE);
    }
}
