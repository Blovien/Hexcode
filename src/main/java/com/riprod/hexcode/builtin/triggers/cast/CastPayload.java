package com.riprod.hexcode.builtin.triggers.cast;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record CastPayload(Ref<EntityStore> caster) {
}
