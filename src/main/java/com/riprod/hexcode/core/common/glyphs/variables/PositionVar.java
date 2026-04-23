package com.riprod.hexcode.core.common.glyphs.variables;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import com.hypixel.hytale.math.vector.Vector3d;

public class PositionVar extends HexVar {
    private Vector3d position;
    private boolean absolute;

    public PositionVar() {
    }

    public PositionVar(Vector3d position) {
        this.position = position;
    }

    public PositionVar(Vector3d position, boolean absolute) {
        this.position = position;
        this.absolute = absolute;
    }

    public boolean isAbsolute() {
        return absolute;
    }

    public Vector3d getValue() {
        return position;
    }

    @Override
    public Object getRawValue() {
        return position;
    }

    @Override
    public Double toScalar() {
        return position == null ? 0 : position.length();
    }

    @Override
    public String describe() {
        if (position == null) return "PositionVar: [null]";
        return String.format("PositionVar: (%.2f, %.2f, %.2f) [%s]",
                position.x, position.y, position.z, absolute ? "abs" : "rel");
    }

    @Override
    public boolean equalTo(HexVar other) {
        if (other instanceof PositionVar pb) {
            if (position == null || pb.position == null) return position == pb.position;
            return position.equals(pb.position);
        }
        return super.equalTo(other);
    }

    @Override
    public int compareTo(HexVar other) {
        if (other instanceof PositionVar pb) {
            if (position == null && pb.position == null) return 0;
            if (position == null) return -1;
            if (pb.position == null) return 1;
            return Double.compare(position.length(), pb.position.length());
        }
        return super.compareTo(other);
    }

    @Override
    public String toString() {
        return "PositionVar(" + position + ", absolute=" + absolute + ")";
    }

    public static final BuilderCodec<PositionVar> CODEC = BuilderCodec
            .builder(PositionVar.class, PositionVar::new, HexVar.BASE_CODEC)
            .append(new KeyedCodec<>("Position", com.hypixel.hytale.math.vector.Vector3d.CODEC),
                    (v, pos) -> v.position = new Vector3d(pos.x, pos.y, pos.z),
                    v -> new com.hypixel.hytale.math.vector.Vector3d(v.position.x, v.position.y, v.position.z))
            .add()
            .append(new KeyedCodec<>("Absolute", Codec.BOOLEAN),
                    (v, abs) -> v.absolute = abs,
                    v -> v.absolute)
            .add()
            .build();

    static {
        HexVar.CODEC.register("Position", PositionVar.class, PositionVar.CODEC);
    }
}
