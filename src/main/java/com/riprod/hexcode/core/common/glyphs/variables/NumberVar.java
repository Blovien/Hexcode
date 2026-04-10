package com.riprod.hexcode.core.common.glyphs.variables;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class NumberVar extends HexVar {
    private double number;

    public NumberVar() {
    }

    public NumberVar(double number) {
        this.number = number;
    }

    public double getValue() {
        return number;
    }

    @Override
    public Object getRawValue() {
        return number;
    }

    @Override
    public double toScalar() {
        return number;
    }

    @Override
    public String describe() {
        return "NumberVar: " + number;
    }

    @Override
    public boolean equalTo(HexVar other) {
        if (other instanceof NumberVar nb) {
            return Double.compare(this.number, nb.number) == 0;
        }
        return super.equalTo(other);
    }

    @Override
    public int compareTo(HexVar other) {
        if (other instanceof NumberVar nb) {
            return Double.compare(this.number, nb.number);
        }
        return super.compareTo(other);
    }

    public static final BuilderCodec<NumberVar> CODEC = BuilderCodec
            .builder(NumberVar.class, NumberVar::new, HexVar.BASE_CODEC)
            .append(new KeyedCodec<>("Value", Codec.DOUBLE),
                    (v, num) -> v.number = num,
                    v -> v.number)
            .add()
            .build();

    static {
        HexVar.CODEC.register("Number", NumberVar.class, NumberVar.CODEC);
    }
}
