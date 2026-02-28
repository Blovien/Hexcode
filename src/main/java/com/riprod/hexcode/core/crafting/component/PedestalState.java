package com.riprod.hexcode.core.crafting.component;

public enum PedestalState {
    /** Not ready yet */
    OFF,
    /** Ready to be activated */
    READY,
    /** Active and awaiting glyph selection */
    ACTIVE,
    /** Actively crafting */
    CRAFTING
}
