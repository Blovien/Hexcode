package com.riprod.hexcode.core.common.utilities.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class DebugComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, DebugComponent> componentType;

    public static void setComponentType(ComponentType<EntityStore, DebugComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, DebugComponent> getComponentType() {
        return componentType;
    }

    private DebugShape shape;
    private Vector3f color;
    private double scale;
    private float respawnInterval;
    private float timer;
    private float fadeMultiplier = 2.0f;
    private float intervalMultiplier = 1;
    private float scaleMultiplier = 1;

    public DebugComponent() {
        this(DebugShape.Cube, new Vector3f(0f, 1f, 0f), 0.5, 2.0f);
    }

    public DebugComponent(DebugShape shape, Vector3f color, double scale, float respawnInterval) {
        this.shape = shape;
        this.color = color;
        this.scale = scale;
        this.respawnInterval = respawnInterval;
        this.timer = 0;
    }

    public DebugShape getShape() {
        return shape;
    }

    public void setShape(DebugShape shape) {
        this.shape = shape;
    }

    public void setScaleMultiplier(float scale) {
        this.scaleMultiplier = scale;
    }

    public void resetScaleMultiplier() {
        this.scaleMultiplier = 1;
    }
    public float getScaleMultiplier() {
        return scaleMultiplier;
    }

    public Vector3f getColor() {
        return color;
    }

    public void setColor(Vector3f color) {
        this.color = color;
    }

    public double getScale() {
        return scale * scaleMultiplier;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public float getRespawnInterval() {
        return respawnInterval;
    }

    public float getFadeTime() {
        return respawnInterval * fadeMultiplier;
    }

    public void setRespawnInterval(float respawnInterval) {
        this.respawnInterval = respawnInterval;
    }

    public float getTimer() {
        return timer;
    }

    public void setTimer(float timer) {
        this.timer = timer * intervalMultiplier;
    }

    public float getFadeMultiplier() {
        return this.fadeMultiplier;
    }

    public void setFadeMultiplier(float multiplier) {
        this.fadeMultiplier = multiplier;
    }

    public void resetFadeMultipler() {
        this.fadeMultiplier = 2;
    }

    public float getIntervalMultiplier() {
        return this.intervalMultiplier;
    }

    public void setIntervalMultiplier(float multiplier) {
        this.intervalMultiplier = multiplier;
    }

    public void resetIntervalMultiplier() {
        this.intervalMultiplier = 1;
    }

    @Nonnull
    @Override
    public DebugComponent clone() {
        DebugComponent copy = new DebugComponent(this.shape, this.color, this.scale, this.respawnInterval);
        copy.timer = this.timer;
        return copy;
    }
}
