package com.riprod.hexcode.core.state.crafting.constants;

public enum PedestalState {
    /** Not ready yet */
    IDLE,
    /** Ready for use */
    READY,
    /** Active and awaiting glyph selection */
    SELECTING,
    /** Actively crafting */
    CRAFTING,
}
