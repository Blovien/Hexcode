package com.riprod.hexcode.core.glyphs.variables;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3i;

public class BlockVar extends SpellVar {
    public Vector3i position;

    public BlockVar() {
    }

    public BlockVar(Vector3i position) {
        this.position = position;
    }

    public static final BuilderCodec<BlockVar> CODEC = BuilderCodec
            .builder(BlockVar.class, BlockVar::new, SpellVar.BASE_CODEC)
            .append(new KeyedCodec<>("Position", Vector3i.CODEC),
                    (v, pos) -> v.position = pos,
                    v -> v.position)
            .add()
            .build();

    static {
        SpellVar.CODEC.register("Block", BlockVar.class, BlockVar.CODEC);
    }
}