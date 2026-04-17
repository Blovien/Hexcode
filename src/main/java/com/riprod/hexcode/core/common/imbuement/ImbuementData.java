package com.riprod.hexcode.core.common.imbuement;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.saved.SavedHexAsset;
import com.riprod.hexcode.core.state.execution.component.HexColors;

public class ImbuementData {

    @Nullable private String hexAssetId;
    @Nullable private Hex hex;
    @Nullable private String referenceId;
    private float volatilityOverride = -1f;
    private float efficiencyOverride = -1f;
    private float volatilityBudgetOverride = -1f;
    private float powerModifier = 1.0f;
    private float manaCostMultiplier = 1.0f;
    private int cooldownTicks = 20;
    @Nullable private HexColors colors;

    public ImbuementData() {
    }

    @Nullable
    public String getHexAssetId() {
        return hexAssetId;
    }

    public void setHexAssetId(@Nullable String hexAssetId) {
        this.hexAssetId = hexAssetId;
    }

    @Nullable
    public Hex getHex() {
        return hex;
    }

    public void setHex(@Nullable Hex hex) {
        this.hex = hex;
    }

    @Nullable
    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(@Nullable String referenceId) {
        this.referenceId = referenceId;
    }

    public float getVolatilityOverride() {
        return volatilityOverride;
    }

    public void setVolatilityOverride(float volatilityOverride) {
        this.volatilityOverride = volatilityOverride;
    }

    public float getEfficiencyOverride() {
        return efficiencyOverride;
    }

    public void setEfficiencyOverride(float efficiencyOverride) {
        this.efficiencyOverride = efficiencyOverride;
    }

    public float getVolatilityBudgetOverride() {
        return volatilityBudgetOverride;
    }

    public void setVolatilityBudgetOverride(float volatilityBudgetOverride) {
        this.volatilityBudgetOverride = volatilityBudgetOverride;
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

    public static final BuilderCodec<ImbuementData> CODEC = BuilderCodec
            .builder(ImbuementData.class, ImbuementData::new)
            .append(new KeyedCodec<>("HexAssetId", Codec.STRING),
                    (c, v) -> c.hexAssetId = v,
                    c -> c.hexAssetId)
            .add()
            .append(new KeyedCodec<>("Hex", Hex.CODEC),
                    (c, v) -> c.hex = v,
                    c -> c.hex)
            .add()
            .append(new KeyedCodec<>("ReferenceId", Codec.STRING),
                    (c, v) -> c.referenceId = v,
                    c -> c.referenceId)
            .addValidatorLate(() -> SavedHexAsset.VALIDATOR_CACHE.getValidator().late())
            .add()
            .append(new KeyedCodec<>("VolatilityOverride", Codec.FLOAT),
                    (c, v) -> c.volatilityOverride = v,
                    c -> c.volatilityOverride)
            .add()
            .append(new KeyedCodec<>("EfficiencyOverride", Codec.FLOAT),
                    (c, v) -> c.efficiencyOverride = v,
                    c -> c.efficiencyOverride)
            .add()
            .append(new KeyedCodec<>("VolatilityBudgetOverride", Codec.FLOAT),
                    (c, v) -> c.volatilityBudgetOverride = v,
                    c -> c.volatilityBudgetOverride)
            .add()
            .append(new KeyedCodec<>("PowerModifier", Codec.FLOAT),
                    (c, v) -> c.powerModifier = v,
                    c -> c.powerModifier)
            .add()
            .append(new KeyedCodec<>("ManaCostMultiplier", Codec.FLOAT),
                    (c, v) -> c.manaCostMultiplier = v,
                    c -> c.manaCostMultiplier)
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
