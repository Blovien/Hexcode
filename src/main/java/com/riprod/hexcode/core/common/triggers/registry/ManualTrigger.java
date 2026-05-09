package com.riprod.hexcode.core.common.triggers.registry;

import javax.annotation.Nonnull;

// reusable Source.MANUAL trigger for slot keys whose hex is read directly
// from the imbuement metadata map (spellbook pages, block activation).
// holds the key only — there's no dispatch behavior because no event source
// fires it.
public final class ManualTrigger implements Trigger {

    private final String id;

    public ManualTrigger(@Nonnull String id) {
        this.id = id;
    }

    @Override public String getId() { return id; }
    @Override public Source getSource() { return Source.MANUAL; }
    @Override public DefaultVariableKind getDefaultVariable() { return DefaultVariableKind.NONE; }
}
