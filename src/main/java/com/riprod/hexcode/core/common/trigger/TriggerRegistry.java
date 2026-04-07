package com.riprod.hexcode.core.common.trigger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class TriggerRegistry {
    private static final Map<String, TriggerHandler> handlers = new HashMap<>();

    private TriggerRegistry() {
    }

    public static void register(@Nonnull String id, @Nonnull TriggerHandler handler) {
        handlers.put(id, handler);
    }

    @Nullable
    public static TriggerHandler get(@Nonnull String id) {
        return handlers.get(id);
    }
}
