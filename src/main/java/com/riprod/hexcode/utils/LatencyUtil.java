package com.riprod.hexcode.utils;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.packets.connection.PongType;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class LatencyUtil {

    private LatencyUtil() {
    }

    public static int pingMillis(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef) {
        try {
            double avgMicros = accessor.getComponent(playerRef, PlayerRef.getComponentType())
                    .getPacketHandler()
                    .getPingInfo(PongType.Tick)
                    .getPingMetricSet()
                    .getAverage(0);
            return (int) PacketHandler.PingInfo.TIME_UNIT.toMillis(Math.round(avgMicros));
        } catch (Exception e) {
            return 0;
        }
    }
}
