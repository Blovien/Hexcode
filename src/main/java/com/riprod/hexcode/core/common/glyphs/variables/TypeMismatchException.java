package com.riprod.hexcode.core.common.glyphs.variables;

public class TypeMismatchException extends RuntimeException {
    public TypeMismatchException(String op, HexVar a, HexVar b) {
        super(String.format("%s: unsupported types %s and %s",
                op,
                a == null ? "null" : a.getClass().getSimpleName(),
                b == null ? "null" : b.getClass().getSimpleName()));
    }
}
