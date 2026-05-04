package com.riprod.hexcode.builtin.glyphs.scale;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.builtin.glyphs.scale.style.ScaleStyle;
import com.riprod.hexcode.core.common.construct.system.HexConstructSpawner;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.HexVarUtil;

public class ScaleGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public String getId() {
        return ID;
    }

    public static final String ID = "Scale";

    private static final double DEFAULT_MAGNITUDE = 2.0;
    private static final double MIN_MAGNITUDE = 0.01;
    private static final double MAX_MAGNITUDE = 32.0;
    private static final double DEFAULT_DURATION = 5.0;
    private static final double MIN_DURATION = 0.1;

    private static final double K_GROWTH = 0.5;

    private static final Vector3f MOUNT_OFFSET = new Vector3f(0f, 2.5f, 0f);

    @Override
    public boolean consumeVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null)
            return true;
        int repeatCount = tracker.getGlyphUsage(glyph.getId());
        float baseCost = VolatilityTracker.computeGlyphCost(glyph, repeatCount);
        if (baseCost <= 0)
            return true;

        double magnitude = clamp(HexVarUtil.numberOrDefault(
                glyph.readSlot(ScaleGlyphSlots.MAGNITUDE, hexContext), DEFAULT_MAGNITUDE),
                MIN_MAGNITUDE, MAX_MAGNITUDE);

        float currentScale = readCurrentScale(glyph, hexContext);
        double resultScale = clamp(currentScale * magnitude, MIN_MAGNITUDE, MAX_MAGNITUDE);

        double factor = Math.expm1(K_GROWTH * (resultScale - 1.0));
        float cost = (float) Math.max(baseCost, baseCost * factor);

        return tracker.consumeVolatility(cost);
    }

    private float readCurrentScale(Glyph glyph, HexContext hexContext) {
        try {
            HexVar targets = glyph.readSlot(ScaleGlyphSlots.TARGET, hexContext);
            EntityVar entityVar = HexVarUtil.resolveEntityVar(targets, hexContext);
            if (entityVar == null) return 1.0f;
            CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
            Ref<EntityStore> targetRef = entityVar.getRef(accessor);
            if (targetRef == null || !targetRef.isValid()) return 1.0f;
            ModelComponent modelComp = accessor.getComponent(
                    targetRef, ModelComponent.getComponentType());
            if (modelComp == null || modelComp.getModel() == null) return 1.0f;
            return modelComp.getModel().getScale();
        } catch (Exception e) {
            return 1.0f;
        }
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.readSlot(ScaleGlyphSlots.TARGET, hexContext);
        EntityVar entityVar = HexVarUtil.resolveEntityVar(targets, hexContext);
        if (entityVar == null) {
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Target must be a creature");
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        Ref<EntityStore> targetRef = entityVar.getRef(accessor);
        if (targetRef == null || !targetRef.isValid()) {
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Target is no longer available");
            return;
        }

        double magnitude = clamp(HexVarUtil.numberOrDefault(
                glyph.readSlot(ScaleGlyphSlots.MAGNITUDE, hexContext), DEFAULT_MAGNITUDE),
                MIN_MAGNITUDE, MAX_MAGNITUDE);

        float durationSeconds = (float) Math.max(MIN_DURATION, HexVarUtil.numberOrDefault(
                glyph.readSlot(ScaleGlyphSlots.DURATION, hexContext), DEFAULT_DURATION));

        try {
            ModelComponent modelComp = accessor.getComponent(
                    targetRef, ModelComponent.getComponentType());
            if (modelComp == null || modelComp.getModel() == null) {
                HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                        "Target has no model");
                return;
            }
            Model currentModel = modelComp.getModel();
            String assetId = currentModel.getModelAssetId();
            ModelAsset asset = assetId != null ? ModelAsset.getAssetMap().getAsset(assetId) : null;
            if (asset == null) {
                HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                        "Cannot resolve target model asset");
                return;
            }

            float currentScale = currentModel.getScale();
            float newScale = (float) clamp(currentScale * magnitude, MIN_MAGNITUDE, MAX_MAGNITUDE);

            applyScaledModel(accessor, targetRef, asset, newScale);

            PlayerSkinComponent targetSkin = accessor.getComponent(
                    targetRef, PlayerSkinComponent.getComponentType());
            if (targetSkin != null) {
                targetSkin.setNetworkOutdated();
            }

            Vector3d spawnPos;
            TransformComponent targetTransform = accessor.getComponent(
                    targetRef, TransformComponent.getComponentType());
            if (targetTransform != null) {
                spawnPos = new Vector3d(targetTransform.getPosition());
            } else {
                spawnPos = new Vector3d();
            }

            Ref<EntityStore> visualRef = magnitude >= 1.0
                    ? spawnVisual(accessor, spawnPos, targetRef, hexContext)
                    : null;

            ScaleState state = new ScaleState((float) magnitude, visualRef, durationSeconds, assetId);
            HexConstructSpawner.applyWithState(
                    accessor, targetRef, hexContext, glyph, ScaleGlyph.ID, state);

            ScaleStyle.renderApply(spawnPos, hexContext, accessor);

            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
        } catch (Exception e) {
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Cannot apply scale", e);
        }
    }

    public static void applyScaledModel(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> targetRef, ModelAsset asset, float scale) {
        // snap near-rest scales exactly to 1.0 to avoid float drift on full revert
        float effectiveScale = Math.abs(scale - 1.0f) < 1e-4f ? 1.0f : scale;
        Model scaled = Model.createScaledModel(asset, effectiveScale);
        accessor.putComponent(targetRef, ModelComponent.getComponentType(),
                new ModelComponent(scaled));

        // for players the visible mesh is driven by EntityScaleComponent;
        // keep it in lockstep with the model bake so eye-height, bbox, and visual size agree
        EntityScaleComponent existing = accessor.getComponent(
                targetRef, EntityScaleComponent.getComponentType());
        if (existing != null) {
            if (existing.getScale() != effectiveScale) {
                existing.setScale(effectiveScale);
            }
        } else {
            accessor.putComponent(targetRef, EntityScaleComponent.getComponentType(),
                    new EntityScaleComponent(effectiveScale));
        }
    }

    private Ref<EntityStore> spawnVisual(CommandBuffer<EntityStore> accessor,
            Vector3d spawnPos, Ref<EntityStore> targetRef, HexContext hexContext) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(spawnPos, new Vector3f()));
        holder.ensureComponent(UUIDComponent.getComponentType());
        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(accessor.getExternalData().takeNextNetworkId()));
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());
        holder.addComponent(MountedComponent.getComponentType(),
                new MountedComponent(targetRef, MOUNT_OFFSET, MountController.Minecart));

        String modelId = ScaleStyle.resolveModelId(hexContext);
        ModelAsset modelAsset = modelId != null ? ModelAsset.getAssetMap().getAsset(modelId) : null;
        if (modelAsset != null) {
            Model model = Model.createUnitScaleModel(modelAsset);
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(PersistentModel.getComponentType(),
                    new PersistentModel(model.toReference()));
        }

        return accessor.addEntity(holder, AddReason.SPAWN);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
