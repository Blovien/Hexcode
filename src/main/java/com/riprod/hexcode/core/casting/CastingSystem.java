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
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.casting.utils.GlyphPositioner;
import com.riprod.hexcode.core.casting.utils.GlyphSelector;
import com.riprod.hexcode.core.casting.utils.GlyphSpawner;
import com.riprod.hexcode.core.casting.utils.GlyphStyler;
import com.riprod.hexcode.core.execution.Compiler;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.utils.CreateGlyph;
import com.riprod.hexcode.core.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.hexbook.registry.HexBookAsset;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.core.hexstaff.registry.HexStaffAsset;
import com.riprod.hexcode.state.HexState;
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

        Player player = buffer.getComponent(ref, Player.getComponentType());
        HexStaffAsset staffAsset = CasterInventory.getHexStaffAsset(player.getInventory().getItemInHand());
        HexBookAsset bookAsset = CasterInventory.getHexBookAsset(player.getInventory().getUtilityItem());
        ModelParticle[] staffParticles = staffAsset != null ? staffAsset.getCastingAuraParticles() : null;
        ModelParticle[] bookParticles = bookAsset != null ? bookAsset.getCastingAuraParticles() : null;
        ModelParticle[] particles = mergeParticles(staffParticles, bookParticles);

        float eyeHeight = 0f;
        ModelComponent modelComp = buffer.getComponent(ref, ModelComponent.getComponentType());
        if (modelComp != null && modelComp.getModel() != null) {
            eyeHeight = modelComp.getModel().getEyeHeight(ref, buffer);
        }
        Ref<EntityStore> castingRootRef = CreateGlyph.createCastingRoot(buffer, ref, eyeHeight, particles);

        comp.setCastingRootRef(castingRootRef);

        List<GlyphComponent> spawnedGlyphs = GlyphSpawner.spawnGlyphs(buffer, ref, castingRootRef, glyphs, style);
        comp.setActiveGlyphs(spawnedGlyphs);
    }

    @Override
    public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        cleanupEntities(buffer, comp);

        HexStaffComponent staff = CasterInventory.getHexStaffComponent(buffer, ref);
        GlyphComponent rootGlyph = comp.getLastSelectedGlyph();

        if (rootGlyph != null && staff != null) {
            staff.setActiveSpell(rootGlyph);
            CasterInventory.saveHexStaffComponent(buffer, ref, staff);
            comp.requestStateChange(HexState.EXECUTION);
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

        // Despawn head anchor if not dragging a glyph
        Ref<EntityStore> headAnchor = comp.getHeadAnchorRef();
        if (comp.getDraggingGlyph() == null && headAnchor != null && headAnchor.isValid()) {
            Holder<EntityStore> headHolder = EntityStore.REGISTRY.newHolder();
            buffer.removeEntity(headAnchor, headHolder, RemoveReason.REMOVE);
            comp.setHeadAnchorRef(null);
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
    public InteractionState enterInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
            CommandBuffer<EntityStore> accessor) {

        GlyphComponent hoveredGlyph = comp.getHoveredGlyph();
        if (hoveredGlyph == null) {
            return InteractionState.Failed;
        }

        float eyeHeight = 0f;
        ModelComponent modelComp = accessor.getComponent(ref, ModelComponent.getComponentType());
        if (modelComp != null && modelComp.getModel() != null) {
            eyeHeight = modelComp.getModel().getEyeHeight(ref, accessor);
        }

        Ref<EntityStore> headRootRef = CreateGlyph.createHeadAnchor(accessor, ref, eyeHeight);

        comp.setHeadAnchorRef(headRootRef);

        GlyphStyler.ExitHover(accessor, hoveredGlyph);

        Ref<EntityStore> glyphRef = hoveredGlyph.getSelfRef();
        if (glyphRef != null && glyphRef.isValid()) {
            accessor.removeComponent(glyphRef, MountedComponent.getComponentType());
        }

        comp.setDraggingGlyph(hoveredGlyph);

        float distance = hoveredGlyph.getDistance();
        accessor.addComponent(glyphRef, MountedComponent.getComponentType(),
                new MountedComponent(headRootRef, new Vector3f(0, 0, -distance), MountController.Minecart));

        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState tickInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
            CommandBuffer<EntityStore> accessor) {

        if (comp.getDraggingGlyph() == null) {
            return InteractionState.Finished;
        }

        // Update on prim tick
        Ref<EntityStore> headAnchor = comp.getHeadAnchorRef();
        // head anchor: match head look direction
        if (headAnchor != null && headAnchor.isValid()) {
            HeadRotation headRot = accessor.getComponent(ref, HeadRotation.getComponentType());
            if (headRot != null) {
                TransformComponent headTransform = accessor.getComponent(headAnchor,
                        TransformComponent.getComponentType());
                headTransform.getRotation().assign(headRot.getRotation().getPitch(), headRot.getRotation().getYaw(), 0);
            }
        }

        GlyphSelector.DragGlyph(accessor, ref, comp.getDraggingGlyph());

        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState exitInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
            CommandBuffer<EntityStore> accessor) {

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
        float distance = draggedGlyph.getDistance();
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

    private static ModelParticle[] mergeParticles(ModelParticle[] a, ModelParticle[] b) {
        if (a == null || a.length == 0) return b;
        if (b == null || b.length == 0) return a;
        ModelParticle[] merged = new ModelParticle[a.length + b.length];
        System.arraycopy(a, 0, merged, 0, a.length);
        System.arraycopy(b, 0, merged, a.length, b.length);
        return merged;
    }

    private void cleanupEntities(ComponentAccessor<EntityStore> accessor, HexcasterComponent comp) {
        List<GlyphComponent> activeGlyphs = comp.getActiveGlyphs();
        for (GlyphComponent glyph : activeGlyphs) {
            try {
                cleanupGlyphChildren(accessor, glyph);
                Holder<EntityStore> glyphHolder = EntityStore.REGISTRY.newHolder();
                accessor.removeEntity(glyph.getSelfRef(), glyphHolder, RemoveReason.REMOVE);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e)
                        .log("Failed to despawn glyph entity with ref: " + glyph.getId());
            }
        }

        Ref<EntityStore> castingRootRef = comp.getCastingRootRef();
        if (castingRootRef != null) {
            try {
                Holder<EntityStore> rootHolder = EntityStore.REGISTRY.newHolder();
                accessor.removeEntity(castingRootRef, rootHolder, RemoveReason.REMOVE);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to despawn casting root entity");
            }
        }

        Ref<EntityStore> headAnchor = comp.getHeadAnchorRef();
        if (headAnchor != null && headAnchor.isValid()) {
            try {
                Holder<EntityStore> headHolder = EntityStore.REGISTRY.newHolder();
                accessor.removeEntity(headAnchor, headHolder, RemoveReason.REMOVE);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to despawn head anchor entity");
            }
        }
    }
}
