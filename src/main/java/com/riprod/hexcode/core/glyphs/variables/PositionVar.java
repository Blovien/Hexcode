package com.riprod.hexcode.core.glyphs.variables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.math.vector.Vector3d;

public class PositionVar extends HexVar {
    private List<Vector3d> positions = new ArrayList<>();

    public PositionVar() {
    }

    public PositionVar(List<Vector3d> positions) {
        this.positions = positions;
    }

    public PositionVar(Vector3d position) {
        this.positions = new ArrayList<>();
        this.positions.add(position);
    }

    public List<Vector3d> getValues() {
        return positions;
    }

    public void setPositions(List<Vector3d> positions) {
        this.positions = positions;
    }

    public Vector3d getAt(int index) {
        return positions.get(index);
    }

    public void addPosition(Vector3d position) {
        this.positions.add(position);
    }

    public void removePosition(Vector3d position) {
        this.positions.remove(position);
    }

    @Override
    public int size() {
        return positions.size();
    }

    public static final BuilderCodec<PositionVar> CODEC = BuilderCodec
            .builder(PositionVar.class, PositionVar::new, HexVar.BASE_CODEC)
            .append(new KeyedCodec<>("Positions", new ArrayCodec<>(Vector3d.CODEC, Vector3d[]::new)),
                    (v, pos) -> v.positions = new ArrayList<>(Arrays.asList(pos)),
                    v -> v.positions.toArray(Vector3d[]::new))
            .add()
            .build();

    static {
        HexVar.CODEC.register("Position", PositionVar.class, PositionVar.CODEC);
    }
}
