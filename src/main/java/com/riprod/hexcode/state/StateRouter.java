package com.riprod.hexcode.state;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Takes the state, returns the current manager for that state
 * 
 * @return
 */
public class StateRouter {
    private static final Map<HexState, HexcodeManager> stateManagers = new EnumMap<>(HexState.class);
    private static final Map<HexState, ComponentType<EntityStore, ?>> stateComponentTypes = new EnumMap<>(HexState.class);

    public static void registerState(HexState state, HexcodeManager manager) {
        stateManagers.put(state, manager);
    }

    public static void registerState(HexState state, ComponentType<EntityStore, ?> componentType) {
        stateComponentTypes.put(state, componentType);
    }

    public static Iterable<HexcodeManager> allManagers() {
        return stateManagers.values();
    }

    public static Iterable<ComponentType<EntityStore, ?>> allComponentTypes() {
        return stateComponentTypes.values();
    }

    @Nullable
    public static HexcodeManager route(HexState state) {
        return stateManagers.get(state);
    }

    public static ComponentType<EntityStore, ?> getStateComponent(HexState state) {
        return stateComponentTypes.get(state);
    }
}
