package com.riprod.hexcode.core.common.triggers.registry;

import javax.annotation.Nonnull;

public final class ManualTrigger implements Trigger {

    private final String id;

    public ManualTrigger(@Nonnull String id) {
        this.id = id;
    }

    @Override public String getId() { return id; }
    @Override public Source getSource() { return Source.MANUAL; }
    @Override public DefaultVariableKind getDefaultVariable() { return DefaultVariableKind.NONE; }
}
