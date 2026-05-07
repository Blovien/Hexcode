package com.riprod.hexcode.core.common.imbuement.registry;

import com.riprod.hexcode.core.common.imbuement.handler.ImbuementHandler;
import com.riprod.hexcode.core.common.imbuement.handler.StandardImbuementHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ImbuementHandlerRegistry {

    private static final Map<String, ImbuementHandler> HANDLERS = new HashMap<>();

    static {
        register(StandardImbuementHandler.ID, StandardImbuementHandler.INSTANCE);
    }

    private ImbuementHandlerRegistry() {
    }

    public static void register(String id, ImbuementHandler handler) {
        HANDLERS.put(id, handler);
    }

    public static ImbuementHandler byId(String id) {
        ImbuementHandler handler = HANDLERS.get(id);
        return handler != null ? handler : StandardImbuementHandler.INSTANCE;
    }

    public static Set<String> keys() {
        return Collections.unmodifiableSet(HANDLERS.keySet());
    }
}
