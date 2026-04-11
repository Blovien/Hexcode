package com.riprod.hexcode.builtin.glyphs.effect.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.builtin.glyphs.effect.domain.component.DomainAuraComponent;
import com.riprod.hexcode.builtin.glyphs.effect.domain.component.DomainZoneComponent;
import com.riprod.hexcode.builtin.glyphs.effect.domain.style.DomainStyle;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.trigger.TriggerHandler;
import com.riprod.hexcode.core.common.trigger.component.TriggerComponent;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class DomainTriggerHandler implements TriggerHandler {

    private static final float AMBIENT_INTERVAL = 1.0f;
    private static final float DOMAIN_VOLATILITY_BOOST = 1.5f;
    private static final Vector3f CONTESTED_COLOR = new Vector3f(0.5f, 0.5f, 0.5f);

    @Override
    public boolean onTick(float dt, Ref<EntityStore> entityRef,
            ArchetypeChunk<EntityStore> chunk, int index,
            TriggerComponent trigger, HexSignal signal,
            CommandBuffer<EntityStore> buffer) {

        DomainZoneComponent zone = chunk.getComponent(index, DomainZoneComponent.getComponentType());
        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (zone == null || transform == null) return true;

        if (signal == null || !signal.hasLiveEntries(buffer)) return true;

        HexSignal.SignalEntry primary = signal.getPrimary();
        if (primary == null) return true;

        Ref<EntityStore> hexEntityRef = primary.getHexEntityRef();
        if (hexEntityRef == null || !hexEntityRef.isValid()) return true;

        RootGlyph execComp = buffer.getComponent(hexEntityRef, RootGlyph.getComponentType());
        if (execComp == null) return true;

        HexRoot root = execComp.getRoot();
        if (root == null || !root.isAlive()) return true;

        // passive mana drain
        float drainCost = zone.getBaseDrainPerSecond() * dt;
        if (!root.tryConsumeMana(drainCost, buffer)) return true;

        // contestation
        updateContestation(zone, transform.getPosition(), entityRef, buffer);

        // entity detection
        Vector3d center = transform.getPosition();
        List<Ref<EntityStore>> found = new ArrayList<>(
                TargetUtil.getAllEntitiesInSphere(center, zone.getRadius(), buffer));

        Set<UUID> previousOccupants = zone.getNewOccupants();
        zone.setLastOccupants(previousOccupants);
        zone.setNewOccupants(new HashSet<>());

        boolean casterInside = false;

        for (Ref<EntityStore> ref : found) {
            if (ref == null || !ref.isValid()) continue;
            if (buffer.getComponent(ref, DomainZoneComponent.getComponentType()) != null) continue;

            UUIDComponent uuid = buffer.getComponent(ref, UUIDComponent.getComponentType());
            if (uuid == null) continue;

            UUID entityId = uuid.getUuid();
            zone.getNewOccupants().add(entityId);

            if (entityId.equals(zone.getCasterUuid())) {
                casterInside = true;
                continue;
            }

            if (!previousOccupants.contains(entityId)) {
                if (!root.tryConsumeMana(zone.getTriggerDrainCost(), buffer)) return true;

                zone.incrementTriggerCount();
                fireOnEntity(zone, signal, ref, uuid, buffer);
            }
        }

        // manage caster aura component
        updateCasterAura(zone, casterInside, entityRef, buffer);

        // ambient vfx
        zone.setAmbientTimer(zone.getAmbientTimer() - dt);
        if (zone.getAmbientTimer() <= 0) {
            zone.setAmbientTimer(AMBIENT_INTERVAL);
            HexContext ctx = primary.getHexContext();
            DomainStyle.renderAmbient(center, zone.getRadius(),
                    ctx != null ? ctx.getColors() : null, buffer);
        }

        return false;
    }

    @Override
    public void onCleanup(Ref<EntityStore> entityRef, TriggerComponent trigger,
            HexSignal signal, CommandBuffer<EntityStore> buffer) {
        DomainZoneComponent zone = buffer.getComponent(entityRef, DomainZoneComponent.getComponentType());

        // remove aura from caster
        if (zone != null && zone.getCasterRef() != null && zone.getCasterRef().isValid()) {
            DomainAuraComponent aura = buffer.getComponent(
                    zone.getCasterRef(), DomainAuraComponent.getComponentType());
            if (aura != null && entityRef.equals(aura.getZoneRef())) {
                buffer.removeComponent(zone.getCasterRef(), DomainAuraComponent.getComponentType());
            }
        }

        TransformComponent transform = buffer.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform != null) {
            HexSignal.SignalEntry entry = signal != null ? signal.getPrimary() : null;
            float radius = zone != null ? zone.getRadius() : 5.0f;
            DomainStyle.renderDespawn(transform.getPosition(), radius,
                    entry != null ? entry.getHexContext().getColors() : null, buffer);
        }
    }

    private void updateCasterAura(DomainZoneComponent zone, boolean casterInside,
            Ref<EntityStore> zoneEntityRef, CommandBuffer<EntityStore> buffer) {
        Ref<EntityStore> casterRef = zone.getCasterRef();
        if (casterRef == null || !casterRef.isValid()) return;

        DomainAuraComponent existing = buffer.getComponent(casterRef, DomainAuraComponent.getComponentType());
        boolean shouldHaveAura = casterInside && !zone.isContested();

        if (shouldHaveAura && existing == null) {
            buffer.addComponent(casterRef, DomainAuraComponent.getComponentType(),
                    new DomainAuraComponent(zoneEntityRef, DOMAIN_VOLATILITY_BOOST));
        } else if (shouldHaveAura && existing != null) {
            // update ref in case it changed (shouldn't normally, but safe)
            if (!zoneEntityRef.equals(existing.getZoneRef())) {
                buffer.removeComponent(casterRef, DomainAuraComponent.getComponentType());
                buffer.addComponent(casterRef, DomainAuraComponent.getComponentType(),
                        new DomainAuraComponent(zoneEntityRef, DOMAIN_VOLATILITY_BOOST));
            }
        } else if (!shouldHaveAura && existing != null && zoneEntityRef.equals(existing.getZoneRef())) {
            buffer.removeComponent(casterRef, DomainAuraComponent.getComponentType());
        }
    }

    private void fireOnEntity(DomainZoneComponent zone, HexSignal signal,
            Ref<EntityStore> entityRef, UUIDComponent entityUuid,
            CommandBuffer<EntityStore> buffer) {

        TransformComponent entityTransform = buffer.getComponent(entityRef,
                TransformComponent.getComponentType());

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

            if (entityTransform != null) {
                DomainStyle.renderTrigger(entityTransform.getPosition(), ctx.getColors(), buffer);
            }

            if (entry.getSourceGlyph() != null) {
                entry.getSourceGlyph().writeSlot("entity",
                        new EntityVar(entityUuid.getUuid(), entityRef), ctx);

                if (zone.getZoneRef() != null && zone.getZoneRef().isValid()) {
                    UUIDComponent zoneUuid = buffer.getComponent(zone.getZoneRef(),
                            UUIDComponent.getComponentType());
                    if (zoneUuid != null) {
                        entry.getSourceGlyph().writeSlot("domain",
                                new EntityVar(zoneUuid.getUuid(), zone.getZoneRef()), ctx);
                    }
                }
            }

            Executor.continueExecution(entry.getNextGlyphIds(), ctx);
        }
    }

    private void updateContestation(DomainZoneComponent self, Vector3d selfCenter,
            Ref<EntityStore> selfRef, CommandBuffer<EntityStore> buffer) {
        Store<EntityStore> store = buffer.getExternalData().getWorld().getEntityStore().getStore();
        boolean wasContested = self.isContested();
        final boolean[] nowContested = {false};

        store.forEachChunk(DomainZoneComponent.getComponentType(), (otherChunk, buf) -> {
            for (int i = 0; i < otherChunk.size(); i++) {
                Ref<EntityStore> otherRef = otherChunk.getReferenceTo(i);
                if (otherRef.equals(selfRef)) continue;

                DomainZoneComponent other = otherChunk.getComponent(i, DomainZoneComponent.getComponentType());
                if (other == null) continue;

                TransformComponent otherTransform = buf.getComponent(otherRef, TransformComponent.getComponentType());
                if (otherTransform == null) continue;

                double dist = new Vector3d(selfCenter).subtract(otherTransform.getPosition()).length();
                if (dist < self.getRadius() + other.getRadius()) {
                    if (self.getPower() <= other.getPower()) {
                        nowContested[0] = true;
                        return;
                    }
                }
            }
        });

        self.setContested(nowContested[0]);

        if (nowContested[0] && !wasContested) {
            HexSignal signal = buffer.getComponent(selfRef, HexSignal.getComponentType());
            HexSignal.SignalEntry entry = signal != null ? signal.getPrimary() : null;
            DomainStyle.renderContested(selfCenter,
                    entry != null ? entry.getHexContext().getColors() : null, buffer);

            DebugComponent debug = buffer.getComponent(selfRef, DebugComponent.getComponentType());
            if (debug != null) {
                debug.setColor(CONTESTED_COLOR);
            }
        } else if (!nowContested[0] && wasContested) {
            HexSignal signal = buffer.getComponent(selfRef, HexSignal.getComponentType());
            HexSignal.SignalEntry entry = signal != null ? signal.getPrimary() : null;
            if (entry != null) {
                DebugComponent debug = buffer.getComponent(selfRef, DebugComponent.getComponentType());
                if (debug != null) {
                    debug.setColor(DomainStyle.resolveColor(entry.getHexContext().getColors()));
                }
            }
        }
    }
}
