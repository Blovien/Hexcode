package com.riprod.hexcode.core.state.crafting.entity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.riprod.hexcode.core.common.block.event.BlockBreakEvent;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.EffectComponent;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hexes.utils.CreateHex;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.riprod.hexcode.core.state.casting.utils.GlyphSpawner;
import com.riprod.hexcode.core.state.casting.utils.GlyphStyler;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalPlayerData;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.utils.PedestalBlockUtil;

public class AnchorEntity {

    private static final float GLYPH_DISPLAY_DISTANCE = 1.0f;
    private static final float PEDESTAL_GLYPH_PITCH = (float) (-Math.PI / 2);
    private static final Box PREVIEW_BOUNDING_BOX = new Box(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25);

    public static Ref<EntityStore> spawnFilledSlot(CommandBuffer<EntityStore> buffer, Hex hex,
            Ref<EntityStore> anchorRef, Vector3d anchorPos, Vector3f offset, @Nullable Ref<EntityStore> playerRef) {

        Vector3d globalPos = new Vector3d(anchorPos.x + offset.x, anchorPos.y + offset.y, anchorPos.z + offset.z);

        HexComponent hexComponent = new HexComponent(hex);
        hexComponent.setRootRef(anchorRef);
        hexComponent.setParentRef(null);
        hexComponent.setOffset(offset);

        Holder<EntityStore> holder = CreateHex.createHexHolder(buffer, hexComponent, globalPos);
        HiddenUtils.addHiddenToHolder(holder, playerRef);

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset("Selection_Anchor");

        if (modelAsset == null) {
            return null;
        }

        Model model = Model.createScaledModel(modelAsset, 1.0f);

        holder.addComponent(ModelComponent.getComponentType(),
                new ModelComponent(model));

        holder.addComponent(PersistentModel.getComponentType(),
                new PersistentModel(model.toReference()));

        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(PREVIEW_BOUNDING_BOX));
        holder.addComponent(HoverableComponent.getComponentType(), new HoverableComponent(HoverableType.HEX));

        Ref<EntityStore> hexRef = CreateHex.createEntity(buffer, holder);
        hexComponent.setSelfRef(hexRef);

        int numGlyphs = hex.getGlyphs().size();
        float scaleMultiplier = 1 + (numGlyphs * GlyphStyler.SCALE_PER_GLYPH);

        String firstGlyphId = hex.getFirstGlyphId();
        Glyph firstGlyph = hex.get(firstGlyphId);
        EffectComponent firstGlyphComponent = new EffectComponent(firstGlyph);

        firstGlyphComponent.setHexRef(hexRef);
        firstGlyphComponent.setParentRef(hexRef);
        firstGlyphComponent.setOffset(Vector3f.ZERO);
        firstGlyphComponent.setRotation(new Vector3f(PEDESTAL_GLYPH_PITCH, 0, GLYPH_DISPLAY_DISTANCE));
        firstGlyphComponent.setScale(scaleMultiplier);
        hexComponent.setScale(scaleMultiplier);

        GlyphSpawner.spawnGlyphs(buffer, hexComponent, firstGlyphComponent, globalPos, playerRef);

        return hexRef;
    }

    public static Ref<EntityStore> spawnEmptySlot(CommandBuffer<EntityStore> buffer,
            Ref<EntityStore> anchorRef, Vector3d anchorPos, Vector3f offset, @Nullable Ref<EntityStore> playerRef) {

        Vector3d globalPos = new Vector3d(anchorPos.x + offset.x, anchorPos.y + offset.y, anchorPos.z + offset.z);

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        HiddenUtils.addHiddenToHolder(holder, playerRef);

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset("Selection_Anchor");

        if (modelAsset == null) {
            return null;
        }

        Model model = Model.createScaledModel(modelAsset, 1.0f);

        holder.addComponent(ModelComponent.getComponentType(),
                new ModelComponent(model));

        holder.addComponent(PersistentModel.getComponentType(),
                new PersistentModel(model.toReference()));

        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(globalPos, new Vector3f(0, 0, 0)));
        holder.addComponent(UUIDComponent.getComponentType(),
                new UUIDComponent(UUID.randomUUID()));
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        int networkId = buffer.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(PREVIEW_BOUNDING_BOX));
        holder.addComponent(HoverableComponent.getComponentType(), new HoverableComponent(HoverableType.CONTAINER));
        holder.addComponent(DebugComponent.getComponentType(),
                new DebugComponent(DebugShape.Sphere, new Vector3f(0.5f, 0.5f, 0.5f), 0.5, 2.0f, playerRef));

        return buffer.addEntity(holder, AddReason.SPAWN);
    }

    public static void DespawnHexPreviews(CommandBuffer<EntityStore> buffer, PedestalBlockComponent pedestal,
            PedestalPlayerData playerData) {
        List<Ref<EntityStore>> refs = playerData.getHexPreviewRefs();
        if (refs == null || refs.isEmpty()) {
            return;
        }

        for (int i = 0; i < refs.size(); i++) {
            Ref<EntityStore> hexRef = refs.get(i);
            if (hexRef == null || !hexRef.isValid()) {
                continue;
            }

            HexComponent hexComp = buffer.getComponent(hexRef, HexComponent.getComponentType());
            if (hexComp != null) {
                Map<String, Ref<EntityStore>> childRefs = hexComp.getChildGlyphRefs();
                if (childRefs != null) {
                    for (Ref<EntityStore> glyphRef : childRefs.values()) {
                        if (glyphRef == null || !glyphRef.isValid())
                            continue;
                        EffectComponent effect = buffer.getComponent(glyphRef,
                                EffectComponent.getComponentType());
                        if (effect != null && effect.getNodeRef() != null
                                && effect.getNodeRef().isValid()) {
                            buffer.removeEntity(effect.getNodeRef(), RemoveReason.REMOVE);
                        }
                        buffer.removeEntity(glyphRef, RemoveReason.REMOVE);
                    }
                }
            }

            buffer.removeEntity(hexRef, RemoveReason.REMOVE);
        }

        playerData.clearHexPreviewRefs();
        playerData.setActiveHex(null);
        playerData.setActiveHexEntityRef(null);
    }
}
