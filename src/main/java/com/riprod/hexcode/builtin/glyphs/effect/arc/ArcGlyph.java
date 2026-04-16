package com.riprod.hexcode.builtin.glyphs.effect.arc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.arc.component.ArcComponent;
import com.riprod.hexcode.builtin.glyphs.effect.arc.style.ArcStyle;
import com.riprod.hexcode.builtin.glyphs.effect.arc.utils.ArcUtils;
import com.riprod.hexcode.core.common.construct.HexConstructSpawner;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.utils.SpellVarUtil;

public class ArcGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Arc";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.readSlot("target", hexContext);
        EntityVar entityVar = SpellVarUtil.resolveEntityVar(targets, hexContext);
        if (entityVar == null) {
            LOGGER.atWarning().log("arc: no entity target provided");
            Executor.fail(hexContext);
            return;
        }

        Ref<EntityStore> initialTarget = entityVar.getRef(hexContext.getAccessor());
        if (initialTarget == null || !initialTarget.isValid()) {
            LOGGER.atWarning().log("arc: initial target ref invalid");
            Executor.fail(hexContext);
            return;
        }

        List<String> branches = glyph.getNextLinks();
        if (branches.isEmpty()) {
            LOGGER.atInfo().log("arc: no child branches, nothing to do");
            Executor.fail(hexContext);
            return;
        }

        double maxJump = SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("jump", hexContext), 15.0);
        double delay = SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("delay", hexContext), 0.75);

        Set<UUID> visited = new HashSet<>();

        UUIDComponent casterUuid = hexContext.getAccessor().getComponent(
                hexContext.getCasterRef(), UUIDComponent.getComponentType());
        if (casterUuid != null) {
            visited.add(casterUuid.getUuid());
        }

        UUIDComponent targetUuid = hexContext.getAccessor().getComponent(
                initialTarget, UUIDComponent.getComponentType());
        if (targetUuid != null) {
            visited.add(targetUuid.getUuid());
        }

        TransformComponent casterTc = hexContext.getAccessor().getComponent(
                hexContext.getCasterRef(), TransformComponent.getComponentType());
        TransformComponent targetTc = hexContext.getAccessor().getComponent(
                initialTarget, TransformComponent.getComponentType());

        HexColors colors = hexContext.getColors();

        if (casterTc != null && targetTc != null) {
            World world = hexContext.getAccessor().getExternalData().getWorld();
            ArcStyle.renderArc(hexContext.getAccessor(), world,
                    casterTc.getPosition(), targetTc.getPosition(), colors);
        }

        ArcUtils.applyShockEffect(hexContext.getAccessor(), initialTarget);
        if (targetTc != null) {
            ArcStyle.renderHit(hexContext.getAccessor(), targetTc.getPosition(), colors);
        }

        Vector3d spawnPos = targetTc != null ? targetTc.getPosition() : new Vector3d();

        Holder<EntityStore> holder = HexConstructSpawner.create(
                hexContext.getAccessor(), hexContext, glyph,
                "arc", -1, 0,
                null, branches, null,
                spawnPos);

        ArcComponent arcComponent = new ArcComponent(
                glyph, new ArrayList<>(branches), visited,
                (float) maxJump, (float) delay);
        holder.addComponent(ArcComponent.getComponentType(), arcComponent);

        Ref<EntityStore> constructRef = hexContext.getAccessor().addEntity(holder, AddReason.SPAWN);

        RootGlyph rootGlyph = hexContext.getAccessor().getComponent(
                hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
        if (rootGlyph != null) {
            rootGlyph.addDependent(constructRef);
        }

        LOGGER.atInfo().log("arc: spawned construct with %d branches on initial target", branches.size());
    }
}
