package com.riprod.hexcode.builtin.glyphs.effect.rupture.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class RuptureComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, RuptureComponent> componentType;

    private List<SpikeEntry> spikes;
    private int durationTicks;
    private int elapsedTicks;
    private float spikeDamage;
    private int damageCooldownTicks;
    private Map<UUID, Integer> lastDamageTickMap;
    private Vector3d center;
    private double radius;

    public RuptureComponent() {
    }

    public RuptureComponent(List<SpikeEntry> spikes, int durationTicks, float spikeDamage,
            int damageCooldownTicks, Vector3d center, double radius) {
        this.spikes = spikes;
        this.durationTicks = durationTicks;
        this.elapsedTicks = 0;
        this.spikeDamage = spikeDamage;
        this.damageCooldownTicks = damageCooldownTicks;
        this.lastDamageTickMap = new HashMap<>();
        this.center = center;
        this.radius = radius;
    }

    public static void setComponentType(ComponentType<EntityStore, RuptureComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, RuptureComponent> getComponentType() {
        return componentType;
    }

    public List<SpikeEntry> getSpikes() {
        return spikes;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public int getElapsedTicks() {
        return elapsedTicks;
    }

    public void incrementElapsed() {
        elapsedTicks++;
    }

    public boolean isExpired() {
        return elapsedTicks >= durationTicks;
    }

    public float getSpikeDamage() {
        return spikeDamage;
    }

    public Vector3d getCenter() {
        return center;
    }

    public double getRadius() {
        return radius;
    }

    public boolean canDamageTarget(UUID targetId) {
        Integer lastTick = lastDamageTickMap.get(targetId);
        if (lastTick == null) return true;
        return (elapsedTicks - lastTick) >= damageCooldownTicks;
    }

    public void recordDamage(UUID targetId) {
        lastDamageTickMap.put(targetId, elapsedTicks);
    }

    @Nonnull
    @Override
    public RuptureComponent clone() {
        RuptureComponent copy = new RuptureComponent();
        copy.spikes = this.spikes != null ? new ArrayList<>(this.spikes) : null;
        copy.durationTicks = this.durationTicks;
        copy.elapsedTicks = this.elapsedTicks;
        copy.spikeDamage = this.spikeDamage;
        copy.damageCooldownTicks = this.damageCooldownTicks;
        copy.lastDamageTickMap = this.lastDamageTickMap != null ? new HashMap<>(this.lastDamageTickMap) : null;
        copy.center = this.center;
        copy.radius = this.radius;
        return copy;
    }
}
