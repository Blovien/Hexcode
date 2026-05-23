package com.riprod.hexcode.core.common.obelisk.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.hypixel.hytale.math.block.BlockSphereUtil;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.core.common.obelisk.component.ObeliskBlockComponent;

import io.sentry.util.Pair;

public class ObeliskBlockUtil {
    public static List<Pair<Vector3i, ObeliskBlockComponent>> getObelisks(Vector3i center, double radius, World world) {
        List<Pair<Vector3i, ObeliskBlockComponent>> results = new ArrayList<>();

        BlockSphereUtil.forEachBlockExact(center.x, center.y, center.z, radius, world, (x, y, z, w) -> {
            ObeliskBlockComponent comp = BlockModule.getComponent(ObeliskBlockComponent.getComponentType(), w, x, y, z);
            if (comp != null) {
                results.add(new Pair<>(new Vector3i(x, y, z), comp));
            }
            return true; // continue iterating
        });

        return results;
    }

    public static List<Pair<Vector3i, ObeliskBlockComponent>> getAvailableObelisks(
            Vector3i center, double radius, World world, int maxCount) {

        List<Pair<Vector3i, ObeliskBlockComponent>> results = new ArrayList<>();

        BlockSphereUtil.forEachBlockExact(center.x, center.y, center.z, radius, world, (x, y, z, w) -> {
            ObeliskBlockComponent comp = BlockModule.getComponent(ObeliskBlockComponent.getComponentType(), w, x, y, z);
            if (comp != null && !comp.isRegistered()) {
                results.add(new Pair<>(new Vector3i(x, y, z), comp));
            }
            return true;
        });

        results.sort(Comparator.comparingLong(pair -> center.distanceSquared(pair.getFirst())));

        if (results.size() > maxCount) {
            return new ArrayList<>(results.subList(0, maxCount));
        }
        return results;
    }
}
