package com.riprod.hexcode.core.glyphs.variables;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;

public abstract class SpellVar {
    public static final CodecMapCodec<SpellVar> CODEC = new CodecMapCodec<>("Type");
    public static final BuilderCodec<SpellVar> BASE_CODEC = BuilderCodec.abstractBuilder(SpellVar.class).build();
}