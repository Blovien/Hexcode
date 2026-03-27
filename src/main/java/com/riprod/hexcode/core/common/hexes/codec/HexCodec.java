package com.riprod.hexcode.core.common.hexes.codec;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.annotation.Nullable;

import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.utils.HexSerializer;
import com.riprod.hexcode.core.common.hexes.utils.HexUtils;

public class HexCodec {

    private static final String NEW_PREFIX = "hx:";
    private static final String LEGACY_PREFIX = "hexcode:";

    @Nullable
    public static String serialize(Hex hex) {
        Hex clone = hex.clone();
        HexUtils.validate(clone);
        HexUtils.compress(clone);

        String reason = CodecUtil.exceedsV4Limits(clone);
        String version;
        byte[] raw;
        if (reason == null) {
            version = "4";
            raw = HexCodecV4.encode(clone);
        } else {
            version = "5";
            raw = HexCodecV5.encode(clone);
        }
        if (raw == null) return null;

        byte[] compressed = deflate(raw);
        byte[] payload = (compressed != null && compressed.length < raw.length) ? compressed : raw;

        String encoded = Base64.getEncoder().withoutPadding().encodeToString(payload);
        String checksum = computeChecksum(encoded);
        return NEW_PREFIX + checksum + ":" + version + ":" + encoded;
    }

    public static DecodeResult deserialize(String data) {
        if (data == null) return DecodeResult.error("null input");

        if (data.startsWith(LEGACY_PREFIX)) {
            Hex hex = HexSerializer.deserialize(data);
            if (hex == null) return DecodeResult.error("legacy decode failed");
            return new DecodeResult(hex, List.of(
                    new DecodeIssue("decoded from legacy format", DecodeIssue.Severity.INFO)));
        }

        if (!data.startsWith(NEW_PREFIX))
            return DecodeResult.error("invalid prefix");

        String remainder = data.substring(NEW_PREFIX.length());
        String[] parts = remainder.split(":", 3);
        if (parts.length < 3)
            return DecodeResult.error("malformed string: expected hx:checksum:version:payload");

        String checksum = parts[0];
        String version = parts[1];
        String encoded = parts[2];

        if (!checksum.equals(computeChecksum(encoded)))
            return DecodeResult.error("checksum mismatch");

        byte[] payload;
        try {
            int pad = (4 - encoded.length() % 4) % 4;
            payload = Base64.getDecoder().decode(encoded + "=".repeat(pad));
        } catch (IllegalArgumentException e) {
            return DecodeResult.error("invalid base64");
        }

        byte[] raw = inflate(payload);
        if (raw == null) raw = payload;

        DecodeResult result = switch (version) {
            case "4" -> HexCodecV4.decode(raw);
            case "5" -> HexCodecV5.decode(raw);
            default -> DecodeResult.error("unknown version '" + version + "'");
        };

        if (result.getHex() != null) {
            HexUtils.repair(result.getHex());
        }

        return result;
    }

    @Nullable
    private static byte[] deflate(byte[] data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DeflaterOutputStream dos = new DeflaterOutputStream(baos,
                     new Deflater(Deflater.BEST_COMPRESSION))) {
            dos.write(data);
            dos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static byte[] inflate(byte[] data) {
        try (InflaterInputStream iis = new InflaterInputStream(
                new java.io.ByteArrayInputStream(data))) {
            return iis.readAllBytes();
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
