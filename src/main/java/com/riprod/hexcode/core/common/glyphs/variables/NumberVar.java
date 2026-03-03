package com.riprod.hexcode.core.common.glyphs.variables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

public class NumberVar extends HexVar {
    private List<Double> numbers = new ArrayList<>();

    public NumberVar() {
    }

    public NumberVar(List<Double> numbers) {
        this.numbers = numbers;
    }

    public NumberVar(double number) {
        this.numbers = new ArrayList<>(List.of(number));
    }

    public List<Double> getValues() {
        return numbers;
    }

    public void setNumbers(List<Double> numbers) {
        this.numbers = numbers;
    }

    public void addNumber(double number) {
        this.numbers.add(number);
    }

    public void removeAt(int index) {
        this.numbers.remove(index);
    }

    public double getAt(int index) {
        return this.numbers.get(index);
    }

    @Override
    public int size() {
        return numbers.size();
    }

    public static final BuilderCodec<NumberVar> CODEC = BuilderCodec
            .builder(NumberVar.class, NumberVar::new, HexVar.BASE_CODEC)
            .append(
                new KeyedCodec<>("Numbers", new ArrayCodec<>(Codec.DOUBLE, Double[]::new)),
                (v, nums) -> v.numbers = new ArrayList<>(Arrays.asList(nums)),
                v -> v.numbers.toArray(Double[]::new)
            )
            .add()
            .build();

    static {
        HexVar.CODEC.register("Number", NumberVar.class, NumberVar.CODEC);
    }
}
