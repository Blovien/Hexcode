package com.riprod.hexcode.core.glyphs.values;

import java.util.List;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.glyphs.variables.HexVar;

public abstract interface HexVal {
    public static final CodecMapCodec<HexVal> CODEC = new CodecMapCodec<>("Type");
    public static final BuilderCodec<HexVal> BASE_CODEC = BuilderCodec.abstractBuilder(HexVal.class).build();

    abstract public HexVar getValue(ExecutionContext context, HexContext hexContext);
}
