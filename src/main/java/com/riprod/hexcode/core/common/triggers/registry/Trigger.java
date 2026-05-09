package com.riprod.hexcode.core.common.triggers.registry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.triggers.component.TriggerEvent;

public interface Trigger {

    @Nonnull
    String getId();

    @Nonnull
    Source getSource();

    @Nonnull
    DefaultVariableKind getDefaultVariable();

    default boolean isProfileSlotEligible() {
        return true;
    }

    @Nullable
    default HexVar resolveDefaultVariable(@Nonnull TriggerEvent event) {
        return getDefaultVariable().from(event);
    }

    enum Source {
        // dispatched by an event source via TriggerListenerRegistry.fire, then
        // a CastRootDispatcher reads slot data and builds a cast root.
        ITEM_HELD,
        ITEM_EQUIPPED_ARMOR,
        ENTITY_SELF,
        // slot key exists for storage only; consumer reads from the imbuement
        // metadata map directly (e.g. spellbook page index, block activation).
        // no event dispatch, no registered CastRootDispatcher.
        MANUAL
    }
}
