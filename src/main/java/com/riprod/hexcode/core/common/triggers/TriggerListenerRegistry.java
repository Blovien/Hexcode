package com.riprod.hexcode.core.common.triggers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class TriggerListenerRegistry implements Resource<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static ResourceType<EntityStore, TriggerListenerRegistry> resourceType;

    public static ResourceType<EntityStore, TriggerListenerRegistry> getResourceType() {
        return resourceType;
    }

    public static void setResourceType(ResourceType<EntityStore, TriggerListenerRegistry> type) {
        resourceType = type;
    }

    private final Map<String, Map<UUID, List<TriggerSubscription>>> bySubject = new ConcurrentHashMap<>();
    private final Map<String, List<TriggerSubscription>> bootstraps = new ConcurrentHashMap<>();
    private final Map<String, TriggerSource> sources = new HashMap<>();
    private final Map<String, Integer> listenerCounts = new HashMap<>();

    public TriggerListenerRegistry() {
    }

    public void registerSource(@Nonnull TriggerSource source) {
        sources.put(source.key(), source);
        listenerCounts.putIfAbsent(source.key(), 0);
    }

    public void subscribe(@Nullable Store<EntityStore> store, @Nonnull TriggerSubscription sub) {
        if (sub.subjectUuid() == null) {
            bootstraps.computeIfAbsent(sub.key(), k -> new ArrayList<>()).add(sub);
            bumpCount(store, sub.key(), +1);
            return;
        }
        bySubject
                .computeIfAbsent(sub.key(), k -> new LinkedHashMap<>())
                .computeIfAbsent(sub.subjectUuid(), u -> new ArrayList<>())
                .add(sub);
        bumpCount(store, sub.key(), +1);
    }

    public boolean unsubscribe(@Nullable Store<EntityStore> store, @Nonnull String key, @Nonnull UUID subscriptionId) {
        boolean removed = false;
        Map<UUID, List<TriggerSubscription>> map = bySubject.get(key);
        if (map != null) {
            UUID subjectKey = null;
            for (Map.Entry<UUID, List<TriggerSubscription>> entry : map.entrySet()) {
                List<TriggerSubscription> list = entry.getValue();
                if (list.removeIf(s -> s.id().equals(subscriptionId))) {
                    removed = true;
                    if (list.isEmpty()) subjectKey = entry.getKey();
                    break;
                }
            }
            if (subjectKey != null) map.remove(subjectKey);
        }
        if (!removed) {
            List<TriggerSubscription> boot = bootstraps.get(key);
            if (boot != null && boot.removeIf(s -> s.id().equals(subscriptionId))) removed = true;
        }
        if (removed) bumpCount(store, key, -1);
        return removed;
    }

    public void fire(@Nonnull CommandBuffer<EntityStore> buffer, @Nonnull TriggerEvent event) {
        Store<EntityStore> store = buffer.getExternalData().getWorld().getStore();

        List<TriggerSubscription> bootList = bootstraps.get(event.key());
        if (bootList != null && !bootList.isEmpty()) {
            for (TriggerSubscription s : new ArrayList<>(bootList)) {
                invokeSafe(buffer, s, event);
            }
        }
        if (event.subjectUuid() == null) return;
        Map<UUID, List<TriggerSubscription>> bySubj = bySubject.get(event.key());
        if (bySubj == null) return;
        List<TriggerSubscription> list = bySubj.get(event.subjectUuid());
        if (list == null || list.isEmpty()) return;

        List<TriggerSubscription> snapshot = new ArrayList<>(list);
        for (TriggerSubscription s : snapshot) {
            invokeSafe(buffer, s, event);
            if (s.oneShot()) {
                if (list.remove(s)) bumpCount(store, event.key(), -1);
            }
        }
        if (list.isEmpty()) bySubj.remove(event.subjectUuid());
    }

    public int countListeners(@Nonnull String key) {
        return listenerCounts.getOrDefault(key, 0);
    }

    public boolean hasListenersFor(@Nonnull String key, @Nonnull UUID subjectUuid) {
        Map<UUID, List<TriggerSubscription>> map = bySubject.get(key);
        if (map == null) return false;
        List<TriggerSubscription> list = map.get(subjectUuid);
        return list != null && !list.isEmpty();
    }

    private void invokeSafe(CommandBuffer<EntityStore> buffer, TriggerSubscription s, TriggerEvent event) {
        try {
            s.callback().onFire(buffer, s, event);
        } catch (Exception e) {
            LOGGER.atSevere().log("trigger callback failed for %s: %s", s.key(), e.getMessage());
        }
    }

    private void bumpCount(@Nullable Store<EntityStore> store, String key, int delta) {
        int before = listenerCounts.getOrDefault(key, 0);
        int after = Math.max(0, before + delta);
        listenerCounts.put(key, after);
        if (store == null) return;
        TriggerSource source = sources.get(key);
        if (source == null) return;
        try {
            if (before == 0 && after > 0) source.onFirstListener(store);
            else if (before > 0 && after == 0) source.onLastListener(store);
        } catch (Exception e) {
            LOGGER.atWarning().log("trigger source lifecycle hook failed for %s: %s", key, e.getMessage());
        }
    }

    @Nullable
    @Override
    public Resource<EntityStore> clone() {
        return new TriggerListenerRegistry();
    }
}
