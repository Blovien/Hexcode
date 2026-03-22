package com.riprod.hexcode.api.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.system.CancellableEcsEvent;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexes.component.Hex;

public class SpellCastEvent extends CancellableEcsEvent implements IEvent<Void> {

    private final Ref<EntityStore> casterRef;
    private final Hex hex;
    private float powerModifier = 0.0f;
    private float manaCostMultiplier = 1.0f;
    private float volatilityMultiplier = 1.0f;

    public SpellCastEvent(Ref<EntityStore> casterRef, Hex hex) {
        this.casterRef = casterRef;
        this.hex = hex;
    }

    public Ref<EntityStore> getCasterRef() {
        return casterRef;
    }

    public Hex getHex() {
        return hex;
    }

    public float getPowerModifier() {
        return powerModifier;
    }

    public void setPowerModifier(float powerModifier) {
        this.powerModifier = powerModifier;
    }

    public float getManaCostMultiplier() {
        return manaCostMultiplier;
    }

    public void setManaCostMultiplier(float manaCostMultiplier) {
        this.manaCostMultiplier = manaCostMultiplier;
    }

    public float getVolatilityMultiplier() {
        return volatilityMultiplier;
    }

    public void setVolatilityMultiplier(float volatilityMultiplier) {
        this.volatilityMultiplier = volatilityMultiplier;
    }
}
