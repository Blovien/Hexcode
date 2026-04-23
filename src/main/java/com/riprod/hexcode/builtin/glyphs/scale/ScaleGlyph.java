package com.riprod.hexcode.builtin.glyphs.scale;

import java.util.List;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.scale.component.ScaleComponent;
import com.riprod.hexcode.builtin.glyphs.scale.component.ScaleTargetMarker;
import com.riprod.hexcode.builtin.glyphs.scale.style.ScaleStyle;
import com.riprod.hexcode.core.common.construct.component.HexEffectsComponent;
import com.riprod.hexcode.core.common.construct.system.HexConstructSpawner;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.utils.SpellVarUtil;

public class ScaleGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
public String getId() { return ID; };

public static final String ID = "Scale";
    public static final String HANDLER_ID = "scale";
    private static final String MODEL_ID = "Scale";

    private static final double DEFAULT_MAGNITUDE = 2.0;
    private static final double MIN_MAGNITUDE = 0.25;
    private static final double MAX_MAGNITUDE = 4.0;
    private static final double DEFAULT_DURATION = 5.0;
    private static final double MIN_DURATION = 0.1;
    private static final double MAX_DURATION = 60.0;

    private static final Vector3f MOUNT_OFFSET = new Vector3f(0f, 2.5f, 0f);

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.readSlot(ScaleGlyphSlots.TARGET, hexContext);
        EntityVar entityVar = SpellVarUtil.resolveEntityVar(targets, hexContext);
        if (entityVar == null) {
            LOGGER.atWarning().log("scale: no entity target provided");
            HexExecuter.fail(hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        Ref<EntityStore> targetRef = entityVar.getRef(accessor);
        if (targetRef == null || !targetRef.isValid()) {
            LOGGER.atWarning().log("scale: target ref invalid");
            HexExecuter.fail(hexContext);
            return;
        }

        double magnitude = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot(ScaleGlyphSlots.MAGNITUDE, hexContext), DEFAULT_MAGNITUDE),
                MIN_MAGNITUDE, MAX_MAGNITUDE);
        double duration = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot(ScaleGlyphSlots.DURATION, hexContext), DEFAULT_DURATION),
                MIN_DURATION, MAX_DURATION);

        List<String> nextLinks = glyph.getNextLinks();

        try {
            ScaleTargetMarker existingMarker = accessor.getComponent(
                    targetRef, ScaleTargetMarker.getComponentType());
            if (existingMarker != null) {
                Ref<EntityStore> existingConstruct = existingMarker.getTriggerRef();
                if (existingConstruct != null && existingConstruct.isValid()) {
                    HexEffectsComponent existingHc = accessor.getComponent(
                            existingConstruct, HexEffectsComponent.getComponentType());
                    if (existingHc != null) {
                        float currentRemaining = existingHc.getRemainingLifetime();
                        existingHc.setRemainingLifetime(
                                Math.max(currentRemaining, (float) duration));
                    }
                    return;
                }
                accessor.removeComponent(targetRef, ScaleTargetMarker.getComponentType());
            }

            EntityScaleComponent scaleComp = accessor.getComponent(
                    targetRef, EntityScaleComponent.getComponentType());
            BoundingBox box = accessor.getComponent(targetRef, BoundingBox.getComponentType());

            boolean hadEntityScaleBefore = scaleComp != null;
            float originalScale = hadEntityScaleBefore ? scaleComp.getScale() : 1.0f;
            Box originalBox = box != null ? new Box(box.getBoundingBox()) : null;

            float newScale = originalScale * (float) magnitude;
            if (hadEntityScaleBefore) {
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
            if (box != null && originalBox != null) {
                Box scaled = new Box(originalBox).scale((float) magnitude);
                box.setBoundingBox(scaled);
            }

            Vector3d spawnPos;
            TransformComponent targetTransform = accessor.getComponent(
                    targetRef, TransformComponent.getComponentType());
            if (targetTransform != null) {
                spawnPos = new Vector3d(targetTransform.getPosition());
            } else {
                spawnPos = new Vector3d();
            }

            ScaleComponent scaleMarker = new ScaleComponent(
                    targetRef, originalScale, originalBox, hadEntityScaleBefore);

            Holder<EntityStore> holder = HexConstructSpawner.create(
                    accessor, hexContext, glyph,
                    HANDLER_ID, (float) duration, 0f,
                    null, null, nextLinks,
                    spawnPos);

            holder.addComponent(ScaleComponent.getComponentType(), scaleMarker);
            holder.addComponent(MountedComponent.getComponentType(),
                    new MountedComponent(targetRef, MOUNT_OFFSET, MountController.Minecart));

            ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(MODEL_ID);
            if (modelAsset != null) {
                Model model = Model.createUnitScaleModel(modelAsset);
                holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                holder.addComponent(PersistentModel.getComponentType(),
                        new PersistentModel(model.toReference()));
            }

            Ref<EntityStore> constructRef = accessor.addEntity(holder, AddReason.SPAWN);
            accessor.addComponent(targetRef, ScaleTargetMarker.getComponentType(),
                    new ScaleTargetMarker(constructRef));

            ScaleStyle.renderApply(spawnPos, hexContext.getColors(), accessor);

            RootGlyph rootGlyph = accessor.getComponent(
                    hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
            if (rootGlyph != null) {
                rootGlyph.addDependent(constructRef);
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] scale: apply failed: %s", e.getMessage());
            HexExecuter.fail(hexContext);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
