package com.riprod.hexcode.core.crafting.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.casting.utils.GlyphSpawner;
import com.riprod.hexcode.core.casting.utils.GlyphStyler;
import com.riprod.hexcode.core.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.crafting.component.PedestalState;
import com.riprod.hexcode.core.crafting.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.crafting.utils.PedestalSpawner;
import com.riprod.hexcode.core.crafting.utils.RadialPositionUtil;
import com.riprod.hexcode.core.debug.DebugComponent;
import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.hexes.component.Hex;
import com.riprod.hexcode.core.hexes.component.HexComponent;
import com.riprod.hexcode.core.hexes.utils.CreateHex;

public class PedestalSystem {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    private static final float PREVIEW_RADIUS = 3.5f;
    private static final float GLYPH_DISPLAY_DISTANCE = 1.0f;
    private static final float PEDESTAL_GLYPH_PITCH = (float) (-Math.PI / 2);
    private static final Box PREVIEW_BOUNDING_BOX = new Box(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25);
    private static final Vector3f ACTIVE_HEX_OFFSET = new Vector3f(0, 2.0f, 0);

    public static void SpawnHexPreviews(CommandBuffer<EntityStore> buffer, PedestalBlockComponent pedestal) {
        Integer totalSlots = pedestal.getBookSlots();
        if (totalSlots == null || totalSlots <= 0) {
            return;
        }

        List<Hex> hexes = pedestal.getHexes();
        Ref<EntityStore> anchorRef = pedestal.getAnchorRef();
        if (anchorRef == null || !anchorRef.isValid()) {
            return;
        }

        Vector3d anchorPos = PedestalSpawner.getAnchorPosition(pedestal.getLocation());
        List<Vector3f> offsets = RadialPositionUtil.calculateOffsets(totalSlots, PREVIEW_RADIUS, 0);
        List<Ref<EntityStore>> spawnedRefs = new ArrayList<>();

        for (int i = 0; i < totalSlots; i++) {
            Vector3f offset = offsets.get(i);

            if (i < hexes.size()) {
                Ref<EntityStore> hexRef = spawnFilledSlot(buffer, hexes.get(i), anchorRef, anchorPos, offset);
                spawnedRefs.add(hexRef);
            } else {
                Ref<EntityStore> emptyRef = spawnEmptySlot(buffer, anchorRef, anchorPos, offset);
                spawnedRefs.add(emptyRef);
            }
        }

        pedestal.setHexPreviewRefs(spawnedRefs);
    }

    private static Ref<EntityStore> spawnFilledSlot(CommandBuffer<EntityStore> buffer, Hex hex,
            Ref<EntityStore> anchorRef, Vector3d anchorPos, Vector3f offset) {

        Vector3d globalPos = new Vector3d(anchorPos.x + offset.x, anchorPos.y + offset.y, anchorPos.z + offset.z);

        HexComponent hexComponent = new HexComponent(hex);
        hexComponent.setRootRef(anchorRef);
        hexComponent.setParentRef(null);
        hexComponent.setOffset(offset);

        Holder<EntityStore> holder = CreateHex.createHexHolder(buffer, hexComponent, globalPos);
        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(PREVIEW_BOUNDING_BOX));
        holder.addComponent(DebugComponent.getComponentType(),
                new DebugComponent(DebugShape.Cube, new Vector3f(0f, 1f, 0f), 0.5, 2.0f));

        Ref<EntityStore> hexRef = CreateHex.createEntity(buffer, holder);
        hexComponent.setSelfRef(hexRef);

        int numGlyphs = hex.getGlyphs().size();
        float scaleMultiplier = 1 + (numGlyphs * GlyphStyler.SCALE_PER_GLYPH);

        String firstGlyphId = hex.getFirstGlyphId();
        Glyph firstGlyph = hex.get(firstGlyphId);
        GlyphComponent firstGlyphComponent = new GlyphComponent(firstGlyph);

        firstGlyphComponent.setHexRef(hexRef);
        firstGlyphComponent.setParentRef(hexRef);
        firstGlyphComponent.setOffset(Vector3f.ZERO);
        firstGlyphComponent.setRotation(new Vector3f(PEDESTAL_GLYPH_PITCH, 0, GLYPH_DISPLAY_DISTANCE));
        firstGlyphComponent.setScale(scaleMultiplier);
        hexComponent.setScale(scaleMultiplier);

        GlyphSpawner.spawnGlyphs(buffer, hexComponent, firstGlyphComponent, globalPos);

        return hexRef;
    }

    private static Ref<EntityStore> spawnEmptySlot(CommandBuffer<EntityStore> buffer,
            Ref<EntityStore> anchorRef, Vector3d anchorPos, Vector3f offset) {

        Vector3d globalPos = new Vector3d(anchorPos.x + offset.x, anchorPos.y + offset.y, anchorPos.z + offset.z);

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(globalPos, new Vector3f(0, 0, 0)));
        holder.addComponent(UUIDComponent.getComponentType(),
                new UUIDComponent(UUID.randomUUID()));
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        int networkId = buffer.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(PREVIEW_BOUNDING_BOX));
        holder.addComponent(DebugComponent.getComponentType(),
                new DebugComponent(DebugShape.Cube, new Vector3f(0.5f, 0.5f, 0.5f), 0.5, 2.0f));

        return buffer.addEntity(holder, AddReason.SPAWN);
    }

    public static void ActivateHexSelection(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestal, Ref<EntityStore> selectedHexRef) {

        List<Ref<EntityStore>> refs = pedestal.getHexPreviewRefs();
        if (refs == null || refs.isEmpty()) {
            return;
        }

        for (Ref<EntityStore> ref : refs) {
            if (ref == null || !ref.isValid() || ref.equals(selectedHexRef)) {
                continue;
            }

            HexComponent hexComp = buffer.getComponent(ref, HexComponent.getComponentType());
            if (hexComp != null) {
                Map<String, Ref<EntityStore>> childRefs = hexComp.getChildGlyphRefs();
                if (childRefs != null) {
                    for (Ref<EntityStore> glyphRef : childRefs.values()) {
                        if (glyphRef != null && glyphRef.isValid()) {
                            buffer.removeEntity(glyphRef, RemoveReason.REMOVE);
                        }
                    }
                }
            }

            buffer.removeEntity(ref, RemoveReason.REMOVE);
        }

        Vector3d anchorPos = PedestalSpawner.getAnchorPosition(pedestal.getLocation());
        Vector3d activePos = new Vector3d(
                anchorPos.x + ACTIVE_HEX_OFFSET.x,
                anchorPos.y + ACTIVE_HEX_OFFSET.y,
                anchorPos.z + ACTIVE_HEX_OFFSET.z);
        buffer.putComponent(selectedHexRef, TransformComponent.getComponentType(),
                new TransformComponent(activePos, new Vector3f(0, 0, 0)));
        if (buffer.getComponent(selectedHexRef, MountedComponent.getComponentType()) != null) {
            buffer.removeComponent(selectedHexRef, MountedComponent.getComponentType());
        }

        pedestal.setHexPreviewRefs(List.of(selectedHexRef));
        pedestal.setActiveHexEntityRef(selectedHexRef);
        pedestal.setState(PedestalState.CRAFTING);
        ObeliskProtectionSystem.protect(pedestal.getLocation());

        PedestalBlockUtil.changeBlockState(
                buffer.getExternalData().getWorld(), pedestal.getLocation(), "Active");
    }

    public static void DespawnHexPreviews(CommandBuffer<EntityStore> buffer, PedestalBlockComponent pedestal) {
        List<Ref<EntityStore>> refs = pedestal.getHexPreviewRefs();
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
                        if (glyphRef != null && glyphRef.isValid()) {
                            buffer.removeEntity(glyphRef, RemoveReason.REMOVE);
                        }
                    }
                }
            }

            buffer.removeEntity(hexRef, RemoveReason.REMOVE);
        }

        pedestal.clearHexPreviewRefs();
        pedestal.setActiveHex(null);
        pedestal.setActiveHexEntityRef(null);
    }
}
