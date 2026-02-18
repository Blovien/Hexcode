package com.riprod.hexcode.core.glyphs.variables;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3f;

public class RotationVar extends SpellVar {
    public Vector3f rotation;

    public RotationVar() {
    }

    public RotationVar(Vector3f rotation) {
        this.rotation = rotation;
    }

    public static final BuilderCodec<RotationVar> CODEC = BuilderCodec
            .builder(RotationVar.class, RotationVar::new, SpellVar.BASE_CODEC)
            .append(new KeyedCodec<>("Rotation", Vector3f.CODEC),
                    (v, rot) -> v.rotation = rot,
                    v -> v.rotation)
            .add()
            .build();

    static {
        SpellVar.CODEC.register("Rotation", RotationVar.class, RotationVar.CODEC);
    }
}