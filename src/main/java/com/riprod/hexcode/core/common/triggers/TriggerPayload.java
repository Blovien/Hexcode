package com.riprod.hexcode.core.common.triggers;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public sealed interface TriggerPayload permits
        TriggerPayload.Attack,
        TriggerPayload.Eat,
        TriggerPayload.Click,
        TriggerPayload.Move,
        TriggerPayload.Rotate,
        TriggerPayload.Death,
        TriggerPayload.Cast,
        TriggerPayload.Sleep,
        TriggerPayload.Empty {

    record Attack(@Nullable Ref<EntityStore> attacker,
                  Ref<EntityStore> victim,
                  @Nullable ItemStack weapon,
                  float damage) implements TriggerPayload {
    }

    record Eat(Ref<EntityStore> eater,
               @Nullable ItemStack food) implements TriggerPayload {
    }

    record Click(Ref<EntityStore> clicker,
                 boolean primary,
                 @Nullable ItemStack heldItem) implements TriggerPayload {
    }

    record Move(Ref<EntityStore> entity,
                Vector3d from,
                Vector3d to) implements TriggerPayload {
    }

    record Rotate(Ref<EntityStore> entity,
                  Vector3f from,
                  Vector3f to) implements TriggerPayload {
    }

    record Death(Ref<EntityStore> deceased,
                 @Nullable Ref<EntityStore> killer) implements TriggerPayload {
    }

    record Cast(Ref<EntityStore> caster,
                String castedHexId) implements TriggerPayload {
    }

    record Sleep(Ref<EntityStore> sleeper) implements TriggerPayload {
    }

    record Empty() implements TriggerPayload {
        public static final Empty INSTANCE = new Empty();
    }
}
