package com.riprod.hexcode.builtin.glyphs.effect.fortify.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class FortifyComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, FortifyComponent> componentType;

    public static ComponentType<EntityStore, FortifyComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, FortifyComponent> type) {
        componentType = type;
    }

    private float damageReduction;
    private float remainingDuration;
    private float manaConsumptionAccumulator = 0;
    private HexContext hexContext;
    private float manaCost;

    public FortifyComponent() {
    }

    public FortifyComponent(float damageReduction, float remainingDuration, HexContext hexContext, float manaCost) {
        this.damageReduction = damageReduction;
        this.remainingDuration = remainingDuration;
        this.hexContext = hexContext;
        this.manaCost = manaCost;
    }

    public float getDamageReduction() {
        return damageReduction;
    }

    public void setDamageReduction(float damageReduction) {
        this.damageReduction = damageReduction;
    }

    public float getRemainingDuration() {
        return remainingDuration;
    }

    public void setRemainingDuration(float remainingDuration) {
        this.remainingDuration = remainingDuration;
    }

    public HexContext getHexContext() {
        return hexContext;
    }

    public float getManaCost() {
        return manaCost;
    }

    public boolean tick(float dt) {
        remainingDuration -= dt;
        manaConsumptionAccumulator += dt;
        return remainingDuration <= 0;
    }

    public boolean shouldConsumeMana() {
        if (manaConsumptionAccumulator >= 1.0f) {
            manaConsumptionAccumulator = 0.0f;
            return true;
        }
        return false;
    }

    @Nonnull
    @Override
    public FortifyComponent clone() {
        return new FortifyComponent(damageReduction, remainingDuration, hexContext, manaCost);
    }
}
