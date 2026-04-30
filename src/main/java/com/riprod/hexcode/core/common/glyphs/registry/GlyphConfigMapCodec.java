package com.riprod.hexcode.core.common.glyphs.registry;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.bson.BsonDocument;
import org.bson.BsonValue;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.StringCodecMapCodec;
import com.hypixel.hytale.codec.util.RawJsonReader;

public class GlyphConfigMapCodec extends StringCodecMapCodec<GlyphConfig, BuilderCodec<? extends GlyphConfig>> {

    public GlyphConfigMapCodec() {
        super("GlyphId", true, false);
    }

    @Override
    public GlyphConfig decodeJson(@Nonnull RawJsonReader reader, @Nonnull ExtraInfo extraInfo) throws IOException {
        BuilderCodec<? extends GlyphConfig> codec = lookupByAssetKey(extraInfo);
        if (codec != null) {
            return codec.decodeJson(reader, extraInfo);
        }
        return super.decodeJson(reader, extraInfo);
    }

    @Override
    public GlyphConfig decode(@Nonnull BsonValue bsonValue, ExtraInfo extraInfo) {
        BuilderCodec<? extends GlyphConfig> codec = lookupByAssetKey(extraInfo);
        if (codec != null) {
            return codec.decode(bsonValue.asDocument(), extraInfo);
        }
        return super.decode(bsonValue, extraInfo);
    }

    @Override
    public GlyphConfig decodeAndInherit(@Nonnull BsonDocument document, GlyphConfig parent, ExtraInfo extraInfo) {
        BuilderCodec<? extends GlyphConfig> codec = lookupByAssetKey(extraInfo);
        if (codec != null) {
            return codec.decode(document, extraInfo);
        }
        return super.decodeAndInherit(document, parent, extraInfo);
    }

    private BuilderCodec<? extends GlyphConfig> lookupByAssetKey(ExtraInfo extraInfo) {
        if (!(extraInfo instanceof AssetExtraInfo<?> assetInfo)) return null;
        Object key = assetInfo.getKey();
        if (!(key instanceof String id) || id.isEmpty()) return null;
        return this.idToCodec.get(id);
    }
}
