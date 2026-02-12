package com.riprod.hexcode.core.glyphs.variables;

import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EntityVar extends SpellVar {
    public UUID entityId;
    public transient Ref<EntityStore> ref;

    public EntityVar() {
    }

    public EntityVar(UUID entityId, Ref<EntityStore> ref) {
        this.entityId = entityId;
        this.ref = ref;
    }

    public static final BuilderCodec<EntityVar> CODEC = BuilderCodec
            .builder(EntityVar.class, EntityVar::new, SpellVar.BASE_CODEC)
            .append(new KeyedCodec<>("EntityId", Codec.UUID_STRING),
                    (v, id) -> v.entityId = id,
                    v -> v.entityId)
            .add()
            .build();

    static {
        SpellVar.CODEC.register("Entity", EntityVar.class, EntityVar.CODEC);
    }
}