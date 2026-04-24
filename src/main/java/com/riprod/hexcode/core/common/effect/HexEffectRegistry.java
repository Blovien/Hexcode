package com.riprod.hexcode.core.common.effect;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HexEffectRegistry {

    private static final Map<String, HexEffectHandler> handlers = new HashMap<>();

    private HexEffectRegistry() {
    }

    public static void register(@Nonnull String id, @Nonnull HexEffectHandler handler) {
        handlers.put(id, handler);
    }

    @Nullable
    public static HexEffectHandler get(@Nonnull String id) {
        return handlers.get(id);
    }

    @Nonnull
    public static Map<String, HexEffectHandler> getAll() {
        return handlers;
    }
}
