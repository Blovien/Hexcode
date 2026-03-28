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
    private float durationSeconds;
    private float elapsedSeconds;
    private float spikeDamage;
    private float damageCooldownSeconds;
    private Map<UUID, Float> lastDamageTimeMap;
    private Vector3d center;
    private double radius;

    public RuptureComponent() {
    }

    public RuptureComponent(List<SpikeEntry> spikes, float durationSeconds, float spikeDamage,
            float damageCooldownSeconds, Vector3d center, double radius) {
        this.spikes = spikes;
        this.durationSeconds = durationSeconds;
        this.elapsedSeconds = 0;
        this.spikeDamage = spikeDamage;
        this.damageCooldownSeconds = damageCooldownSeconds;
        this.lastDamageTimeMap = new HashMap<>();
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

    public float getDurationSeconds() {
        return durationSeconds;
    }

    public float getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void incrementElapsed(float dt) {
        elapsedSeconds += dt;
    }

    public boolean isExpired() {
        return elapsedSeconds >= durationSeconds;
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
        Float lastTime = lastDamageTimeMap.get(targetId);
        if (lastTime == null) return true;
        return (elapsedSeconds - lastTime) >= damageCooldownSeconds;
    }

    public void recordDamage(UUID targetId) {
        lastDamageTimeMap.put(targetId, elapsedSeconds);
    }

    @Nonnull
    @Override
    public RuptureComponent clone() {
        RuptureComponent copy = new RuptureComponent();
        copy.spikes = this.spikes != null ? new ArrayList<>(this.spikes) : null;
        copy.durationSeconds = this.durationSeconds;
        copy.elapsedSeconds = this.elapsedSeconds;
        copy.spikeDamage = this.spikeDamage;
        copy.damageCooldownSeconds = this.damageCooldownSeconds;
        copy.lastDamageTimeMap = this.lastDamageTimeMap != null ? new HashMap<>(this.lastDamageTimeMap) : null;
        copy.center = this.center;
        copy.radius = this.radius;
        return copy;
    }
}
