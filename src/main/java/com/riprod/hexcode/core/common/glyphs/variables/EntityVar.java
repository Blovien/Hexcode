package com.riprod.hexcode.core.common.glyphs.variables;

import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.reference.PersistentRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EntityVar extends HexVar {
    private PersistentRef entity;

    public EntityVar() {
    }

    public EntityVar(PersistentRef entity) {
        this.entity = entity;
    }

    public EntityVar(UUID entityId, Ref<EntityStore> ref) {
        this.entity = createRef(entityId, ref);
    }

    public static PersistentRef createRef(UUID entityId, Ref<EntityStore> ref) {
        PersistentRef persistent = new PersistentRef();
        persistent.setEntity(ref, entityId);
        return persistent;
    }

    public PersistentRef getValue() {
        return entity;
    }

    @Nullable
    public Ref<EntityStore> getRef(ComponentAccessor<EntityStore> accessor) {
        if (entity == null) return null;
        return entity.getEntity(accessor);
    }

    @Override
    public Object getRawValue() {
        return entity;
    }

    @Override
    public Double toScalar() {
        return entity != null ? 1.0 : 0.0;
    }

    @Override
    public String describe() {
        if (entity == null) return "EntityVar: [null]";
        UUID id = entity.getUuid();
        if (id == null) return "EntityVar: [unset]";
        String s = id.toString();
        return "EntityVar: " + s.substring(0, Math.min(8, s.length()));
    }

    @Override
    public boolean equalTo(HexVar other) {
        if (other instanceof EntityVar ev) {
            if (entity == null || ev.entity == null) return entity == ev.entity;
            return entity.getUuid().equals(ev.entity.getUuid());
        }
        return super.equalTo(other);
    }

    @Override
    public int compareTo(HexVar other) {
        if (other instanceof EntityVar ev) {
            if (entity == null && ev.entity == null) return 0;
            if (entity == null) return -1;
            if (ev.entity == null) return 1;
            return entity.getUuid().compareTo(ev.entity.getUuid());
        }
        return super.compareTo(other);
    }

    @Override
    public String toString() {
        return "EntityVar(" + entity.getUuid() + ")";
    }

    public static final BuilderCodec<EntityVar> CODEC = BuilderCodec
            .builder(EntityVar.class, EntityVar::new, HexVar.BASE_CODEC)
            .append(new KeyedCodec<>("Entity", PersistentRef.CODEC),
                    (v, ref) -> v.entity = ref,
                    v -> v.entity)
            .add()
            .build();

    static {
        HexVar.CODEC.register("Entity", EntityVar.class, EntityVar.CODEC);
    }
}
