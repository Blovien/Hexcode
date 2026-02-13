package com.riprod.hexcode.core.casting;

import java.util.List;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.casting.utils.GlyphPositioner;
import com.riprod.hexcode.core.casting.utils.GlyphSelector;
import com.riprod.hexcode.core.casting.utils.GlyphSpawner;
import com.riprod.hexcode.core.casting.utils.GlyphStyler;
import com.riprod.hexcode.core.execution.Compiler;
import com.riprod.hexcode.core.execution.component.HexGraph;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.utils.CreateGlyph;
import com.riprod.hexcode.core.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.state.HexcodeManager;
import com.riprod.hexcode.utils.GlyphMath;

public class CastingSystem extends HexcodeManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void firstTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        HexStaffComponent staff = CasterInventory.getHexStaffComponent(buffer, ref);
        HexBookComponent book = CasterInventory.getHexBookComponent(buffer, ref);

        if (staff == null || book == null) {
            LOGGER.atSevere()
                    .log("Player is missing required HexStaffComponent or HexBookComponent to enter casting mode");
            return;
        }

        List<GlyphComponent> glyphs = book.getGlyphs();
        String style = staff.getStyleId();

        TransformComponent transform = buffer.getComponent(ref, TransformComponent.getComponentType());
        Vector3d ownerPos = transform.getPosition();
        Ref<EntityStore> castingRootRef = CreateGlyph.createCastingRoot(buffer, ownerPos);

        comp.setCastingRootRef(castingRootRef);

        List<GlyphComponent> spawnedGlyphs = GlyphSpawner.spawnGlyphs(buffer, ref, castingRootRef, glyphs, style);
        comp.setActiveGlyphs(spawnedGlyphs);
    }

    @Override
    public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        List<GlyphComponent> activeGlyphs = comp.getActiveGlyphs();
        for (GlyphComponent glyph : activeGlyphs) {
            try {
                cleanupGlyphChildren(buffer, glyph);
                Holder<EntityStore> glyphHolder = EntityStore.REGISTRY.newHolder();
                buffer.removeEntity(glyph.getSelfRef(), glyphHolder, RemoveReason.REMOVE);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e)
                        .log("Failed to despawn glyph entity with ref: " + glyph.getId());
            }
        }

        Ref<EntityStore> castingRootRef = comp.getCastingRootRef();
        if (castingRootRef != null) {
            try {
                Holder<EntityStore> rootHolder = EntityStore.REGISTRY.newHolder();
                buffer.removeEntity(castingRootRef, rootHolder, RemoveReason.REMOVE);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to despawn casting root entity");
            }
        }

        HexStaffComponent staff = CasterInventory.getHexStaffComponent(buffer, ref);
        GlyphComponent rootGlyph = comp.getLastSelectedGlyph();

        if (rootGlyph != null && staff != null) {
            HexGraph compiledGlyph = Compiler.compile(rootGlyph);
            staff.setActiveSpell(compiledGlyph);
            CasterInventory.saveHexStaffComponent(buffer, ref, staff);
        }

        comp.clearCastingState();
    }

    @Override
    public void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        Ref<EntityStore> castingRootRef = comp.getCastingRootRef();

        TransformComponent transform = buffer.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotation = buffer.getComponent(ref, HeadRotation.getComponentType());
        if (transform == null || headRotation == null) {
            return;
        }
        Vector3d ownerPos = transform.getPosition();

        List<GlyphComponent> activeGlyphs = comp.getActiveGlyphs();

        if (activeGlyphs == null || castingRootRef == null || !castingRootRef.isValid()) {
            return;
        }

        GlyphPositioner.PositionGlyphs(buffer, ref, ownerPos, castingRootRef);

        GlyphComponent hoveredGlyph = GlyphSelector.GetHoveredGlyph(buffer, headRotation, activeGlyphs,
                comp.getDraggingGlyph() != null);

        GlyphStyler.HoverGlyph(buffer, hoveredGlyph, comp);
    }

    @Override
    public void onPlayerJoin(Holder<EntityStore> holder, HexcasterComponent comp) {
    }

    @Override
    public void onPlayerLeave(PlayerRef playerRef) {
    }

    @Override
    public InteractionState onPrimaryEnter(Ref<EntityStore> ref, HexcasterComponent comp,
            ComponentAccessor<EntityStore> accessor) {

        GlyphComponent hoveredGlyph = comp.getHoveredGlyph();
        if (hoveredGlyph == null) {
            return InteractionState.Failed;
        }

        GlyphStyler.ExitHover(accessor, hoveredGlyph);

        Ref<EntityStore> glyphRef = hoveredGlyph.getSelfRef();
        if (glyphRef != null && glyphRef.isValid()) {
            accessor.removeComponent(glyphRef, MountedComponent.getComponentType());
        }

        comp.setDraggingGlyph(hoveredGlyph);

        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState onPrimaryTick(Ref<EntityStore> ref, HexcasterComponent comp,
            ComponentAccessor<EntityStore> accessor) {

        if (comp.getDraggingGlyph() == null) {
            return InteractionState.Finished;
        }

        GlyphSelector.DragGlyph(accessor, ref, comp.getDraggingGlyph());

        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState onPrimaryExit(Ref<EntityStore> ref, HexcasterComponent comp,
            ComponentAccessor<EntityStore> accessor) {

        GlyphComponent draggedGlyph = comp.getDraggingGlyph();
        if (draggedGlyph == null) {
            return InteractionState.Finished;
        }

        HeadRotation headRotation = accessor.getComponent(ref, HeadRotation.getComponentType());
        if (headRotation == null) {
            return InteractionState.Failed;
        }

        GlyphComponent hoveredGlyph = GlyphSelector.GetHoveredGlyph(accessor, headRotation,
                comp.getActiveGlyphs(), true);

        if (hoveredGlyph != null) {
            try {
                float eyeHeight = 0f;
                ModelComponent modelComp = accessor.getComponent(ref, ModelComponent.getComponentType());
                eyeHeight = modelComp.getModel().getEyeHeight(ref, accessor);
                GlyphSpawner.MergeGlyphs(accessor, draggedGlyph, hoveredGlyph, eyeHeight);
                comp.setDraggingGlyph(null);
                comp.removeActiveGlyph(draggedGlyph.getId());
                return InteractionState.Finished;
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Error merging glyphs, dropping on ground instead");
            }
        }

        comp.setDraggingGlyph(null);

        float pitch = draggedGlyph.getPitch();
        float yaw = draggedGlyph.getYaw();
        double distance = draggedGlyph.getDistance();
        Vector3d pos = GlyphMath.sphericalToCartesian(new Vector3d(0, 0, 0), yaw, pitch, distance);

        accessor.putComponent(draggedGlyph.getSelfRef(), MountedComponent.getComponentType(),
                new MountedComponent(draggedGlyph.getRootRef(),
                        new Vector3f((float) pos.x, (float) pos.y, (float) pos.z),
                        MountController.Minecart));

        return InteractionState.Finished;
    }

    private void cleanupGlyphChildren(ComponentAccessor<EntityStore> accessor, GlyphComponent glyph) {
        try {
            List<GlyphComponent> children = glyph.getChildren();
            if (children != null) {
                for (GlyphComponent child : children) {
                    cleanupGlyphChildren(accessor, child);
                    Holder<EntityStore> childHolder = EntityStore.REGISTRY.newHolder();
                    accessor.removeEntity(child.getSelfRef(), childHolder, RemoveReason.REMOVE);
                }
            }
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("Failed to cleanup child glyphs for glyph with ref: " + glyph.getId());
        }
    }
}
