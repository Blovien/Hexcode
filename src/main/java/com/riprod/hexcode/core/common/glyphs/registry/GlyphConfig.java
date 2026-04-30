package com.riprod.hexcode.core.common.glyphs.registry;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.Priority;
import com.hypixel.hytale.codec.schema.metadata.ui.UIDisplayMode;

public abstract class GlyphConfig {

    public static final GlyphConfigMapCodec CODEC = new GlyphConfigMapCodec();

    public static final BuilderCodec<GlyphConfig> BASE_CODEC = BuilderCodec
            .abstractBuilder(GlyphConfig.class)
            .appendInherited(new KeyedCodec<>("GlyphId", Codec.STRING),
                    (s, v) -> s.glyphId = v, s -> s.glyphId,
                    (child, parent) -> child.glyphId = parent.glyphId)
            .metadata(UIDisplayMode.HIDDEN)
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

    static {
        CODEC.register(Priority.DEFAULT, "", Default.class, Default.CODEC);
    }
}