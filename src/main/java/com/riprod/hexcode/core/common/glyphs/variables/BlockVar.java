package com.riprod.hexcode.core.common.glyphs.variables;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3i;

public class BlockVar extends HexVar {
    private Vector3i position;

    public BlockVar() {
    }

    public BlockVar(Vector3i position) {
        this.position = position;
    }

    public Vector3i getValue() {
        return position;
    }

    @Override
    public Object getRawValue() {
        return position;
    }

    @Override
    public double toScalar() {
        if (position == null) return 0;
        return Math.sqrt(position.x * position.x + position.y * position.y + position.z * position.z);
    }

    @Override
    public String describe() {
        if (position == null) return "BlockVar: [null]";
        return "BlockVar: (" + position.x + ", " + position.y + ", " + position.z + ")";
    }

    @Override
    public boolean equalTo(HexVar other) {
        if (other instanceof BlockVar bb) {
            if (position == null || bb.position == null) return position == bb.position;
            return position.equals(bb.position);
        }
        return super.equalTo(other);
    }

    @Override
    public int compareTo(HexVar other) {
        if (other instanceof BlockVar bb) {
            if (position == null && bb.position == null) return 0;
            if (position == null) return -1;
            if (bb.position == null) return 1;
            return Double.compare(this.toScalar(), bb.toScalar());
        }
        return super.compareTo(other);
    }

    public static final BuilderCodec<BlockVar> CODEC = BuilderCodec
            .builder(BlockVar.class, BlockVar::new, HexVar.BASE_CODEC)
            .append(new KeyedCodec<>("Position", Vector3i.CODEC),
                    (v, pos) -> v.position = pos,
                    v -> v.position)
            .add()
            .build();

    static {
        HexVar.CODEC.register("Block", BlockVar.class, BlockVar.CODEC);
    }
}
