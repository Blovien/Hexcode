package com.riprod.hexcode.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

public class Glyph {
    private String glyphId;
    private float accuracy;
    private float speed;
    private HashMap<Integer, Integer> numbers = new HashMap<>();
    private HashMap<Integer, Integer> variables = new HashMap<>();
    private List<UUID> next = new ArrayList<>();

    public Glyph() {
    }

    public String getGlyphId() {
        return glyphId;
    }

    public void setGlyphId(String glyphId) {
        this.glyphId = glyphId;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public int getNumber(int key) {
        return numbers.getOrDefault(key, key);
    }

    public void setNumber(int key, int value) {
        numbers.put(key, value);
    }

    public HashMap<Integer, Integer> getNumbers() {
        return numbers;
    }

    /**
     * Gets the variable slot for a given key. Use these to access the Variables
     * inside the ExecutionContext. Defaults to the key itself if not set.
     * 
     * @param key
     * @return
     */
    public int getVariable(int key) {
        return variables.getOrDefault(key, key);
    }

    public void setVariable(int key, int value) {
        variables.put(key, value);
    }

    public HashMap<Integer, Integer> getVariables() {
        return variables;
    }

    public List<UUID> getNext() {
        return next;
    }

    public void setNext(List<UUID> next) {
        this.next = next;
    }

    private static final ArrayCodec<UUID> UUID_ARRAY_CODEC = new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new);

    private static final MapCodec<Integer, Map<String, Integer>> INT_MAP_CODEC = new MapCodec<>(Codec.INTEGER,
            HashMap::new, false);

    public static final BuilderCodec<Glyph> CODEC = BuilderCodec
            .builder(Glyph.class, Glyph::new)
            .append(new KeyedCodec<>("GlyphId", Codec.STRING),
                    (n, v) -> n.glyphId = v, n -> n.glyphId)
            .add()
            .append(new KeyedCodec<>("Accuracy", Codec.FLOAT),
                    (n, v) -> n.accuracy = v, n -> n.accuracy)
            .add()
            .append(new KeyedCodec<>("Speed", Codec.FLOAT),
                    (n, v) -> n.speed = v, n -> n.speed)
            .add()
            .append(new KeyedCodec<>("Numbers", INT_MAP_CODEC),
                    (n, v) -> {
                        n.numbers = new HashMap<>();
                        for (Map.Entry<String, Integer> e : v.entrySet())
                            n.numbers.put(Integer.parseInt(e.getKey()), e.getValue());
                    },
                    n -> {
                        Map<String, Integer> map = new HashMap<>();
                        for (Map.Entry<Integer, Integer> e : n.numbers.entrySet())
                            map.put(e.getKey().toString(), e.getValue());
                        return map;
                    })
            .add()
            .append(new KeyedCodec<>("Variables", INT_MAP_CODEC),
                    (n, v) -> {
                        n.variables = new HashMap<>();
                        for (Map.Entry<String, Integer> e : v.entrySet())
                            n.variables.put(Integer.parseInt(e.getKey()), e.getValue());
                    },
                    n -> {
                        Map<String, Integer> map = new HashMap<>();
                        for (Map.Entry<Integer, Integer> e : n.variables.entrySet())
                            map.put(e.getKey().toString(), e.getValue());
                        return map;
                    })
            .add()
            .append(new KeyedCodec<>("Next", UUID_ARRAY_CODEC),
                    (n, v) -> n.next = v != null
                            ? new ArrayList<>(Arrays.asList(v))
                            : new ArrayList<>(),
                    n -> n.next.toArray(UUID[]::new))
            .add()
            .build();
}
