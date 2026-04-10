package com.riprod.hexcode.core.common.glyphs.variables;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;

public abstract class HexVar {
    public static final CodecMapCodec<HexVar> CODEC = new CodecMapCodec<>("Type");
    public static final BuilderCodec<HexVar> BASE_CODEC = BuilderCodec.abstractBuilder(HexVar.class).build();

    public abstract Object getRawValue();

    public abstract double toScalar();

    public abstract String describe();

    public boolean equalTo(HexVar other) {
        if (other == null) return false;
        return Double.compare(this.toScalar(), other.toScalar()) == 0;
    }

    public int compareTo(HexVar other) {
        if (other == null) return 1;
        return Double.compare(this.toScalar(), other.toScalar());
    }
}
