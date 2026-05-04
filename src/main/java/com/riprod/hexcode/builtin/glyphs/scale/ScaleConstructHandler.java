package com.riprod.hexcode.builtin.glyphs.scale;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.scale.style.ScaleStyle;
import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;

public class ScaleConstructHandler implements ConstructHandler<ScaleState> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public boolean onTick(float dt, HexStatus<ScaleState> status, ConstructTickContext ctx) {
        ScaleState state = status.getState();
        if (state == null) return true;
        if (state.isExpired()) return true;
        state.tick(dt);
        return !drainSustain(dt, status);
    }

    @Override
    public void onCleanup(HexStatus<ScaleState> status, ConstructTickContext ctx) {
        try {
            ScaleState state = status.getState();
            if (state == null) return;

            CommandBuffer<EntityStore> buffer = ctx.getBuffer();
            Ref<EntityStore> targetRef = ctx.getEntityRef();
            float magnitude = state.getAppliedMagnitude();
            if (targetRef != null && targetRef.isValid() && magnitude != 0f) {
                ModelComponent modelComp = buffer.getComponent(
                        targetRef, ModelComponent.getComponentType());
                if (modelComp != null && modelComp.getModel() != null) {
                    String assetId = state.getModelAssetId() != null
                            ? state.getModelAssetId()
                            : modelComp.getModel().getModelAssetId();
                    ModelAsset asset = assetId != null
                            ? ModelAsset.getAssetMap().getAsset(assetId)
                            : null;
                    if (asset != null) {
                        float revertedScale = modelComp.getModel().getScale() / magnitude;
                        ScaleGlyph.applyScaledModel(buffer, targetRef, asset, revertedScale);
                    }
                }

                PlayerSkinComponent skinComp = buffer.getComponent(
                        targetRef, PlayerSkinComponent.getComponentType());
                if (skinComp != null) {
                    skinComp.setNetworkOutdated();
                }

                TransformComponent tc = buffer.getComponent(
                        targetRef, TransformComponent.getComponentType());
                if (tc != null) {
                    ScaleStyle.renderRestore(tc.getPosition(), status.getHexContext(), buffer);
                }
            }

            Ref<EntityStore> visualRef = state.getVisualRef();
            if (visualRef != null && visualRef.isValid()) {
                buffer.tryRemoveEntity(visualRef, RemoveReason.REMOVE);
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] ScaleConstructHandler.onCleanup failed: %s", e.getMessage());
        }
    }
}
