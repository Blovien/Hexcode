package com.riprod.hexcode.core.common.hexes.codec;

public class BitReader {

    private final byte[] data;
    private int bytePos = 0;
    private int bitPos = 0;

    public BitReader(byte[] data) {
        this.data = data;
    }

    public int read(int numBits) {
        int value = 0;
        for (int i = 0; i < numBits; i++) {
            value = (value << 1) | ((data[bytePos] >> (7 - bitPos)) & 1);
            bitPos++;
            if (bitPos == 8) {
                bitPos = 0;
                bytePos++;
            }
        }
        return value;
    }

    public int readVarInt() {
        if (read(1) == 0) return read(3);
        if (read(1) == 0) return read(6) + 8;
        return read(12);
    }
}