package com.riprod.hexcode.core.common.triggers;

public final class TriggerKey {

    public static final String ON_ATTACK = "OnAttack";
    public static final String ON_EAT = "OnEat";
    public static final String ON_RIGHT_CLICK = "OnRightClick";
    public static final String ON_LEFT_CLICK = "OnLeftClick";
    public static final String ON_ROTATE = "OnRotate";
    public static final String ON_MOVE = "OnMove";
    public static final String ON_DEATH = "OnDeath";
    public static final String ON_CAST = "OnCast";
    public static final String ON_SLEEP = "OnSleep";

    public static final String[] ALL = new String[] {
            ON_ATTACK, ON_EAT, ON_RIGHT_CLICK, ON_LEFT_CLICK,
            ON_ROTATE, ON_MOVE, ON_DEATH, ON_CAST, ON_SLEEP
    };

    private TriggerKey() {
    }
}
