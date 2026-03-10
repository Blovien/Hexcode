package com.riprod.hexcode.core.state.crafting.constants;

public enum NodeType {
    /** Not ready yet */
    IDLE,
    /** Ready to be activated */
    READY,
    /** Active and awaiting glyph selection */
    SELECTING,
    /** Actively crafting */
    CRAFTING,
}
