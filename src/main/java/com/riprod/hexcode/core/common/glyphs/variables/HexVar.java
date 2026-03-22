package com.riprod.hexcode.core.common.glyphs.variables;

import java.util.Collections;
import java.util.List;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;

public abstract class HexVar {
    public static final CodecMapCodec<HexVar> CODEC = new CodecMapCodec<>("Type");
    public static final BuilderCodec<HexVar> BASE_CODEC = BuilderCodec.abstractBuilder(HexVar.class).build();

    public abstract List<?> getValues();
    public abstract int size();

    public double toScalar() {
        return size();
    }

    public boolean equalTo(HexVar other) {
        if (other == null) return false;
        return this.size() == other.size();
    }

    public int compareTo(HexVar other) {
        if (other == null) return 1;
        return Double.compare(this.toScalar(), other.toScalar());
    }

    public void shuffleAndTrim(int count) {
        List<?> values = getValues();
        Collections.shuffle(values);
        int trimmed = Math.min(count, values.size());
        values.subList(trimmed, values.size()).clear();
    }
} 