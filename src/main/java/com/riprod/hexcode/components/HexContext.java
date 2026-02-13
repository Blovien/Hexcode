package com.riprod.hexcode.components;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.execution.component.HexGraph;

/**
 * Holds all of the information relating to the current hex execution
 * 
 * Will have all the various mutators and extensions that will dynamically change throughout the casting of the spell
 */
public class HexContext {
    public final Ref<EntityStore> casterRef;
    public final ComponentAccessor<EntityStore> accessor;
    public final HexGraph spellGraph;

    public HexContext(Ref<EntityStore> casterRef, ComponentAccessor<EntityStore> accessor, HexGraph spellGraph) {
        this.casterRef = casterRef;
        this.accessor = accessor;
        this.spellGraph = spellGraph;
    }
}
