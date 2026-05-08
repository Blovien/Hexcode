package com.riprod.hexcode.core.common.imbuement.codec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.bson.BsonDocument;
import org.bson.BsonValue;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.riprod.hexcode.core.common.imbuement.component.ImbuementData;

public final class ImbuementMapCodec implements Codec<Map<String, ImbuementData>> {

    public static final ImbuementMapCodec INSTANCE = new ImbuementMapCodec();
    public static final String LEGACY_DEFAULT_KEY = "Default";

    private static final MapCodec<ImbuementData, HashMap<String, ImbuementData>> DELEGATE =
            new MapCodec<>(ImbuementData.CODEC, HashMap::new, false);

    private ImbuementMapCodec() {
    }

    // live source of truth: presence of any current ImbuementData top-level field
    // indicates the BSON doc is a legacy single-ImbuementData shape, not a slot map
    private static Set<String> codecFieldNames() {
        return ImbuementData.CODEC.getEntries().keySet();
    }

    @Override
    public Map<String, ImbuementData> decode(BsonValue bsonValue, ExtraInfo extraInfo) {
        if (bsonValue == null || bsonValue.isNull()) return new HashMap<>();
        BsonDocument doc = bsonValue.asDocument();
        if (looksLikeLegacy(doc)) {
            ImbuementData data = ImbuementData.CODEC.decode(doc, extraInfo);
            HashMap<String, ImbuementData> map = new HashMap<>();
            if (data != null) map.put(LEGACY_DEFAULT_KEY, data);
            return map;
        }
        return DELEGATE.decode(bsonValue, extraInfo);
    }

    @Override
    public BsonValue encode(Map<String, ImbuementData> value, ExtraInfo extraInfo) {
        if (value == null) return new BsonDocument();
        return DELEGATE.encode(value, extraInfo);
    }

    @Override
    public Map<String, ImbuementData> decodeJson(@Nonnull RawJsonReader reader, ExtraInfo extraInfo)
            throws IOException {
        return DELEGATE.decodeJson(reader, extraInfo);
    }

    @Nonnull
    @Override
    public Schema toSchema(@Nonnull SchemaContext context) {
        return DELEGATE.toSchema(context);
    }

    private static boolean looksLikeLegacy(BsonDocument doc) {
        if (doc.isEmpty()) return false;
        Set<String> fields = codecFieldNames();
        // slot-shape guard: if any value is a sub-document carrying an ImbuementData
        // field, treat the outer doc as a slot map even if a slot key happens to
        // collide with a field name
        for (BsonValue value : doc.values()) {
            if (value != null && value.isDocument()) {
                BsonDocument nested = value.asDocument();
                for (String field : fields) {
                    if (nested.containsKey(field)) return false;
                }
            }
        }
        for (Map.Entry<String, BsonValue> entry : doc.entrySet()) {
            if (fields.contains(entry.getKey())) return true;
        }
        return false;
    }
}
