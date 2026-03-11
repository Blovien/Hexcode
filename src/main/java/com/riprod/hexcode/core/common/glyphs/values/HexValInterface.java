package com.riprod.hexcode.core.common.glyphs.values;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public abstract interface HexValInterface {
    public static final CodecMapCodec<HexValInterface> CODEC = new CodecMapCodec<>("Type");
    public static final BuilderCodec<HexValInterface> BASE_CODEC = BuilderCodec.abstractBuilder(HexValInterface.class).build();

    abstract public HexVar getValue(HexContext hexContext);
}
