package com.riprod.hexcode.core.glyphs.variables;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class NumberVar extends SpellVar {
    public double number;

    public NumberVar() {
    }

    public NumberVar(double number) {
        this.number = number;
    }

    public static final BuilderCodec<NumberVar> CODEC = BuilderCodec
            .builder(NumberVar.class, NumberVar::new, SpellVar.BASE_CODEC)
            .append(new KeyedCodec<>("Number", Codec.DOUBLE),
                    (v, num) -> v.number = num,
                    v -> v.number)
            .add()
            .build();

    static {
        SpellVar.CODEC.register("Number", NumberVar.class, NumberVar.CODEC);
    }
}