package com.riprod.hexcode.core.common.glyphs.values;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public abstract interface HexVal {
    public static final CodecMapCodec<HexVal> CODEC = new CodecMapCodec<>("Type");
    public static final BuilderCodec<HexVal> BASE_CODEC = BuilderCodec.abstractBuilder(HexVal.class).build();

    abstract public HexVar getValue(HexContext hexContext);
}
