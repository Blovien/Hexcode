package com.riprod.hexcode.builtin.glyphs.value;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.riprod.hexcode.core.common.glyphs.values.HexValInterface;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class NumberValue implements HexValInterface {

    private int number;

    public NumberValue() {

    }

    public NumberValue(int number) {
        this.number = number;
    }

    @Override
    public HexVar getValue(HexContext hexContext) {
        return new NumberVar(this.number);
    }

    public static final BuilderCodec<NumberValue> CODEC = BuilderCodec
            .builder(NumberValue.class, NumberValue::new, HexValInterface.BASE_CODEC)
            .append(new KeyedCodec<>("Number", Codec.INTEGER),
                    (v, num) -> v.number = num,
                    v -> v.number)
            .add()
            .build();

    static {
        HexValInterface.CODEC.register("Number", NumberValue.class, NumberValue.CODEC);
    }
}
