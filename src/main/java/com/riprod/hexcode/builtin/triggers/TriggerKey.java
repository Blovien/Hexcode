package com.riprod.hexcode.builtin.triggers;

// trigger keys aligned with Hytale's InteractionType taxonomy where applicable.
// these strings are used both as registry keys AND as imbuement slot keys on
// items, so a weapon's "Primary" slot fires on the wielder's primary interaction.
public final class TriggerKey {

    public static final String PRIMARY = "Primary";
    public static final String SECONDARY = "Secondary";
    public static final String USE = "Use";

    public static final String DEATH = "Death";

    public static final String CAST = "Cast";

    public static final String SLEEP = "Sleep";

    public static final String[] ALL = new String[] {
            PRIMARY, SECONDARY, USE,
            DEATH, CAST, SLEEP
    };

    private TriggerKey() {
    }
}
