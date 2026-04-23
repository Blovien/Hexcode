package com.riprod.hexcode.builtin.glyphs.scale;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.scale.component.ScaleComponent;
import com.riprod.hexcode.builtin.glyphs.scale.component.ScaleTargetMarker;
import com.riprod.hexcode.builtin.glyphs.scale.style.ScaleStyle;
import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.state.NoState;
import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;
import com.riprod.hexcode.core.state.execution.component.HexColors;

public class ScaleConstructHandler implements ConstructHandler<NoState> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void onCleanup(HexStatus<NoState> status, ConstructTickContext ctx) {
        try {
            CommandBuffer<EntityStore> buffer = ctx.getBuffer();
            ScaleComponent scale = buffer.getComponent(ctx.getEntityRef(), ScaleComponent.getComponentType());
            if (scale != null) {
                Ref<EntityStore> targetRef = scale.getTargetRef();
                if (targetRef != null && targetRef.isValid()) {
                    EntityScaleComponent scaleComp = buffer.getComponent(
                            targetRef, EntityScaleComponent.getComponentType());
                    if (scaleComp != null) {
                        scaleComp.setScale(scale.getOriginalScale());
                    }
                    PlayerSkinComponent skinComp = buffer.getComponent(
                            targetRef, PlayerSkinComponent.getComponentType());
                    if (skinComp != null) {
                        skinComp.setNetworkOutdated();
                    }
                    Box originalBox = scale.getOriginalBoundingBox();
                    BoundingBox box = buffer.getComponent(targetRef, BoundingBox.getComponentType());
                    if (box != null && originalBox != null) {
                        box.setBoundingBox(originalBox);
                    }

                    HexColors colors = status.getHexContext() != null
                            ? status.getHexContext().getColors()
                            : null;
                    TransformComponent tc = buffer.getComponent(
                            targetRef, TransformComponent.getComponentType());
                    if (tc != null) {
                        ScaleStyle.renderRestore(tc.getPosition(), colors, buffer);
                    }

                    buffer.tryRemoveComponent(targetRef, ScaleTargetMarker.getComponentType());
                }
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] ScaleConstructHandler.onCleanup failed: %s", e.getMessage());
        }
    }
}
