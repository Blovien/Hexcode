package com.riprod.hexcode.builtin.glyphs.scale;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
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
                float inverse = 1.0f / magnitude;
                EntityScaleComponent scaleComp = buffer.getComponent(
                        targetRef, EntityScaleComponent.getComponentType());
                if (scaleComp != null) {
                    scaleComp.setScale(scaleComp.getScale() * inverse);
                }
                PlayerSkinComponent skinComp = buffer.getComponent(
                        targetRef, PlayerSkinComponent.getComponentType());
                if (skinComp != null) {
                    skinComp.setNetworkOutdated();
                }
                BoundingBox box = buffer.getComponent(targetRef, BoundingBox.getComponentType());
                if (box != null) {
                    box.setBoundingBox(new Box(box.getBoundingBox()).scale(inverse));
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
