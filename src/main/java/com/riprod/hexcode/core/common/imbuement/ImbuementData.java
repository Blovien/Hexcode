package com.riprod.hexcode.core.common.imbuement;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.riprod.hexcode.core.common.hexes.codec.HexCacheResource;
import com.riprod.hexcode.core.common.hexes.codec.HexCodec;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.saved.SavedHexAsset;
import com.riprod.hexcode.core.state.execution.component.HexColors;

public class ImbuementData {

    @Nullable
    private String hexCompressedId;
    @Nullable
    private Hex hex;
    @Nullable
    private String hexAssetId;
    private float manaOverride = -1f;
    private float manaMultiplier = 1.0f;
    private float volatilityOverride = -1f;
    private float volatilityMultiplier = 1.0f;
    private float powerOverride = -1f;
    private float powerMultiplier = 1.0f;
    @Nullable
    private HexColors colors;

    public ImbuementData() {
    }

    // copy / clone

    public ImbuementData copy() {
        ImbuementData copy = new ImbuementData();
        copy.hexCompressedId = this.hexCompressedId;
        copy.hex = this.hex;
        copy.hexAssetId = this.hexAssetId;
        copy.manaOverride = this.manaOverride;
        copy.manaMultiplier = this.manaMultiplier;
        copy.volatilityOverride = this.volatilityOverride;
        copy.volatilityMultiplier = this.volatilityMultiplier;
        copy.powerOverride = this.powerOverride;
        copy.powerMultiplier = this.powerMultiplier;
        copy.colors = this.colors;
        return copy;
    }

    @Override
    public ImbuementData clone() {
        return copy();
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

    public void setHexFromValue(@Nullable Hex hex) {
        if (hex == null) {
            this.hexCompressedId = null;
            this.hex = null;
        } else {
            this.hexCompressedId = HexCodec.serializeImbue(hex);
            this.hex = null;
        }
    }

    @Nullable
    public Hex resolveHex(HexCacheResource cache) {
        if (hexCompressedId != null) {
            Hex resolved = cache.getOrDecode(hexCompressedId);
            if (resolved != null) return resolved;
        }
        return hex;
    }

    @Nullable
    public String getHexAssetId() {
        return hexAssetId;
    }

    public void setHexAssetId(@Nullable String hexAssetId) {
        this.hexAssetId = hexAssetId;
    }

    public float getManaOverride() {
        return manaOverride;
    }

    public void setManaOverride(float manaOverride) {
        this.manaOverride = manaOverride;
    }

    public float getManaMultiplier() {
        return manaMultiplier;
    }

    public void setManaMultiplier(float manaMultiplier) {
        this.manaMultiplier = manaMultiplier;
    }

    public float getVolatilityOverride() {
        return volatilityOverride;
    }

    public void setVolatilityOverride(float volatilityOverride) {
        this.volatilityOverride = volatilityOverride;
    }

    public float getVolatilityMultiplier() {
        return volatilityMultiplier;
    }

    public void setVolatilityMultiplier(float volatilityMultiplier) {
        this.volatilityMultiplier = volatilityMultiplier;
    }

    public float getPowerOverride() {
        return powerOverride;
    }

    public void setPowerOverride(float powerOverride) {
        this.powerOverride = powerOverride;
    }

    public float getPowerMultiplier() {
        return powerMultiplier;
    }

    public void setPowerMultiplier(float powerMultiplier) {
        this.powerMultiplier = powerMultiplier;
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
            .append(new KeyedCodec<>("ManaOverride", Codec.FLOAT),
                    (c, v) -> c.manaOverride = v,
                    c -> c.manaOverride)
            .add()
            .append(new KeyedCodec<>("ManaMultiplier", Codec.FLOAT),
                    (c, v) -> c.manaMultiplier = v,
                    c -> c.manaMultiplier)
            .add()
            .append(new KeyedCodec<>("VolatilityOverride", Codec.FLOAT),
                    (c, v) -> c.volatilityOverride = v,
                    c -> c.volatilityOverride)
            .add()
            .append(new KeyedCodec<>("VolatilityMultiplier", Codec.FLOAT),
                    (c, v) -> c.volatilityMultiplier = v,
                    c -> c.volatilityMultiplier)
            .add()
            .append(new KeyedCodec<>("PowerOverride", Codec.FLOAT),
                    (c, v) -> c.powerOverride = v,
                    c -> c.powerOverride)
            .add()
            .append(new KeyedCodec<>("PowerMultiplier", Codec.FLOAT),
                    (c, v) -> c.powerMultiplier = v,
                    c -> c.powerMultiplier)
            .add()
            .append(new KeyedCodec<>("Colors", HexColors.CODEC),
                    (c, v) -> c.colors = v,
                    c -> c.colors)
            .add()
            .build();
}
