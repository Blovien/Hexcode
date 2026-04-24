package com.riprod.hexcode.core.state.execution.events;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.saved.SavedHexAsset;
import com.riprod.hexcode.core.common.imbuement.ImbuementData;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;

public class CastingEventData {

    @Nullable
    private String hexCompressedId;
    @Nullable
    private Hex hex;
    @Nullable
    private HexRoot hexRoot;
    @Nullable
    private String hexAssetId;
    private VolatilityTracker volatilityTracker;
    private float manaCost = -1f;
    private float manaMultiplier = 1.0f;
    private int cooldownTicks = 20;
    @Nullable
    private HexColors colors;

    private Ref<EntityStore> targetRef;

    public CastingEventData() {
    }

    public CastingEventData(Hex hex, Ref<EntityStore> targetRef, float manaCost, HexRoot hexRoot, @Nullable HexColors colors, VolatilityTracker volatilityTracker) {
        this.hex = hex;
        this.targetRef = targetRef;
        this.manaCost = manaCost;
        this.hexRoot = hexRoot;
        this.colors = colors;
        this.volatilityTracker = volatilityTracker;
    }

    public Ref<EntityStore> getTargetRef() {
        return targetRef;
    }

    public void setTargetRef(Ref<EntityStore> targetRef) {
        this.targetRef = targetRef;
    }

    public void setHexRoot(HexRoot hexRoot) {
        this.hexRoot = hexRoot;
    }

    public HexRoot getHexRoot() {
        return hexRoot;
    }

    // utility

    public void hydrate(Ref<EntityStore> targetRef, HexRoot hexRoot) {
        this.targetRef = targetRef;
        this.hexRoot = hexRoot;
    }

    // persistence

    @Nullable
    public String getHexCompressedId() {
        return hexCompressedId;
    }

    public void setHexCompressedId(@Nullable String hexCompressedId) {
        this.hexCompressedId = hexCompressedId;
    }

    @Nullable
    public Hex getHex() {
        return hex;
    }

    public void setHex(@Nullable Hex hex) {
        this.hex = hex;
    }

    @Nullable
    public String getHexAssetId() {
        return hexAssetId;
    }

    public void setHexAssetId(@Nullable String hexAssetId) {
        this.hexAssetId = hexAssetId;
    }

    public float getVolatilityOverride() {
        return this.volatilityTracker.getStartingBudget();
    }

    public void setVolatilityOverride(float volatilityOverride) {
        this.volatilityTracker.setBudget(volatilityOverride);
        this.volatilityTracker.setStartingBudget(volatilityOverride);
    }

    public float getManaMultiplier() {
        return manaMultiplier;
    }

    public void setManaMultiplier(float manaCostMultiplier) {
        this.manaMultiplier = manaCostMultiplier;
    }

    public float getManaCost() {
        return manaCost * manaMultiplier;
    }

    public void setManaCost(float manaCost) {
        this.manaCost = manaCost;
    }

    public float getBaseManaCost() {
        return manaCost;
    }

    public float getVolatilityMultiplier() {
        return this.volatilityTracker.getVolatilityMultiplier();
    }

    public void setVolatilityMultiplier(float volatilityMultiplier) {
        this.volatilityTracker.setVolatilityMultiplier(volatilityMultiplier);
    }

    public float getPowerMultiplier() {
        return this.volatilityTracker.getMagicPowerMultiplier();
    }

    public void setPowerMultiplier(float powerMultiplier) {
        this.volatilityTracker.setMagicPowerMultiplier(powerMultiplier);
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    @Nullable
    public HexColors getColors() {
        return colors;
    }

    public void setColors(@Nullable HexColors colors) {
        this.colors = colors;
    }

    public VolatilityTracker getVolatilityTracker() {
        return volatilityTracker;
    }

    public void setVolatilityTracker(VolatilityTracker volatilityTracker) {
        this.volatilityTracker = volatilityTracker;
    }

    public static final BuilderCodec<CastingEventData> CODEC = BuilderCodec
            .builder(CastingEventData.class, CastingEventData::new)
            .append(new KeyedCodec<>("CompressedId", Codec.STRING),
                    (c, v) -> c.hexCompressedId = v,
                    c -> c.hexCompressedId)
            .add()
            .append(new KeyedCodec<>("Hex", Hex.CODEC),
                    (c, v) -> c.hex = v,
                    c -> c.hex)
            .add()
            .append(new KeyedCodec<>("AssetId", Codec.STRING),
                    (c, v) -> c.hexAssetId = v,
                    c -> c.hexAssetId)
            .addValidatorLate(() -> SavedHexAsset.VALIDATOR_CACHE.getValidator().late())
            .add()
            .append(new KeyedCodec<>("ManaCost", Codec.FLOAT),
                    (c, v) -> c.manaCost = v,
                    c -> c.manaCost)
            .add()
            .append(new KeyedCodec<>("ManaMultiplier", Codec.FLOAT),
                    (c, v) -> c.manaMultiplier = v,
                    c -> c.manaMultiplier)
            .add()
            .append(new KeyedCodec<>("VolatilityTracker", VolatilityTracker.CODEC),
                    (c, v) -> c.volatilityTracker = v,
                    c -> c.volatilityTracker)
            .add()
            .append(new KeyedCodec<>("CooldownTicks", Codec.INTEGER),
                    (c, v) -> c.cooldownTicks = v,
                    c -> c.cooldownTicks)
            .add()
            .append(new KeyedCodec<>("Colors", HexColors.CODEC),
                    (c, v) -> c.colors = v,
                    c -> c.colors)
            .add()
            .build();
}
