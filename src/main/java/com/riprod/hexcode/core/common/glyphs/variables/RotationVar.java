package com.riprod.hexcode.core.common.glyphs.variables;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import com.hypixel.hytale.math.vector.Vector3f;

public class RotationVar extends HexVar {
    private Vector3f rotation;

    public RotationVar() {
    }

    public RotationVar(Vector3f rotation) {
        this.rotation = rotation;
    }

    public Vector3f getValue() {
        return rotation;
    }

    @Override
    public Object getRawValue() {
        return rotation;
    }

    @Override
    public Double toScalar() {
        return rotation == null ? 0.0 : rotation.length();
    }

    @Override
    public String describe() {
        if (rotation == null)
            return "RotationVar: [null]";
        return String.format("RotationVar: pitch=%.1f, yaw=%.1f, roll=%.1f",
                rotation.x, rotation.y, rotation.z);
    }

    @Override
    public boolean equalTo(HexVar other) {
        if (other instanceof RotationVar rb) {
            if (rotation == null || rb.rotation == null)
                return rotation == rb.rotation;
            return rotation.equals(rb.rotation);
        }
        return super.equalTo(other);
    }

    @Override
    public int compareTo(HexVar other) {
        if (other instanceof RotationVar rb) {
            if (rotation == null && rb.rotation == null)
                return 0;
            if (rotation == null)
                return -1;
            if (rb.rotation == null)
                return 1;
            return Double.compare(rotation.length(), rb.rotation.length());
        }
        return super.compareTo(other);
    }

    @Override
    public String toString() {
        return "RotationVar(" + rotation + ")";
    }

    public static final BuilderCodec<RotationVar> CODEC = BuilderCodec
            .builder(RotationVar.class, RotationVar::new, HexVar.BASE_CODEC)
            .append(new KeyedCodec<>("Rotation", com.hypixel.hytale.math.vector.Vector3f.CODEC),
                    (v, rot) -> v.rotation = new Vector3f(rot.x, rot.y, rot.z),
                    v -> new com.hypixel.hytale.math.vector.Vector3f(v.rotation.x, v.rotation.y, v.rotation.z))
            .add()
            .build();

    static {
        HexVar.CODEC.register("Rotation", RotationVar.class, RotationVar.CODEC);
    }
}
