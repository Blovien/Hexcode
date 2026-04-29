package com.riprod.hexcode.core.common.glyphs.registry;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.BuilderCodecMapCodec;

public abstract class GlyphConfig {

    public static final BuilderCodecMapCodec<GlyphConfig> CODEC = new BuilderCodecMapCodec<>("GlyphId", false);

    public static final BuilderCodec<GlyphConfig> BASE_CODEC = BuilderCodec
            .abstractBuilder(GlyphConfig.class)
            .appendInherited(new KeyedCodec<>("GlyphId", Codec.STRING),
                    (s, v) -> s.glyphId = v, s -> s.glyphId,
                    (child, parent) -> child.glyphId = parent.glyphId)
            .add()
            .build();

    protected String glyphId = "";

    public String getGlyphId() {
        return glyphId;
    }

    public static class Default extends GlyphConfig {
        public static final BuilderCodec<Default> CODEC = BuilderCodec
                .builder(Default.class, Default::new, BASE_CODEC)
                .build();
    }
}