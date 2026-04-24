package com.riprod.hexcode.core.common.construct.state;

public final class NoState implements ConstructState {
    public static final NoState INSTANCE = new NoState();

    private NoState() {
    }

    @Override
    public NoState copy() {
        return this;
    }
}
