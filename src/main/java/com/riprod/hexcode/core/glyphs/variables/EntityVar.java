package com.riprod.hexcode.core.glyphs.variables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.reference.PersistentRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EntityVar extends HexVar {
    private List<PersistentRef> entities;

    public EntityVar() {
    }

    public EntityVar(List<PersistentRef> entities) {
        this.entities = entities;
    }

    public EntityVar(PersistentRef entity) {
        this.entities = new ArrayList<>(List.of(entity));
    }

    public EntityVar(UUID entityId, Ref<EntityStore> ref) {
        this.entities = new ArrayList<>(List.of(createRef(entityId, ref)));
    }

    public static PersistentRef createRef(UUID entityId, Ref<EntityStore> ref) {
        PersistentRef persistent = new PersistentRef();
        persistent.setEntity(ref, entityId);
        return persistent;
    }

    public List<PersistentRef> getValues() {
        return entities;
    }

    public PersistentRef getAt(int index) {
        return entities.get(index);
    }

    @Nullable
    public Ref<EntityStore> getRef(int index, ComponentAccessor<EntityStore> accessor) {
        return entities.get(index).getEntity(accessor);
    }

    public void addEntity(PersistentRef entity) {
        this.entities.add(entity);
    }

    public void addEntity(UUID entityId, Ref<EntityStore> ref) {
        this.entities.add(createRef(entityId, ref));
    }

    public void removeEntity(UUID entityId) {
        this.entities.removeIf(e -> entityId.equals(e.getUuid()));
    }

    @Override
    public int size() {
        return entities.size();
    }

    public static final BuilderCodec<EntityVar> CODEC = BuilderCodec
            .builder(EntityVar.class, EntityVar::new, HexVar.BASE_CODEC)
            .append(new KeyedCodec<>("Entities", new ArrayCodec<>(PersistentRef.CODEC, PersistentRef[]::new)),
                    (v, refs) -> v.entities = new ArrayList<>(Arrays.asList(refs)),
                    v -> v.entities.toArray(PersistentRef[]::new))
            .add()
            .build();

    static {
        HexVar.CODEC.register("Entity", EntityVar.class, EntityVar.CODEC);
    }
}
