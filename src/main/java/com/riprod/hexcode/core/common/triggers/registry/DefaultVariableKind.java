package com.riprod.hexcode.core.common.triggers.registry;

import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.triggers.component.TriggerEvent;

public enum DefaultVariableKind {
    NONE,
    SELF,
    TARGET_ENTITY,
    ATTACKER;

    @Nullable
    public HexVar from(TriggerEvent event) {
        return switch (this) {
            case NONE -> null;
            case SELF -> entityVar(event.subjectUuid(), event.subjectRef());
            case TARGET_ENTITY -> {
                if (event.payload() instanceof TargetedPayload p) {
                    yield entityVar(p.targetUuid(), p.target());
                }
                yield null;
            }
            case ATTACKER -> {
                if (event.payload() instanceof AttackedPayload p) {
                    yield entityVar(p.attackerUuid(), p.attacker());
                }
                yield null;
            }
        };
    }

    @Nullable
    private static EntityVar entityVar(@Nullable UUID uuid, @Nullable Ref<EntityStore> ref) {
        if (uuid == null || ref == null) return null;
        return new EntityVar(uuid, ref);
    }

    public interface TargetedPayload {
        @Nullable UUID targetUuid();
        @Nullable Ref<EntityStore> target();
    }

    public interface AttackedPayload {
        @Nullable UUID attackerUuid();
        @Nullable Ref<EntityStore> attacker();
    }
}
