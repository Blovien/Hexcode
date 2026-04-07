package com.riprod.hexcode.core.state.crafting.handlers.node.Container;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.state.crafting.handlers.node.Slot.SlotNodeHandler;
import com.riprod.hexcode.core.common.glyphs.utils.CreateGlyph;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hexes.utils.CreateHex;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.api.event.EnterCraftingModeEvent;
import com.riprod.hexcode.api.event.HexcodeEvents;
import com.riprod.hexcode.core.common.obelisk.system.ObeliskDispatcher;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;

import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.common.pedestal.events.PedestalSystem;
import com.riprod.hexcode.core.common.pedestal.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.riprod.hexcode.core.state.casting.utils.GlyphStyler;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.component.CraftingData;
import com.riprod.hexcode.core.state.crafting.constants.CraftingColors;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.entity.GlyphSpawner;
import com.riprod.hexcode.core.state.crafting.entity.PedestalEntity;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeInterface;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeRouter;
import com.riprod.hexcode.core.state.crafting.handlers.node.Anchor.AnchorNodeHandler;
import com.riprod.hexcode.core.state.crafting.utils.CraftingDataUtil;
import com.riprod.hexcode.utils.CleanupUtils;
import com.riprod.hexcode.utils.GlyphMath;
import com.hypixel.hytale.logger.HytaleLogger;

public class ContainerNodeHandler implements NodeInterface {
    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public static final ContainerNodeHandler INSTANCE = new ContainerNodeHandler();

    private static final float GLYPH_DISPLAY_DISTANCE = 1.0f;
    private static final float PEDESTAL_GLYPH_PITCH = (float) (-Math.PI / 2);
    private static final Box PREVIEW_BOUNDING_BOX = new Box(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25);

    public Ref<EntityStore> spawnContainer(CommandBuffer<EntityStore> accessor, Hex hex,
            Ref<EntityStore> anchorRef, Vector3d anchorPos, Vector3f offset, Ref<EntityStore> playerRef) {

        Vector3d globalPos = new Vector3d(anchorPos.x + offset.x, anchorPos.y + offset.y, anchorPos.z + offset.z);

        boolean isEmpty = hex == null;

        Holder<EntityStore> holder;
        HexComponent hexComponent = new HexComponent(hex); // will not be used if hex is null, but needed later
        if (!isEmpty) {
            hexComponent.setRootRef(anchorRef);
            hexComponent.setParentRef(null);
            hexComponent.setOffset(offset);

            holder = CreateHex.createHexHolder(accessor, hexComponent, globalPos);
        } else {
            holder = EntityStore.REGISTRY.newHolder();
            holder.addComponent(TransformComponent.getComponentType(),
                    new TransformComponent(globalPos, new Vector3f(0, 0, 0)));
            holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());
            holder.addComponent(UUIDComponent.getComponentType(),
                    new UUIDComponent(UUID.randomUUID()));
            int networkId = accessor.getExternalData().takeNextNetworkId();
            holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));
        }

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
        holder.addComponent(HoverableComponent.getComponentType(), new HoverableComponent(HoverableType.NODE));
        holder.addComponent(NodeComponent.getComponentType(), new NodeComponent(anchorRef, NodeType.Container));
        holder.addComponent(DebugComponent.getComponentType(),
                new DebugComponent(DebugShape.Sphere, isEmpty ? CraftingColors.EMPTY_SLOT : CraftingColors.FILLED_SLOT,
                        0.5, 2.0f));

        Ref<EntityStore> hexRef = CreateHex.createEntity(accessor, holder);
        if (isEmpty || hex == null) {
            // Early isEmpty break as it doesn't have to spawn the rest of the glyph stuff
            return hexRef;
        }

        hexComponent.setSelfRef(hexRef);

        int numGlyphs = (int) hex.getGlyphs().stream()
                .filter(glyph -> glyph != null)
                .count();

        float scaleMultiplier = 1 + (numGlyphs * GlyphStyler.SCALE_PER_GLYPH);

        String firstGlyphId = hex.getFirstGlyphId();
        Glyph firstGlyph = firstGlyphId != null ? hex.get(firstGlyphId) : null;
        if (firstGlyph == null)
            return hexRef;
        GlyphComponent firstGlyphComponent = new GlyphComponent(firstGlyph.clone());

        Vector3f glyphRot = new Vector3f(PEDESTAL_GLYPH_PITCH, 0, GLYPH_DISPLAY_DISTANCE);
        firstGlyphComponent.setHexRef(hexRef);
        firstGlyphComponent.setParentRef(hexRef);
        firstGlyphComponent.setOffset(Vector3f.ZERO);
        firstGlyphComponent.setRotation(glyphRot);
        firstGlyphComponent.setScale(scaleMultiplier);
        hexComponent.setScale(scaleMultiplier);

        GlyphSpawner.spawnGlyphs(accessor, hexComponent, firstGlyphComponent, globalPos, glyphRot, playerRef);
        accessor.putComponent(hexRef, HexComponent.getComponentType(), hexComponent);
        return hexRef;
    }

    @Override
    public InteractionState enter(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {
        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(playerRef, accessor);
        if (pedestal == null) {
            logger.atWarning().log("container enter: pedestal is null");
            return InteractionState.Failed;
        }

        CraftingData playerData = pedestal.getCraftingDataComponent();
        if (playerData == null) {
            logger.atWarning().log("container enter: playerData is null");
            return InteractionState.Failed;
        }

        if (!playerData.isOwner(playerRef)) {
            logger.atWarning().log("container enter: not owner — ownerRef=%s, playerRef=%s, same=%s",
                    playerData.getOwnerRef(), playerRef, playerData.getOwnerRef() == playerRef);
            return InteractionState.Failed;
        }

        if (playerData.getState() != PedestalState.SELECTING) {
            logger.atWarning().log("container enter: wrong state=%s", playerData.getState());
            return InteractionState.Failed;
        }

        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null)
            return InteractionState.Failed;

        HexComponent hexComp = accessor.getComponent(node, HexComponent.getComponentType());
        boolean isFilled = hexComp != null && hexComp.getHex() != null;

        Hex originalHex = null;

        if (isFilled) {
            int slotIndex = playerData.getHexPreviewRefs().indexOf(node);
            playerData.setActiveSlotIndex(slotIndex);

            Map<String, Ref<EntityStore>> oldChildren = hexComp.getChildGlyphRefs();
            if (oldChildren != null) {
                for (Ref<EntityStore> childRef : oldChildren.values()) {
                    if (childRef == null || !childRef.isValid()) continue;
                    SlotNodeHandler.INSTANCE.despawnSlotsForGlyph(accessor, childRef);
                    accessor.tryRemoveEntity(childRef, RemoveReason.REMOVE);
                }
                oldChildren.clear();
            }

            List<Hex> bookHexes = playerData.getHexes();
            if (slotIndex >= 0 && slotIndex < bookHexes.size()) {
                Hex hex = bookHexes.get(slotIndex).clone();
                HexComponent freshComp = new HexComponent(hex);
                freshComp.setSelfRef(node);
                freshComp.setRootRef(hexComp.getRootRef());
                accessor.putComponent(node, HexComponent.getComponentType(), freshComp);
                originalHex = hex;
            }

        } else {
            playerData.setActiveSlotIndex(playerData.getHexes().size());

            originalHex = new Hex();
            HexComponent newHexComp = new HexComponent(originalHex);

            newHexComp.setSelfRef(node);
            accessor.putComponent(node, HexComponent.getComponentType(), newHexComp);

        }

        PedestalSystem.enterCrafting(accessor, playerRef, pedestal, node);
        HexcodeEvents.fire(new EnterCraftingModeEvent(playerRef, originalHex, pedestal));
        ObeliskDispatcher.dispatchEnterCrafting(accessor, pedestal, playerRef);
        craftingComp.setHoveredRef(null);
        accessor.removeComponent(node, DebugComponent.getComponentType());

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(pedestal.getCraftingDataComponent().getPedestalLocation());
        Vector3d activePos = new Vector3d(
                anchorPos.x + PedestalSystem.ACTIVE_HEX_OFFSET.x,
                anchorPos.y + PedestalSystem.ACTIVE_HEX_OFFSET.y,
                anchorPos.z + PedestalSystem.ACTIVE_HEX_OFFSET.z);

        Ref<EntityStore> rootNodeRef = AnchorNodeHandler.INSTANCE.spawnNode(accessor, originalHex,
                node, activePos, playerRef);
        playerData.setAnchorNodeRef(rootNodeRef);

        return InteractionState.Finished;
    }

    @Override
    public InteractionState tick(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {

        return InteractionState.Finished;
    }

    @Override
    public InteractionState exit(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {

        return InteractionState.Finished;
    }

    @Override
    public InteractionState click(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {

        return InteractionState.Finished;
    }

    @Override
    public InteractionState ability(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            InteractionType inputType, Ref<EntityStore> playerRef) {

        return InteractionState.Finished;
    }

    @Override
    public void despawn(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'despawn'");
    }

    @Override
    public void hover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        DebugComponent debug = accessor.getComponent(nodeRef, DebugComponent.getComponentType());
        if (debug == null)
            return;
        debug.setScaleMultiplier(1.3f);
        debug.setIntervalMultiplier(0.25f);
        debug.setFadeMultiplier(0.25f);
        debug.setTimer(0);
    }

    @Override
    public void unhover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        DebugComponent debug = accessor.getComponent(nodeRef, DebugComponent.getComponentType());
        if (debug == null)
            return;
        debug.resetScaleMultiplier();
        debug.resetFadeMultipler();
        debug.resetIntervalMultiplier();
    }
}
