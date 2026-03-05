package com.riprod.hexcode.core.state.casting;

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
import com.riprod.hexcode.core.common.glyphs.component.EffectComponent;
import com.riprod.hexcode.core.common.glyphs.utils.CreateGlyph;
import com.riprod.hexcode.core.common.hexbook.component.HexBookAsset;
import com.riprod.hexcode.core.common.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffAsset;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.core.state.casting.component.HexcasterCastingComponent;
import com.riprod.hexcode.core.state.casting.utils.GlyphPositioner;
import com.riprod.hexcode.core.state.casting.utils.GlyphStyler;
import com.riprod.hexcode.core.state.casting.utils.HexSelector;
import com.riprod.hexcode.core.state.casting.utils.HexSpawner;
import com.riprod.hexcode.core.state.casting.utils.RootSpawner;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.state.HexcodeManager;
import com.riprod.hexcode.utils.GlyphMath;
import com.riprod.hexcode.utils.HexSlot;

public class CastingSystem extends HexcodeManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void firstTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        HexcasterCastingComponent castingComp = new HexcasterCastingComponent();
        buffer.addComponent(ref, HexcasterCastingComponent.getComponentType(), castingComp);

        HexStaffComponent staff = CasterInventory.getHexStaffComponent(buffer, ref);
        HexBookComponent book = CasterInventory.getHexBookComponent(buffer, ref);

        if (staff == null || book == null) {
            LOGGER.atSevere()
                    .log("Player is missing required HexStaffComponent or HexBookComponent to enter casting mode");
            return;
        }

        List<Hex> hexes = book.getHexes();
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
        Ref<EntityStore> castingRootRef = RootSpawner.createCastingRoot(buffer, ref, eyeHeight, particles);
        castingComp.setCastingRootRef(castingRootRef);

        List<Ref<EntityStore>> spawnedHexes = HexSpawner.spawnHexes(buffer, ref, castingRootRef, hexes, style);
        castingComp.setActiveHexes(spawnedHexes);
    }

    @Override
    public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        HexcasterCastingComponent castingComp = buffer.getComponent(ref, HexcasterCastingComponent.getComponentType());

        cleanupEntities(buffer, castingComp);

        HexStaffComponent staff = CasterInventory.getHexStaffComponent(buffer, ref);
        HexComponent rootGlyph = castingComp.getLastSelectedHex();

        if (rootGlyph != null && staff != null) {
            staff.setActiveHex(rootGlyph.getHex());
            CasterInventory.saveHexStaffComponent(buffer, ref, staff);
            comp.requestStateChange(HexState.EXECUTION);
        }

        castingComp.clearCastingState();
        buffer.tryRemoveComponent(ref, HexcasterCastingComponent.getComponentType());
    }

    @Override
    public void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        HexcasterCastingComponent castingComp = buffer.getComponent(ref, HexcasterCastingComponent.getComponentType());
        Ref<EntityStore> castingRootRef = castingComp.getCastingRootRef();

        TransformComponent transform = buffer.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotation = buffer.getComponent(ref, HeadRotation.getComponentType());
        if (transform == null || headRotation == null) {
            return;
        }
        Vector3d ownerPos = transform.getPosition();

        List<Ref<EntityStore>> activeHexes = castingComp.getActiveHexes();

        if (activeHexes == null || castingRootRef == null || !castingRootRef.isValid()) {
            return;
        }

        // Despawn head anchor if not dragging a hex
        Ref<EntityStore> headAnchor = castingComp.getHeadAnchorRef();
        if (castingComp.getDraggingHex() == null && headAnchor != null && headAnchor.isValid()) {
            Holder<EntityStore> headHolder = EntityStore.REGISTRY.newHolder();
            buffer.removeEntity(headAnchor, headHolder, RemoveReason.REMOVE);
            castingComp.setHeadAnchorRef(null);
        }

        GlyphPositioner.PositionGlyphs(buffer, ref, ownerPos, castingRootRef);

        HexComponent hoveredHex = HexSelector.findHoveredHex(buffer, headRotation.getRotation(), activeHexes);

        GlyphStyler.hoverHex(buffer, hoveredHex, castingComp);
    }

    @Override
    public void onPlayerJoin(Holder<EntityStore> holder, HexcasterComponent comp) {
    }

    @Override
    public void onPlayerLeave(PlayerRef playerRef) {
    }

    @Override
    public InteractionState enterInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref, HexcasterComponent comp) {

        HexcasterCastingComponent castingComp = accessor.getComponent(ref,
                HexcasterCastingComponent.getComponentType());

        HexComponent hoveredHex = castingComp.getHoveredHex();
        if (hoveredHex == null) {
            return InteractionState.Failed;
        }

        float eyeHeight = 0f;
        ModelComponent modelComp = accessor.getComponent(ref, ModelComponent.getComponentType());
        if (modelComp != null && modelComp.getModel() != null) {
            eyeHeight = modelComp.getModel().getEyeHeight(ref, accessor);
        }

        Ref<EntityStore> headRootRef = CreateGlyph.createHeadAnchor(accessor, ref, eyeHeight);

        castingComp.setHeadAnchorRef(headRootRef);

        GlyphStyler.hoverHex(accessor, hoveredHex, castingComp);

        Ref<EntityStore> glyphRef = hoveredHex.getSelfRef();
        if (glyphRef != null && glyphRef.isValid()) {
            accessor.tryRemoveComponent(glyphRef, MountedComponent.getComponentType());

            float distance = hoveredHex.getDistance();
            accessor.addComponent(glyphRef, MountedComponent.getComponentType(),
                    new MountedComponent(headRootRef, new Vector3f(0, 0, -distance), MountController.Minecart));
        }

        castingComp.setDraggingHex(hoveredHex);

        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState tickInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref, float dt, HexcasterComponent comp) {

        HexcasterCastingComponent castingComp = accessor.getComponent(ref,
                HexcasterCastingComponent.getComponentType());

        if (castingComp.getDraggingHex() == null) {
            return InteractionState.Finished;
        }

        // Update on prim tick
        Ref<EntityStore> headAnchor = castingComp.getHeadAnchorRef();
        // head anchor: match head look direction
        if (headAnchor != null && headAnchor.isValid()) {
            HeadRotation headRot = accessor.getComponent(ref, HeadRotation.getComponentType());
            if (headRot != null) {
                TransformComponent headTransform = accessor.getComponent(headAnchor,
                        TransformComponent.getComponentType());
                headTransform.getRotation().assign(headRot.getRotation().getPitch(), headRot.getRotation().getYaw(), 0);
            }
        }

        HexSelector.DragGlyph(accessor, ref, castingComp.getDraggingHex());

        HeadRotation headRot2 = accessor.getComponent(ref, HeadRotation.getComponentType());
        if (headRot2 != null) {
            HexComponent targetHex = castingComp.getHoveredHex();
            EffectComponent targetGlyph = null;
            if (targetHex != null && targetHex != castingComp.getDraggingHex()) {
                targetGlyph = HexSelector.findHoveredGlyph(accessor, headRot2.getRotation(), targetHex);
            }
            GlyphStyler.hoverGlyph(accessor, targetGlyph, castingComp);
        }

        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState exitInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref, HexcasterComponent comp) {

        HexcasterCastingComponent castingComp = accessor.getComponent(ref,
                HexcasterCastingComponent.getComponentType());

        HexComponent draggedHex = castingComp.getDraggingHex();
        if (draggedHex == null) {
            return InteractionState.Finished;
        }

        EffectComponent hoveredGlyph = castingComp.getHoveredGlyph();
        if (hoveredGlyph != null) {
            try {
                float eyeHeight = 0f;
                ModelComponent modelComp = accessor.getComponent(ref, ModelComponent.getComponentType());
                eyeHeight = modelComp.getModel().getEyeHeight(ref, accessor);
                HexSpawner.MergeGlyphs(accessor, hoveredGlyph, draggedHex, eyeHeight);
                castingComp.getActiveHexes().remove(draggedHex.getSelfRef());
                castingComp.setDraggingHex(null);
                GlyphStyler.hoverGlyph(accessor, null, castingComp);
                return InteractionState.Finished;
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Error merging glyphs, dropping on ground instead");
            }
        }

        // Drop the glyph
        castingComp.setDraggingHex(null);

        GlyphStyler.hoverHex(accessor, null, castingComp);
        GlyphStyler.hoverGlyph(accessor, null, castingComp);

        Vector3d dropPos = GlyphMath.sphericalToCartesian(draggedHex.getRotation());
        Vector3f dropOffset = dropPos.toVector3f();
        draggedHex.setOffset(dropOffset);

        accessor.putComponent(draggedHex.getSelfRef(), MountedComponent.getComponentType(),
                new MountedComponent(draggedHex.getRootRef(),
                        new Vector3f(draggedHex.getOffset()),
                        MountController.Minecart));

        return InteractionState.Finished;
    }

    private static ModelParticle[] mergeParticles(ModelParticle[] a, ModelParticle[] b) {
        if (a == null || a.length == 0)
            return b;
        if (b == null || b.length == 0)
            return a;
        ModelParticle[] merged = new ModelParticle[a.length + b.length];
        System.arraycopy(a, 0, merged, 0, a.length);
        System.arraycopy(b, 0, merged, a.length, b.length);
        return merged;
    }

    private void cleanupEntities(CommandBuffer<EntityStore> accessor, HexcasterCastingComponent comp) {
        List<Ref<EntityStore>> activeGlyphs = comp.getActiveHexes();
        for (Ref<EntityStore> hex : activeGlyphs) {
            try {
                HexComponent hexComp = accessor.getComponent(hex, HexComponent.getComponentType());
                List<Ref<EntityStore>> childGlyphRefs = hexComp.getChildGlyphRefsList();

                for (Ref<EntityStore> childGlyphRef : childGlyphRefs) {
                    try {
                        accessor.tryRemoveEntity(childGlyphRef, RemoveReason.REMOVE);
                    } catch (Exception e) {
                        LOGGER.atSevere().withCause(e)
                                .log("Failed to despawn child glyph entity with ref: " + childGlyphRef);
                    }
                }
                accessor.tryRemoveEntity(hex, RemoveReason.REMOVE);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e)
                        .log("Failed to despawn hex entity");
            }
        }

        Ref<EntityStore> castingRootRef = comp.getCastingRootRef();
        if (castingRootRef != null) {
            try {
                accessor.tryRemoveEntity(castingRootRef, RemoveReason.REMOVE);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to despawn casting root entity");
            }
        }

        Ref<EntityStore> headAnchor = comp.getHeadAnchorRef();
        if (headAnchor != null && headAnchor.isValid()) {
            try {
                accessor.tryRemoveEntity(headAnchor, RemoveReason.REMOVE);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to despawn head anchor entity");
            }
        }
    }
}
