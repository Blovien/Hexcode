package com.riprod.hexcode.core.crafting.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.crafting.component.PedestalState;
import com.riprod.hexcode.core.crafting.registry.PedestalBlockComponent;
import com.riprod.hexcode.core.crafting.spawners.AnchorSpawner;
import com.riprod.hexcode.core.crafting.spawners.PedestalSpawner;
import com.riprod.hexcode.core.crafting.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.crafting.utils.RadialPositionUtil;
import com.riprod.hexcode.core.hexes.component.Hex;
import com.riprod.hexcode.core.hexes.component.HexComponent;

public class PedestalSystem {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    private static final float PREVIEW_RADIUS = 3.5f;
    private static final Vector3f ACTIVE_HEX_OFFSET = new Vector3f(0, 1.3f, 0);

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
                Ref<EntityStore> hexRef = AnchorSpawner.spawnFilledSlot(buffer, hexes.get(i), anchorRef, anchorPos,
                        offset);
                spawnedRefs.add(hexRef);
            } else {
                Ref<EntityStore> emptyRef = AnchorSpawner.spawnEmptySlot(buffer, anchorRef, anchorPos, offset);
                spawnedRefs.add(emptyRef);
            }
        }

        pedestal.setHexPreviewRefs(spawnedRefs);
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
}
