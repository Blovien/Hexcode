package com.riprod.hexcode.core.common.hexes.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.server.core.util.BsonUtil;
import com.riprod.hexcode.core.common.hexes.component.Hex;

import org.bson.BsonDocument;
import org.bson.BsonValue;

public class HexSerializer {

    private static final String PREFIX = "hexcode:";

    private static final List<String> STRIP_GLYPH_KEYS = List.of(
            "RelativePosition", "RelativeRotation", "Previous");

    public static String serialize(Hex hex) {
        Hex clone = hex.clone();
        HexUtils.validate(clone);
        HexUtils.compress(clone);

        BsonDocument doc = Hex.CODEC.encode(clone, ExtraInfo.THREAD_LOCAL.get());
        stripForExport(doc);

        byte[] bsonBytes = BsonUtil.writeToBytes(doc);
        byte[] compressed = gzip(bsonBytes);
        if (compressed == null) return null;

        String encoded = Base64.getEncoder().encodeToString(compressed);
        String checksum = computeChecksum(encoded);
        return PREFIX + checksum + ":" + encoded;
    }

    @Nullable
    public static Hex deserialize(String data) {
        if (data == null || !data.startsWith(PREFIX)) return null;

        String remainder = data.substring(PREFIX.length());
        int sep = remainder.indexOf(':');
        if (sep < 0) return null;

        String checksum = remainder.substring(0, sep);
        String encoded = remainder.substring(sep + 1);

        if (!checksum.equals(computeChecksum(encoded))) return null;

        byte[] compressed;
        try {
            compressed = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            return null;
        }

        byte[] bsonBytes = gunzip(compressed);
        if (bsonBytes == null) return null;

        BsonDocument doc = BsonUtil.readFromBytes(bsonBytes);
        if (doc == null) return null;

        try {
            Hex hex = Hex.CODEC.decode(doc, ExtraInfo.THREAD_LOCAL.get());
            hex.set(UUID.randomUUID().toString());
            return hex;
        } catch (Exception e) {
            return null;
        }
    }

    private static void stripForExport(BsonDocument doc) {
        doc.remove("HexId");

        BsonDocument hexGraph = doc.getDocument("HexGraph", null);
        if (hexGraph == null) return;

        for (String key : hexGraph.keySet()) {
            BsonValue val = hexGraph.get(key);
            if (!val.isDocument()) continue;
            BsonDocument glyph = val.asDocument();

            for (String stripKey : STRIP_GLYPH_KEYS) {
                glyph.remove(stripKey);
            }

            stripEmptyMap(glyph, "Inputs");
            stripEmptyMap(glyph, "Outputs");
            stripEmptyArray(glyph, "Next");
        }
    }

    private static void stripEmptyMap(BsonDocument doc, String key) {
        BsonValue val = doc.get(key);
        if (val != null && val.isDocument() && val.asDocument().isEmpty()) {
            doc.remove(key);
        }
    }

    private static void stripEmptyArray(BsonDocument doc, String key) {
        BsonValue val = doc.get(key);
        if (val != null && val.isArray() && val.asArray().isEmpty()) {
            doc.remove(key);
        }
    }

    @Nullable
    private static byte[] gzip(byte[] data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(data);
            gzos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static byte[] gunzip(byte[] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             GZIPInputStream gzis = new GZIPInputStream(bais)) {
            return gzis.readAllBytes();
        } catch (Exception e) {
            return null;
        }
    }

    private static String computeChecksum(String data) {
        CRC32 crc = new CRC32();
        crc.update(data.getBytes());
        return String.format("%08x", crc.getValue()).substring(0, 4);
    }
}
