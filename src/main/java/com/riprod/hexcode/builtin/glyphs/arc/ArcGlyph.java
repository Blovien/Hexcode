package com.riprod.hexcode.builtin.glyphs.arc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.builtin.glyphs.arc.style.ArcStyle;
import com.riprod.hexcode.builtin.glyphs.arc.utils.ArcUtils;
import com.riprod.hexcode.core.common.construct.system.HexConstructSpawner;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexDirectionUtil;
import com.riprod.hexcode.utils.HexVarUtil;

public class ArcGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public String getId() {
        return ID;
    }

    public static final String ID = "Arc";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.readSlot(ArcGlyphSlots.TARGET, hexContext);
        EntityVar entityVar = HexVarUtil.resolveEntityVar(targets, hexContext);
        if (entityVar == null) {
            LOGGER.atWarning().log("Arc: target must be Entity");
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Arc: target must be Entity");
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        Ref<EntityStore> originRef = entityVar.getRef(accessor);
        if (originRef == null || !originRef.isValid()) {
            LOGGER.atWarning().log("Arc: target ref unresolved");
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Arc: target ref unresolved");
            return;
        }

        List<String> branches = glyph.getNextLinks();
        if (branches.isEmpty()) {
            LOGGER.atWarning().log("Arc: no child branches");
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Arc: no child branches");
            return;
        }

        double maxJump = HexVarUtil.numberOrDefault(
                glyph.readSlot(ArcGlyphSlots.JUMP, hexContext), 15.0);
        double delay = HexVarUtil.numberOrDefault(
                glyph.readSlot(ArcGlyphSlots.DELAY, hexContext), 0.75);

        HexColors colors = hexContext.getColors();

        Set<UUID> visited = new HashSet<>();
        UUIDComponent casterUuid = accessor.getComponent(
                hexContext.getCasterRef(), UUIDComponent.getComponentType());
        if (casterUuid != null) visited.add(casterUuid.getUuid());

        UUIDComponent originUuid = accessor.getComponent(
                originRef, UUIDComponent.getComponentType());
        if (originUuid != null) visited.add(originUuid.getUuid());

        TransformComponent originTc = accessor.getComponent(
                originRef, TransformComponent.getComponentType());
        if (originTc == null) {
            LOGGER.atWarning().log("Arc: cast origin has no transform");
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Arc: cast origin has no transform");
            return;
        }
        Vector3d originPos = originTc.getPosition();

        Ref<EntityStore> firstJump = ArcUtils.getNextArcTarget(
                originPos, (float) maxJump, visited, accessor);
        if (firstJump == null) {
            LOGGER.atWarning().log("Arc: no nearby entity within %.1f to jump to", maxJump);
            ArcStyle.renderFizzle(accessor, originPos, colors);
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Arc: no nearby entity within range");
            return;
        }

        UUIDComponent firstJumpUuid = accessor.getComponent(
                firstJump, UUIDComponent.getComponentType());
        if (firstJumpUuid != null) visited.add(firstJumpUuid.getUuid());

        TransformComponent firstJumpTc = accessor.getComponent(
                firstJump, TransformComponent.getComponentType());
        Vector3d firstJumpPos = firstJumpTc != null ? firstJumpTc.getPosition() : originPos;

        World world = accessor.getExternalData().getWorld();
        ArcStyle.renderArc(accessor, world, originPos, firstJumpPos, colors);
        ArcStyle.renderHit(accessor, firstJumpPos, colors);

        ArcState state = new ArcState(glyph, new ArrayList<>(branches), visited,
                (float) maxJump, (float) delay);

        HexConstructSpawner.applyWithState(
                accessor, firstJump, hexContext, glyph, ArcGlyph.ID, state);

        LOGGER.atInfo().log("arc: applied to first-jump target (%d branches), uuid=%s",
                branches.size(), firstJumpUuid != null ? firstJumpUuid.getUuid() : "null");
    }
}
