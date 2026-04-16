package com.riprod.hexcode.core.common.construct;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConstructRegistry {

    private static final Map<String, ConstructHandler> handlers = new HashMap<>();

    private ConstructRegistry() {
    }

    public static void register(@Nonnull String id, @Nonnull ConstructHandler handler) {
        handlers.put(id, handler);
    }

    @Nullable
    public static ConstructHandler get(@Nonnull String id) {
        return handlers.get(id);
    }

    @Nonnull
    public static Map<String, ConstructHandler> getAll() {
        return new HashMap<>(handlers);
    }
}
