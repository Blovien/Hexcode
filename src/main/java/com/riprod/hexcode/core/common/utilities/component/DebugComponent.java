package com.riprod.hexcode.core.common.utilities.component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
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
    private Vector3d scale;
    private float respawnInterval;
    private float timer;
    private float fadeMultiplier = 2.0f;
    private float intervalMultiplier = 1;
    private float scaleMultiplier = 1;
    private float opacity = 0.8f;
    private int flags = DebugUtils.FLAG_NO_WIREFRAME;
    private Ref<EntityStore> targetRef;

    public DebugComponent() {
        this(DebugShape.Cube, new Vector3f(0f, 1f, 0f), 0.5, 2.0f);
    }

    public DebugComponent(DebugShape shape, Vector3f color, double scale, float respawnInterval) {
        this(shape, color, new Vector3d(scale, scale, scale), respawnInterval, null);
    }

    public DebugComponent(DebugShape shape, Vector3f color, double scale, float respawnInterval,
            @Nullable Ref<EntityStore> targetRef) {
        this(shape, color, new Vector3d(scale, scale, scale), respawnInterval, targetRef);
    }

    public DebugComponent(DebugShape shape, Vector3f color, Vector3d scale, float respawnInterval) {
        this(shape, color, scale, respawnInterval, null);
    }

    public DebugComponent(DebugShape shape, Vector3f color, Vector3d scale, float respawnInterval,
            @Nullable Ref<EntityStore> targetRef) {
        this.shape = shape;
        this.color = color;
        this.scale = scale;
        this.respawnInterval = respawnInterval;
        this.timer = 0;
        this.targetRef = targetRef;
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

    public Vector3d getScale() {
        return new Vector3d(
                scale.x * scaleMultiplier,
                scale.y * scaleMultiplier,
                scale.z * scaleMultiplier);
    }

    public void setScale(double scale) {
        this.scale = new Vector3d(scale, scale, scale);
    }

    public void setScale(Vector3d scale) {
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

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    @Nullable
    public Ref<EntityStore> getTargetRef() {
        return targetRef;
    }

    public void setTargetRef(@Nullable Ref<EntityStore> targetRef) {
        this.targetRef = targetRef;
    }

    @Nonnull
    @Override
    public DebugComponent clone() {
        DebugComponent copy = new DebugComponent(this.shape, this.color,
                new Vector3d(this.scale), this.respawnInterval, this.targetRef);
        copy.timer = this.timer;
        copy.fadeMultiplier = this.fadeMultiplier;
        copy.intervalMultiplier = this.intervalMultiplier;
        copy.scaleMultiplier = this.scaleMultiplier;
        copy.opacity = this.opacity;
        copy.flags = this.flags;
        return copy;
    }
}
