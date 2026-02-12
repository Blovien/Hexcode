package com.riprod.hexcode.core.glyphs.variables;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3d;

public class PositionVar extends SpellVar {
    public Vector3d position;

    public PositionVar() {
    }

    public PositionVar(Vector3d position) {
        this.position = position;
    }

    public static final BuilderCodec<PositionVar> CODEC = BuilderCodec
            .builder(PositionVar.class, PositionVar::new, SpellVar.BASE_CODEC)
            .append(new KeyedCodec<>("Position", Vector3d.CODEC),
                    (v, pos) -> v.position = pos,
                    v -> v.position)
            .add()
            .build();

    static {
        SpellVar.CODEC.register("Position", PositionVar.class, PositionVar.CODEC);
    }
}