package com.riprod.hexcode.core.glyphs.variables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.math.vector.Vector3f;

public class RotationVar extends HexVar {
    private List<Vector3f> rotations = new ArrayList<>();

    public RotationVar() {
    }

    public RotationVar(List<Vector3f> rotations) {
        this.rotations = rotations;
    }

    public RotationVar(Vector3f singleRotation) {
        this.rotations = new ArrayList<>();
        this.rotations.add(singleRotation);
    }

    public List<Vector3f> getValues() {
        return rotations;
    }

    public void setRotations(List<Vector3f> rotations) {
        this.rotations = rotations;
    }

    public Vector3f getAt(int index) {
        return rotations.get(index);
    }

    public void addRotation(Vector3f rotation) {
        this.rotations.add(rotation);
    }

    public void removeRotation(Vector3f rotation) {
        this.rotations.remove(rotation);
    }

    @Override
    public int size() {
        return rotations.size();
    }

    public static final BuilderCodec<RotationVar> CODEC = BuilderCodec
            .builder(RotationVar.class, RotationVar::new, HexVar.BASE_CODEC)
            .append(new KeyedCodec<>("Rotations", new ArrayCodec<>(Vector3f.CODEC, Vector3f[]::new)),
                    (v, rots) -> v.rotations = new ArrayList<>(Arrays.asList(rots)),
                    v -> v.rotations.toArray(Vector3f[]::new))
            .add()
            .build();

    static {
        HexVar.CODEC.register("Rotation", RotationVar.class, RotationVar.CODEC);
    }
}
