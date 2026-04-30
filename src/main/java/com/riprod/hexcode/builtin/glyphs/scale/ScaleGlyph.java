package com.riprod.hexcode.builtin.glyphs.scale;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
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
import com.riprod.hexcode.utils.HexDirectionUtil;
import com.riprod.hexcode.utils.HexVarUtil;

public class ScaleGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public String getId() {
        return ID;
    }

    public static final String ID = "Scale";
    private static final String MODEL_ID = "Scale";

    private static final double DEFAULT_MAGNITUDE = 2.0;
    private static final double MIN_MAGNITUDE = 0.01;
    private static final double MAX_MAGNITUDE = 32.0;
    private static final double DEFAULT_DURATION = 5.0;
    private static final double MIN_DURATION = 0.1;

    private static final Vector3f MOUNT_OFFSET = new Vector3f(0f, 2.5f, 0f);

    @Override
    public boolean consumeVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null)
            return true;
        int repeatCount = tracker.getGlyphUsage(glyph.getId());
        float cost = VolatilityTracker.computeGlyphCost(glyph, repeatCount);
        if (cost <= 0)
            return true;

        double magnitude = clamp(HexVarUtil.numberOrDefault(
                glyph.readSlot(ScaleGlyphSlots.MAGNITUDE, hexContext), DEFAULT_MAGNITUDE),
                MIN_MAGNITUDE, MAX_MAGNITUDE);

        cost *= magnitude;

        boolean consumed = tracker.consumeVolatility(cost);
        return consumed;
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
            EntityScaleComponent scaleComp = accessor.getComponent(
                    targetRef, EntityScaleComponent.getComponentType());
            BoundingBox box = accessor.getComponent(targetRef, BoundingBox.getComponentType());

            float currentScale = scaleComp != null ? scaleComp.getScale() : 1.0f;
            float newScale = currentScale * (float) magnitude;
            if (scaleComp != null) {
                scaleComp.setScale(newScale);
            } else {
                accessor.addComponent(targetRef, EntityScaleComponent.getComponentType(),
                        new EntityScaleComponent(newScale));
            }
            PlayerSkinComponent targetSkin = accessor.getComponent(
                    targetRef, PlayerSkinComponent.getComponentType());
            if (targetSkin != null) {
                targetSkin.setNetworkOutdated();
            }
            if (box != null) {
                box.setBoundingBox(new Box(box.getBoundingBox()).scale((float) magnitude));
            }

            Vector3d spawnPos;
            TransformComponent targetTransform = accessor.getComponent(
                    targetRef, TransformComponent.getComponentType());
            if (targetTransform != null) {
                spawnPos = new Vector3d(targetTransform.getPosition());
            } else {
                spawnPos = new Vector3d();
            }

            Ref<EntityStore> visualRef = spawnVisual(accessor, spawnPos, targetRef);

            ScaleState state = new ScaleState((float) magnitude, visualRef, durationSeconds);
            HexConstructSpawner.applyWithState(
                    accessor, targetRef, hexContext, glyph, ScaleGlyph.ID, state);

            ScaleStyle.renderApply(spawnPos, hexContext, accessor);

            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
        } catch (Exception e) {
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Cannot apply scale", e);
        }
    }

    private Ref<EntityStore> spawnVisual(CommandBuffer<EntityStore> accessor,
            Vector3d spawnPos, Ref<EntityStore> targetRef) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(spawnPos, new Vector3f()));
        holder.ensureComponent(UUIDComponent.getComponentType());
        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(accessor.getExternalData().takeNextNetworkId()));
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());
        holder.addComponent(MountedComponent.getComponentType(),
                new MountedComponent(targetRef, MOUNT_OFFSET, MountController.Minecart));

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(MODEL_ID);
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
