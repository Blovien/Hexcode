package com.riprod.hexcode.core.glyphs.variables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.math.vector.Vector3i;

public class BlockVar extends HexVar {
    private List<Vector3i> positions = new ArrayList<>();

    public BlockVar() {
    }

    public BlockVar(List<Vector3i> positions) {
        this.positions = positions;
    }

    public BlockVar(Vector3i position) {
        this.positions = new ArrayList<>(List.of(position));
    }

    public List<Vector3i> getValues() {
        return positions;
    }

    public void setPositions(List<Vector3i> positions) {
        this.positions = positions;
    }

    public Vector3i getAt(int index) {
        return positions.get(index);
    }

    public void addPosition(Vector3i position) {
        this.positions.add(position);
    }

    public void removePosition(Vector3i position) {
        this.positions.remove(position);
    }

    @Override
    public int size() {
        return positions.size();
    }

    public static final BuilderCodec<BlockVar> CODEC = BuilderCodec
            .builder(BlockVar.class, BlockVar::new, HexVar.BASE_CODEC)
            .append(new KeyedCodec<>("Positions", new ArrayCodec<>(Vector3i.CODEC, Vector3i[]::new)),
                    (v, pos) -> v.positions = new ArrayList<>(Arrays.asList(pos)),
                    v -> v.positions.toArray(Vector3i[]::new))
            .add()
            .build();

    static {
        HexVar.CODEC.register("Block", BlockVar.class, BlockVar.CODEC);
    }
}
