package com.riprod.hexcode.core.common.triggers.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TriggerRegistry {

    private static final Map<String, Trigger> triggers = new HashMap<>();

    private TriggerRegistry() {
    }

    public static void register(@Nonnull Trigger trigger) {
        triggers.put(trigger.getId(), trigger);
    }

    @Nullable
    public static Trigger get(@Nonnull String id) {
        return triggers.get(id);
    }

    @Nonnull
    public static Collection<Trigger> all() {
        return triggers.values();
    }

    @Nonnull
    public static Set<String> keys() {
        return new HashSet<>(triggers.keySet());
    }

    public static boolean isProfileSlotEligible(@Nonnull String id) {
        Trigger t = triggers.get(id);
        return t != null && t.isProfileSlotEligible();
    }
}
