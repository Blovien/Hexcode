package com.riprod.hexcode.core.glyphs.component.context;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;

/**
 * The base spell context object that can hold variables, values, and other base types of data
 */
public abstract class HexCtx {
    public static final CodecMapCodec<HexCtx> CODEC = new CodecMapCodec<>("Type");
    public static final BuilderCodec<HexCtx> BASE_CODEC = BuilderCodec.abstractBuilder(HexCtx.class).build();
}