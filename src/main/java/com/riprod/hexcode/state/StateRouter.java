package com.riprod.hexcode.state;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

public class StateRouter {
    private static final Map<HexState, HexcodeManager> stateManagers = new EnumMap<>(HexState.class);

    public static void registerState(HexState state, HexcodeManager manager) {
        stateManagers.put(state, manager);
    }

    public static Iterable<HexcodeManager> allManagers() {
        return stateManagers.values();
    }

    @Nullable
    public static HexcodeManager route(HexState state) {
        return stateManagers.get(state);
    }
}
