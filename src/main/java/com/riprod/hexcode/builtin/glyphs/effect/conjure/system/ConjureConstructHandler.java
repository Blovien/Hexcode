package com.riprod.hexcode.builtin.glyphs.effect.conjure.system;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.builtin.glyphs.effect.conjure.component.ConjureZoneComponent;
import com.riprod.hexcode.builtin.glyphs.effect.conjure.style.ConjureStyle;
import com.riprod.hexcode.core.common.construct.ConstructHandler;
import com.riprod.hexcode.core.common.construct.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexConstruct;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;

public class ConjureConstructHandler implements ConstructHandler {

    @Override
    public boolean onTick(float dt, HexConstruct construct, ConstructTickContext ctx) {
        ConjureZoneComponent zone = ctx.getChunk().getComponent(
                ctx.getIndex(), ConjureZoneComponent.getComponentType());
        TransformComponent transform = ctx.getChunk().getComponent(
                ctx.getIndex(), TransformComponent.getComponentType());
        if (zone == null || transform == null) return false;

        Velocity vel = ctx.getChunk().getComponent(ctx.getIndex(), Velocity.getComponentType());
        if (vel != null) {
            Vector3d velocity = vel.getVelocity();
            if (velocity.squaredLength() > 0) {
                Vector3d pos = transform.getPosition();
                transform.setPosition(new Vector3d(
                        pos.x + velocity.x * dt,
                        pos.y + velocity.y * dt,
                        pos.z + velocity.z * dt));
            }
        }

        Vector3d pos = transform.getPosition();
        Vector3d half = zone.getHalfExtents();
        Vector3d min = new Vector3d(pos.x - half.x, pos.y - half.y, pos.z - half.z);
        Vector3d max = new Vector3d(pos.x + half.x, pos.y + half.y, pos.z + half.z);

        List<Ref<EntityStore>> found = new java.util.ArrayList<>(
                TargetUtil.getAllEntitiesInBox(min, max, ctx.getBuffer()));

        Set<UUID> previousOccupants = zone.getNewOccupants();
        zone.setLastOccupants(previousOccupants);
        zone.setNewOccupants(new HashSet<>());

        for (Ref<EntityStore> ref : found) {
            if (ref == null || !ref.isValid()) continue;
            if (ctx.getBuffer().getComponent(ref, ConjureZoneComponent.getComponentType()) != null) continue;

            UUIDComponent uuid = ctx.getBuffer().getComponent(ref, UUIDComponent.getComponentType());
            if (uuid == null) continue;

            UUID entityId = uuid.getUuid();
            zone.getNewOccupants().add(entityId);

            if (!previousOccupants.contains(entityId)) {
                fireOnEntity(construct, ctx, zone, ref, uuid);
            }
        }

        if (zone.getInterval() > 0) {
            zone.setIntervalTimer(zone.getIntervalTimer() - dt);
            if (zone.getIntervalTimer() <= 0) {
                zone.setIntervalTimer(zone.getInterval());
                for (Ref<EntityStore> ref : found) {
                    if (ref == null || !ref.isValid()) continue;
                    UUIDComponent uuid = ctx.getBuffer().getComponent(ref, UUIDComponent.getComponentType());
                    if (uuid == null) continue;
                    if (!zone.getNewOccupants().contains(uuid.getUuid())) continue;
                    fireOnEntity(construct, ctx, zone, ref, uuid);
                }
            }
        }

        return false;
    }

    @Override
    public void onCleanup(HexConstruct construct, ConstructTickContext ctx) {
        TransformComponent transform = ctx.getBuffer().getComponent(
                ctx.getEntityRef(), TransformComponent.getComponentType());
        if (transform != null) {
            ConjureStyle.renderDespawn(transform.getPosition(),
                    construct.getHexContext().getColors(), ctx.getBuffer());
        }
    }

    private void fireOnEntity(HexConstruct construct, ConstructTickContext ctx,
            ConjureZoneComponent zone, Ref<EntityStore> entityRef, UUIDComponent entityUuid) {
        TransformComponent entityTransform = ctx.getBuffer().getComponent(
                entityRef, TransformComponent.getComponentType());

        ctx.fireConditional(hexCtx -> {
            if (entityTransform != null) {
                ConjureStyle.renderTrigger(entityTransform.getPosition(),
                        hexCtx.getColors(), ctx.getBuffer());
            }

            if (construct.getTriggeringGlyph() != null) {
                construct.getTriggeringGlyph().writeSlot("entity",
                        new EntityVar(entityUuid.getUuid(), entityRef), hexCtx);

                if (zone.getZoneRef() != null && zone.getZoneRef().isValid()) {
                    UUIDComponent zoneUuid = ctx.getBuffer().getComponent(
                            zone.getZoneRef(), UUIDComponent.getComponentType());
                    if (zoneUuid != null) {
                        construct.getTriggeringGlyph().writeSlot("conjuration",
                                new EntityVar(zoneUuid.getUuid(), zone.getZoneRef()), hexCtx);
                    }
                }
            }
        });
    }
}
