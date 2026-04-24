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
import com.riprod.hexcode.utils.SpellVarUtil;

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
        EntityVar entityVar = SpellVarUtil.resolveEntityVar(targets, hexContext);
        if (entityVar == null) {
            LOGGER.atWarning().log("arc: no entity target provided");
            HexExecuter.fail(hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        Ref<EntityStore> originRef = entityVar.getRef(accessor);
        if (originRef == null || !originRef.isValid()) {
            LOGGER.atWarning().log("arc: cast origin ref invalid");
            HexExecuter.fail(hexContext);
            return;
        }

        List<String> branches = glyph.getNextLinks();
        if (branches.isEmpty()) {
            LOGGER.atInfo().log("arc: no child branches, nothing to do");
            HexExecuter.fail(hexContext);
            return;
        }

        double maxJump = SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot(ArcGlyphSlots.JUMP, hexContext), 15.0);
        double delay = SpellVarUtil.resolveNumberOrDefault(
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
            LOGGER.atWarning().log("arc: cast origin has no transform");
            HexExecuter.fail(hexContext);
            return;
        }
        Vector3d originPos = originTc.getPosition();

        Ref<EntityStore> firstJump = ArcUtils.getNextArcTarget(
                originPos, (float) maxJump, visited, accessor);
        if (firstJump == null) {
            LOGGER.atInfo().log("arc: no nearby entity within %.1f to jump to, fizzling", maxJump);
            ArcStyle.renderFizzle(accessor, originPos, colors);
            HexExecuter.fail(hexContext);
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
