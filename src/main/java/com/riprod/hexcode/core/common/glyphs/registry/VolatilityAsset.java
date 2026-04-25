package com.riprod.hexcode.core.common.glyphs.registry;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class VolatilityAsset {

    private float instantCost = 0.0f;
    // legacy default matching the old hardcoded dt*0.15f used in every construct handler
    private float drainPerSecond = 0.15f;
    private RepeatEscalation repeatEscalation = new RepeatEscalation();
    private AreaTax areaTax = null;

    public VolatilityAsset() {
    }

    public float getInstantCost() {
        return instantCost;
    }

    public float getDrainPerSecond() {
        return drainPerSecond;
    }

    public RepeatEscalation getRepeatEscalation() {
        return repeatEscalation;
    }

    public AreaTax getAreaTax() {
        return areaTax;
    }

    // asymptotic: cost approaches instantCost * (1 + base) as N grows.
    // base=1, k=5 -> 5 uses = +50%, 20 uses = +80%, ceiling = 2x.
    public float getCostForRepeat(int repeatCount) {
        if (repeatCount <= 0) {
            return instantCost;
        }
        float base = repeatEscalation.getBase();
        float k = repeatEscalation.getK();
        float scale = 1.0f + base * (repeatCount / (repeatCount + k));
        return instantCost * scale;
    }

    public float getTotalCostForDuration(float durationSeconds, int repeatCount) {
        return getCostForRepeat(repeatCount) + drainPerSecond * durationSeconds;
    }

    public static class RepeatEscalation {
        private float base = 1.0f;
        private float k = 5.0f;

        public RepeatEscalation() {
        }

        public float getBase() {
            return base;
        }

        public float getK() {
            return k;
        }

        public static final BuilderCodec<RepeatEscalation> CODEC = BuilderCodec
                .builder(RepeatEscalation.class, RepeatEscalation::new)
                .append(new KeyedCodec<>("Base", Codec.FLOAT),
                        (s, v) -> s.base = v, s -> s.base)
                .add()
                .append(new KeyedCodec<>("K", Codec.FLOAT),
                        (s, v) -> s.k = v, s -> s.k)
                .add()
                .build();
    }

    public static class AreaTax {
        private float defaultMagnitude = 1.0f;
        private float exponent = 1.0f;

        public AreaTax() {
        }

        public float getDefaultMagnitude() {
            return defaultMagnitude;
        }

        public float getExponent() {
            return exponent;
        }

        public static final BuilderCodec<AreaTax> CODEC = BuilderCodec
                .builder(AreaTax.class, AreaTax::new)
                .append(new KeyedCodec<>("DefaultMagnitude", Codec.FLOAT),
                        (s, v) -> s.defaultMagnitude = v, s -> s.defaultMagnitude)
                .add()
                .append(new KeyedCodec<>("Exponent", Codec.FLOAT),
                        (s, v) -> s.exponent = v, s -> s.exponent)
                .add()
                .build();
    }

    public static final BuilderCodec<VolatilityAsset> CODEC = BuilderCodec
            .builder(VolatilityAsset.class, VolatilityAsset::new)
            .append(new KeyedCodec<>("InstantCost", Codec.FLOAT),
                    (s, v) -> s.instantCost = v, s -> s.instantCost)
            .add()
            .append(new KeyedCodec<>("DrainPerSecond", Codec.FLOAT),
                    (s, v) -> s.drainPerSecond = v, s -> s.drainPerSecond)
            .add()
            .append(new KeyedCodec<>("RepeatEscalation", RepeatEscalation.CODEC),
                    (s, v) -> s.repeatEscalation = v, s -> s.repeatEscalation)
            .add()
            .append(new KeyedCodec<>("AreaTax", AreaTax.CODEC),
                    (s, v) -> s.areaTax = v, s -> s.areaTax)
            .add()
            .build();
}
