package com.riprod.hexcode.builtin.glyphs.concentration;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.construct.state.ConstructState;

public class ConcentrationState implements ConstructState {

    @Nullable
    private Ref<EntityStore> visualRef;

    public ConcentrationState() {
    }

    public ConcentrationState(@Nullable Ref<EntityStore> visualRef) {
        this.visualRef = visualRef;
    }

    @Nullable
    public Ref<EntityStore> getVisualRef() {
        return visualRef;
    }

    @Override
    public ConcentrationState copy() {
        return new ConcentrationState(visualRef);
    }
}
