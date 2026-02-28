package com.riprod.hexcode.core.crafting;

import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.crafting.component.PedestalAnchorComponent;
import com.riprod.hexcode.core.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.crafting.component.PedestalState;
import com.riprod.hexcode.core.crafting.system.PedestalSystem;
import com.riprod.hexcode.core.crafting.utils.SelectionUtils;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.hexes.component.HexComponent;
import com.riprod.hexcode.state.HexcodeManager;

public class CraftingSystem extends HexcodeManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float DETECTION_RANGE = 10.0f;
    private static final String HOVER_PARTICLE = "Glyph_Ambient";

    @Override
    public void firstTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        Ref<EntityStore> anchorRef = comp.consumePendingPedestalRef();

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());

        if (anchorRef != null && anchorRef.isValid()) {
            if (craftingComp == null) {
                craftingComp = new HexcasterCraftingComponent();
                buffer.putComponent(ref, HexcasterCraftingComponent.getComponentType(), craftingComp);
            }
            craftingComp.setPedestalRef(anchorRef);
            LOGGER.atInfo().log("crafting firstTick: set pedestalRef=%s", anchorRef);
        } else if (craftingComp != null && craftingComp.getPedestalRef() != null) {
            LOGGER.atInfo().log("crafting firstTick: reusing existing pedestalRef=%s", craftingComp.getPedestalRef());
        } else {
            LOGGER.atInfo().log("crafting firstTick: no pedestal ref available");
        }
    }

    @Override
    public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp != null) {
            craftingComp.setHoveredHexRef(null);
        }

        comp.clearCraftingState();
    }

    @Override
    public void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        PedestalBlockComponent pedestal = resolvePedestal(ref, buffer);
        if (pedestal == null) {
            return;
        }

        if (pedestal.getState() != PedestalState.ACTIVE) {
            return;
        }
        switch (pedestal.getState()) {
            case ACTIVE:
                tickActive(buffer, ref, pedestal);
                break;
            case CRAFTING:
                tickCrafting(buffer, ref, pedestal);
                break;
        }
    }

    private static void tickActive(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
            PedestalBlockComponent pedestal) {
        List<Ref<EntityStore>> previewRefs = pedestal.getHexPreviewRefs();

        if (previewRefs == null || previewRefs.isEmpty()) {
            return;
        }

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) {
            return;
        }

        List<Ref<EntityStore>> hexGlyphs = pedestal.getHexPreviewRefs();

        if (hexGlyphs.isEmpty())
            return;

        Ref<EntityStore> targetRef = SelectionUtils.getSmallestTarget(buffer, ref, hexGlyphs);
        Ref<EntityStore> hoveredRef = resolveHoveredPreview(targetRef, previewRefs, buffer);

        Ref<EntityStore> previousHovered = craftingComp.getHoveredHexRef();
        boolean changed = (hoveredRef == null) != (previousHovered == null)
                || (hoveredRef != null && !hoveredRef.equals(previousHovered));
        if (changed) {
            LOGGER.atInfo().log("crafting: hover changed from %s to %s", previousHovered, hoveredRef);
        }

        craftingComp.setHoveredHexRef(hoveredRef);

        if (hoveredRef != null && hoveredRef.isValid()) {
            TransformComponent transform = buffer.getComponent(hoveredRef, TransformComponent.getComponentType());
            if (transform != null) {
                Vector3d pos = transform.getPosition();
                ParticleUtil.spawnParticleEffect(HOVER_PARTICLE, pos, hoveredRef, List.of(ref), buffer);
            }
        }
    }

    private static void tickCrafting(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
            PedestalBlockComponent pedestal) {
        List<Ref<EntityStore>> previewRefs = pedestal.getHexPreviewRefs();

        if (previewRefs == null || previewRefs.isEmpty()) {
            return;
        }

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) {
            return;
        }

        List<Ref<EntityStore>> hexGlyphs = pedestal.getHexPreviewRefs();

        if (hexGlyphs.isEmpty())
            return;

        Ref<EntityStore> targetRef = SelectionUtils.getSmallestTarget(buffer, ref, hexGlyphs);
        Ref<EntityStore> hoveredRef = resolveHoveredPreview(targetRef, previewRefs, buffer);

        Ref<EntityStore> previousHovered = craftingComp.getHoveredHexRef();
        boolean changed = (hoveredRef == null) != (previousHovered == null)
                || (hoveredRef != null && !hoveredRef.equals(previousHovered));
        if (changed) {
            LOGGER.atInfo().log("crafting: hover changed from %s to %s", previousHovered, hoveredRef);
        }

        craftingComp.setHoveredHexRef(hoveredRef);

        if (hoveredRef != null && hoveredRef.isValid()) {
            TransformComponent transform = buffer.getComponent(hoveredRef, TransformComponent.getComponentType());
            if (transform != null) {
                Vector3d pos = transform.getPosition();
                ParticleUtil.spawnParticleEffect(HOVER_PARTICLE, pos, hoveredRef, List.of(ref), buffer);
            }
        }
    }

    @Override
    public InteractionState enterInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
            CommandBuffer<EntityStore> buffer) {

        PedestalBlockComponent pedestal = resolvePedestal(ref, buffer);
        if (pedestal == null) {
            return InteractionState.Failed;
        }

        if (pedestal.getState() != PedestalState.ACTIVE) {
            return InteractionState.NotFinished;
        }

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) {
            return InteractionState.Failed;
        }

        Ref<EntityStore> hoveredRef = craftingComp.getHoveredHexRef();
        LOGGER.atInfo().log("crafting enterInteraction: hoveredRef=%s", hoveredRef);
        if (hoveredRef == null || !hoveredRef.isValid()) {
            return InteractionState.NotFinished;
        }

        HexComponent hexComp = buffer.getComponent(hoveredRef, HexComponent.getComponentType());
        if (hexComp == null || hexComp.getHex() == null) {
            return InteractionState.NotFinished;
        }

        pedestal.setActiveHex(hexComp.getHex());
        PedestalSystem.ActivateHexSelection(buffer, pedestal, hoveredRef);
        craftingComp.setHoveredHexRef(null);

        return InteractionState.Finished;
    }

    @Override
    public InteractionState tickInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
            CommandBuffer<EntityStore> buffer) {
        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState exitInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
            CommandBuffer<EntityStore> buffer) {
        return InteractionState.NotFinished;
    }

    @Override
    public void onPlayerJoin(Holder<EntityStore> holder, HexcasterComponent comp) {
    }

    @Override
    public void onPlayerLeave(PlayerRef playerRef) {
    }

    private static Ref<EntityStore> resolveHoveredPreview(Ref<EntityStore> targetRef,
            List<Ref<EntityStore>> previewRefs, CommandBuffer<EntityStore> buffer) {
        if (targetRef == null || !targetRef.isValid()) {
            return null;
        }

        for (Ref<EntityStore> previewRef : previewRefs) {
            if (previewRef != null && previewRef.equals(targetRef)) {
                return targetRef;
            }
        }

        GlyphComponent glyphComp = buffer.getComponent(targetRef, GlyphComponent.getComponentType());
        if (glyphComp != null) {
            Ref<EntityStore> hexRef = glyphComp.getHexRef();
            if (hexRef != null && hexRef.isValid()) {
                for (Ref<EntityStore> previewRef : previewRefs) {
                    if (previewRef != null && previewRef.equals(hexRef)) {
                        return hexRef;
                    }
                }
            }
        }

        return null;
    }

    private PedestalBlockComponent resolvePedestal(Ref<EntityStore> playerRef,
            CommandBuffer<EntityStore> buffer) {

        HexcasterCraftingComponent craftingComp = buffer.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) {
            return null;
        }

        Ref<EntityStore> anchorRef = craftingComp.getPedestalRef();
        if (anchorRef == null || !anchorRef.isValid()) {
            return null;
        }

        PedestalAnchorComponent anchor = buffer.getComponent(anchorRef,
                PedestalAnchorComponent.getComponentType());
        if (anchor == null || anchor.getPedestalLoc() == null) {
            return null;
        }

        Vector3i pos = anchor.getPedestalLoc();
        return BlockModule.getComponent(
                PedestalBlockComponent.getComponentType(),
                buffer.getExternalData().getWorld(),
                pos.getX(), pos.getY(), pos.getZ());
    }
}
