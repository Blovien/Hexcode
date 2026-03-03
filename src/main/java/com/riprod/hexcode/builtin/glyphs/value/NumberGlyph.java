package com.riprod.hexcode.builtin.glyphs.value;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.riprod.hexcode.core.common.glyphs.values.HexVal;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class NumberGlyph implements HexVal {

    private int number;

    public NumberGlyph() {

    }

    public NumberGlyph(int number) {
        this.number = number;
    }

    @Override
    public HexVar getValue(HexContext hexContext) {
        return new NumberVar(this.number);
    }

    public static final BuilderCodec<NumberGlyph> CODEC = BuilderCodec
            .builder(NumberGlyph.class, NumberGlyph::new, HexVal.BASE_CODEC)
            .append(new KeyedCodec<>("Number", Codec.INTEGER),
                    (v, num) -> v.number = num,
                    v -> v.number)
            .add()
            .build();

    static {
        HexVal.CODEC.register("Number", NumberGlyph.class, NumberGlyph.CODEC);
    }
}
