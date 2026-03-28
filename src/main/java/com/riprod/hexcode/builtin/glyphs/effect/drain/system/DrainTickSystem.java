package com.riprod.hexcode.builtin.glyphs.effect.drain.system;

import java.util.Iterator;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.drain.component.DrainComponent;
import com.riprod.hexcode.builtin.glyphs.effect.drain.component.DrainComponent.DrainEntry;
import com.riprod.hexcode.builtin.glyphs.effect.drain.style.DrainStyle;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class DrainTickSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public Query<EntityStore> getQuery() {
        return DrainComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        try {
            DrainComponent drain = chunk.getComponent(index, DrainComponent.getComponentType());
            Ref<EntityStore> entityRef = chunk.getReferenceTo(index);

            if (drain == null || drain.isEmpty()) return;

            EntityStatMap statMap = buffer.getComponent(entityRef, EntityStatMap.getComponentType());
            if (statMap == null) {
                cleanupAll(drain, entityRef, buffer);
                return;
            }

            TransformComponent tc = buffer.getComponent(entityRef, TransformComponent.getComponentType());

            Iterator<DrainEntry> it = drain.getEntries().iterator();
            while (it.hasNext()) {
                DrainEntry entry = it.next();

                if (shouldStop(entry, statMap)) {
                    completeEntry(entry, buffer);
                    it.remove();
                    continue;
                }

                EntityStatValue sourceStat = statMap.get(entry.getSourceStatIndex());
                if (sourceStat == null) {
                    completeEntry(entry, buffer);
                    it.remove();
                    continue;
                }

                float drainAmount = entry.getDrainPerSecond() * dt;

                // clamp hp drain to leave 1 hp
                if (entry.getSourceStatIndex() == DefaultEntityStatTypes.getHealth()) {
                    float maxDrainable = sourceStat.get() - 1.0f;
                    if (maxDrainable <= 0) {
                        completeEntry(entry, buffer);
                        it.remove();
                        continue;
                    }
                    drainAmount = Math.min(drainAmount, maxDrainable);
                } else {
                    drainAmount = Math.min(drainAmount, sourceStat.get());
                }

                if (drainAmount <= 0) {
                    completeEntry(entry, buffer);
                    it.remove();
                    continue;
                }

                // drain source
                statMap.subtractStatValue(entry.getSourceStatIndex(), drainAmount);

                // convert and add to mana
                float converted = drainAmount * entry.getConversionRate();
                int manaIndex = DefaultEntityStatTypes.getMana();
                EntityStatValue manaStat = statMap.get(manaIndex);
                if (manaStat != null) {
                    float manaRoom = manaStat.getMax() - manaStat.get();
                    converted = Math.min(converted, manaRoom);
                    if (converted > 0) {
                        statMap.addStatValue(manaIndex, converted);
                    }
                }

                entry.addDrained(drainAmount);
                entry.tick(dt);

                if (tc != null) {
                    DrainStyle.renderTick(tc.getPosition(), entry.getColors(), buffer);
                }
            }

            if (drain.isEmpty()) {
                if (tc != null) {
                    DrainStyle.renderComplete(tc.getPosition(), null, buffer);
                }
                buffer.removeComponent(entityRef, DrainComponent.getComponentType());
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] DrainTickSystem failed: %s", e.getMessage());
        }
    }

    private boolean shouldStop(DrainEntry entry, EntityStatMap statMap) {
        if (entry.isExpired()) return true;

        EntityStatValue sourceStat = statMap.get(entry.getSourceStatIndex());
        if (sourceStat == null) return true;

        if (entry.getSourceStatIndex() == DefaultEntityStatTypes.getHealth()) {
            if (sourceStat.get() <= 1.0f) return true;
        } else {
            if (sourceStat.get() <= 0) return true;
        }

        int manaIndex = DefaultEntityStatTypes.getMana();
        EntityStatValue manaStat = statMap.get(manaIndex);
        if (manaStat != null && manaStat.get() >= manaStat.getMax()) return true;

        return false;
    }

    private void completeEntry(DrainEntry entry, CommandBuffer<EntityStore> buffer) {
        Ref<EntityStore> hexEntityRef = entry.getHexEntityRef();
        if (hexEntityRef != null && hexEntityRef.isValid()) {
            RootGlyph rootGlyph = buffer.getComponent(hexEntityRef, RootGlyph.getComponentType());
            if (rootGlyph != null) {
                rootGlyph.decrementExternalWaiters();
            }
        }

        HexContext ctx = entry.getHexContext();
        if (ctx != null) {
            ctx.UpdateAccessor(buffer);
            Executor.continueExecution(entry.getNextGlyphIds(), ctx);
        }
    }

    private void cleanupAll(DrainComponent drain, Ref<EntityStore> entityRef,
            CommandBuffer<EntityStore> buffer) {
        for (DrainEntry entry : drain.getEntries()) {
            completeEntry(entry, buffer);
        }
        drain.getEntries().clear();
        buffer.removeComponent(entityRef, DrainComponent.getComponentType());
    }
}
