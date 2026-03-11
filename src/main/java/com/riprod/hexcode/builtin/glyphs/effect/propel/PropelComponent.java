package com.riprod.hexcode.builtin.glyphs.effect.propel;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class PropelComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, PropelComponent> componentType;

    private Ref<EntityStore> hexEntityRef;
    private HexContext hexContext;
    private Integer outputSlot;
    private Ref<EntityStore> casterRef;
    private double maxDistance;
    private Vector3d spawnPosition;
    private Glyph sourceGlyph;

    public PropelComponent() {
    }

    public PropelComponent(Glyph sourceGlyph, Ref<EntityStore> hexEntityRef, HexContext hexContext,
            Integer outputSlot, Ref<EntityStore> casterRef, double maxDistance, Vector3d spawnPosition) {
        this.sourceGlyph = sourceGlyph;
        this.hexEntityRef = hexEntityRef;
        this.hexContext = hexContext;
        this.outputSlot = outputSlot;
        this.casterRef = casterRef;
        this.maxDistance = maxDistance;
        this.spawnPosition = spawnPosition;
    }

    public static void setComponentType(ComponentType<EntityStore, PropelComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, PropelComponent> getComponentType() {
        return componentType;
    }

    public Ref<EntityStore> getHexEntityRef() {
        return hexEntityRef;
    }

    public HexContext getHexContext() {
        return hexContext;
    }

    public Integer getOutputSlot() {
        return outputSlot;
    }

    public Ref<EntityStore> getCasterRef() {
        return casterRef;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public Vector3d getSpawnPosition() {
        return spawnPosition;
    }

    public Glyph getSourceGlyph() {
        return sourceGlyph;
    }

    @Nonnull
    @Override
    public PropelComponent clone() {
        PropelComponent copy = new PropelComponent();
        copy.sourceGlyph = this.sourceGlyph;
        copy.hexEntityRef = this.hexEntityRef;
        copy.hexContext = this.hexContext != null ? this.hexContext.copy() : null;
        copy.outputSlot = this.outputSlot;
        copy.casterRef = this.casterRef;
        copy.maxDistance = this.maxDistance;
        copy.spawnPosition = this.spawnPosition != null ? new Vector3d(this.spawnPosition) : null;
        return copy;
    }
}
