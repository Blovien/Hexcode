package com.riprod.hexcode.builtin.glyphs.effect.beam;

public enum BeamSlots {
    Source("source"),
    Direction("direction"),
    MaxDistance("maxDistance"),
    HitResult("hitResult");

    private final String id;

    BeamSlots(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
