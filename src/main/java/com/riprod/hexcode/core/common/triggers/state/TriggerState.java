package com.riprod.hexcode.core.common.triggers.state;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.riprod.hexcode.core.common.construct.state.ConstructState;

public class TriggerState implements ConstructState {

    private String triggerKey;
    private UUID subscriptionId;
    private List<String> nextGlyphIds;
    private boolean fired;

    public TriggerState() {
        this.nextGlyphIds = new ArrayList<>();
    }

    public TriggerState(String triggerKey, UUID subscriptionId, List<String> nextGlyphIds) {
        this.triggerKey = triggerKey;
        this.subscriptionId = subscriptionId;
        this.nextGlyphIds = nextGlyphIds != null ? nextGlyphIds : new ArrayList<>();
        this.fired = false;
    }

    public String getTriggerKey() {
        return triggerKey;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public List<String> getNextGlyphIds() {
        return nextGlyphIds;
    }

    public void setNextGlyphIds(List<String> ids) {
        this.nextGlyphIds = ids != null ? ids : new ArrayList<>();
    }

    public boolean isFired() {
        return fired;
    }

    public void markFired() {
        this.fired = true;
    }

    @Override
    public TriggerState copy() {
        TriggerState c = new TriggerState(triggerKey, subscriptionId, new ArrayList<>(nextGlyphIds));
        c.fired = this.fired;
        return c;
    }
}
