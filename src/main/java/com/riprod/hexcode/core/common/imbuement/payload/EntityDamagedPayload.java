package com.riprod.hexcode.core.common.imbuement.payload;

import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.triggers.registry.DefaultVariableKind;

// payload for triggers fired on the target side of a damage event.
// attacker/attackerUuid feed DefaultVariableKind.ATTACKER so the imbued
// hex's defaultVariable resolves to the entity that hit the wearer.
public record EntityDamagedPayload(@Nullable Ref<EntityStore> attacker, @Nullable UUID attackerUuid)
        implements DefaultVariableKind.AttackedPayload {
}
