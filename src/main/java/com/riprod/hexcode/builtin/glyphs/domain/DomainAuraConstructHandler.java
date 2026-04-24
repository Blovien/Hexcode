package com.riprod.hexcode.builtin.glyphs.domain;

import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;

public class DomainAuraConstructHandler implements ConstructHandler<DomainAuraState> {
    // passive state-carrier; zone handler adds/removes via applyWithState / requestKillByHandlerId.
    // default onTick consumes volatility per second, ending aura naturally if caster outlives the zone.
}
