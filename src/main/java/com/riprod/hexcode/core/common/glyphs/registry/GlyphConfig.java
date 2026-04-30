package com.riprod.hexcode.core.common.glyphs.registry;

import com.hypixel.hytale.codec.lookup.BuilderCodecMapCodec;

public interface GlyphConfig {

    public static final BuilderCodecMapCodec<GlyphConfig> CODEC = new BuilderCodecMapCodec<>("GlyphId", true);

}