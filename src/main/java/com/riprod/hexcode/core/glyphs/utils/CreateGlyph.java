package com.riprod.hexcode.core.glyphs.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.utils.GlyphMath;
import com.riprod.hexcode.utils.SphericalPosition;

public class CreateGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static Ref<EntityStore> createCastingRoot(ComponentAccessor<EntityStore> accessor,
            Vector3d playerPos) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        Vector3d eyePos = new Vector3d(playerPos.x, playerPos.y + 1.8, playerPos.z);
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(eyePos, new Vector3f(0, 0, 0)));
        holder.addComponent(UUIDComponent.getComponentType(),
                new UUIDComponent(UUID.randomUUID()));

        int networkId = accessor.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

         Ref<EntityStore> ref = accessor.addEntity(holder, AddReason.SPAWN);

        return ref;
    }

    public static Holder<EntityStore> createGlyphHolder(GlyphComponent glyph, Vector3d parentPos) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());

        if (asset == null) {
            throw new IllegalArgumentException("Unknown glyph ID: " + glyph.getGlyphId());
        }

        holder.addComponent(GlyphComponent.getComponentType(), glyph);

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(asset.getModelPath());
        if (modelAsset == null) {
            throw new IllegalArgumentException("Unknown model asset: " + asset.getModelPath());
        }

        Ref<EntityStore> hexRoot = glyph.getRootRef();

        // Required components
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(parentPos, new Vector3f(glyph.getPitch(), glyph.getYaw(), 0)));

        Model model = Model.createScaledModel(modelAsset, glyph.getScale());

        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));

        holder.addComponent(UUIDComponent.getComponentType(),
                new UUIDComponent(UUID.randomUUID()));
        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(model.getBoundingBox()));
        holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));

        if (hexRoot != null) {
            MountedComponent mounted = new MountedComponent(hexRoot, glyph.getOffset(), MountController.Minecart);
            holder.addComponent(MountedComponent.getComponentType(), mounted);
            LOGGER.atInfo().log("[createGlyphHolder] transform at %s", glyph.getOffset());
        }

        return holder;
    }

    /**
     * creates the glyph entity and adds a transform component to the base glyph for
     * dragging
     * 
     * @throws
     */
    public static List<Ref<EntityStore>> createGlyphEntity(ComponentAccessor<EntityStore> accessor,
            GlyphComponent glyph, Vector3d parentPos) {
        Holder<EntityStore> holder = createGlyphHolder(glyph, parentPos);

        int networkId = accessor.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

        return createGlyphEntity(accessor, holder, parentPos);
    }

    /**
     * @throws
     * @param accessor
     * @param holder
     * @return
     */
    public static List<Ref<EntityStore>> createGlyphEntity(ComponentAccessor<EntityStore> buffer,
            Holder<EntityStore> holder, Vector3d parentPos) {

        GlyphComponent glyphInfo = holder.getComponent(GlyphComponent.getComponentType());
        String glyphId = glyphInfo != null ? glyphInfo.getGlyphId() : "unknown";
        LOGGER.atInfo().log("[createGlyphEntity] spawning '%s' via %s", glyphId, buffer.getClass().getSimpleName());

        List<Ref<EntityStore>> refs = new ArrayList<>();
        Ref<EntityStore> ref = createEntity(buffer, holder);
        LOGGER.atInfo().log("[createGlyphEntity] addEntity returned ref valid=%s for '%s'", ref.isValid(), glyphId);
        refs.add(ref);

        // add any children glyphs
        GlyphComponent glyphComp = holder.getComponent(GlyphComponent.getComponentType());
        List<GlyphComponent> children = glyphComp.getChildren();
        glyphComp.setSelfRef(ref); // backwards reference
        if (children == null) {
            return refs;
        }

        List<Vector3f> offsets = calculateChildOffsets(children.size(), glyphComp.getScale());
        for (int i = 0; i < children.size(); i++) {
            GlyphComponent childGlyph = children.get(i);
            childGlyph.setOwnerRef(ref);
            childGlyph.setScale(glyphComp.getScale() * 0.5f); // TODO: finalize child glyph scale
            childGlyph.setOffset(offsets.get(i));
            childGlyph.setRootRef(ref);

            try {
                List<Ref<EntityStore>> childRefs = createGlyphEntity(buffer, childGlyph, parentPos);
                refs.addAll(childRefs);
                LOGGER.atInfo().log("Spawned child glyph with ID: " + childGlyph.getGlyphId());
            } catch (Exception e) {
                // just log
                LOGGER.atSevere().withCause(e)
                        .log("Failed to create child glyph entity for glyph ID: " + childGlyph.getGlyphId());
            }
        }

        try {
            // get the root position
            TransformComponent transform = holder.getComponent(TransformComponent.getComponentType());
            Vector3d rootPos = transform.getPosition();
            LOGGER.atInfo().log("Created glyph entity with ID: %s at location %s", glyphComp.getGlyphId(), rootPos);
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e)
                    .log("Failed to log created glyph entity with ID: " + glyphComp.getGlyphId());
        }

        return refs;
    }

    public static Ref<EntityStore> createEntity(ComponentAccessor<EntityStore> buffer, Holder<EntityStore> holder) {
        Ref<EntityStore> ref = buffer.addEntity(holder, AddReason.SPAWN);
        return ref;
    }

    private static List<Vector3f> calculateChildOffsets(int childCount, float parentScale) {
        List<Vector3f> offsets = new ArrayList<>();
        float angleIncrement = 360.0f / childCount;
        float radius = parentScale * 1.5f; // example radius based on parent scale

        for (int i = 0; i < childCount; i++) {
            float angle = i * angleIncrement;
            float radian = (float) Math.toRadians(angle);
            float x = radius * (float) Math.cos(radian);
            float z = radius * (float) Math.sin(radian);
            offsets.add(new Vector3f(x, 0, z));
        }

        return offsets;
    }

}
