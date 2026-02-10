package com.riprod.hexcode.core.drawing.system;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.player.component.HexcasterComponent;

public class DrawSystemManager {
    public static InteractionState EnterDrawingMode(ComponentAccessor<EntityStore> accessor,
            HexcasterComponent hexcaster,
            Ref<EntityStore> playerRef) {

        // Setup logic
        return InteractionState.NotFinished;
    }

    public static InteractionState ExitDrawingMode(ComponentAccessor<EntityStore> accessor,
            HexcasterComponent hexcaster,
            Ref<EntityStore> playerRef) {
    
        // shape detection and cleanup logic
        hexcaster.clearDrawing();
        hexcaster.clearTrailRef();

        return InteractionState.Finished;
    }

    public static InteractionState DrawingTick(ComponentAccessor<EntityStore> accessor,
            HexcasterComponent hexcaster,
            Ref<EntityStore> playerRef) {
        // Tick while drawing (sfx, animations, etc)

        if (!hexcaster.isInDrawingMode()) {
            return InteractionState.Finished;
        }

        // spawn the shape particles along the path
        InterfaceManager.spawnParticles(accessor, playerRef, hexcaster);

        return InteractionState.NotFinished;
    }
}
