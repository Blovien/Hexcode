package com.riprod.hexcode.core.crafting.utils;

import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.crafting.registry.ObeliskBlockComponent;
import com.riprod.hexcode.core.crafting.registry.PedestalBlockComponent;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;

public class RangeUtil {
    public static List<ObeliskBlockComponent> getNearbyObelisks(CommandBuffer<ChunkStore> accessor,
            PedestalBlockComponent pedestal, int range) {
        return null;
    }

    public static List<HexcasterComponent> getNearbyHexcasters(CommandBuffer<EntityStore> accessor,
            PedestalBlockComponent pedestal, int range) {
        return null;
    }
}
