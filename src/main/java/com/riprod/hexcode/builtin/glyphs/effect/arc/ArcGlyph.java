package com.riprod.hexcode.builtin.glyphs.effect.arc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.arc.component.ArcComponent;
import com.riprod.hexcode.builtin.glyphs.effect.arc.style.ArcStyle;
import com.riprod.hexcode.builtin.glyphs.effect.arc.utils.ArcUtils;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
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
            return;
        }

        Ref<EntityStore> initialTarget = entityVar.getRef(hexContext.getAccessor());
        if (initialTarget == null || !initialTarget.isValid()) {
            LOGGER.atWarning().log("arc: initial target ref invalid");
            return;
        }

        List<String> branches = glyph.getNextLinks();
        if (branches.isEmpty()) {
            LOGGER.atInfo().log("arc: no child branches, nothing to do");
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

        Ref<EntityStore> hexEntityRef = hexContext.getRoot().getRootEntityRef();

        ArcComponent arcComponent = new ArcComponent(
                glyph,
                new ArrayList<>(branches),
                visited,
                (float) maxJump,
                (float) delay);

        HexSignal signal = new HexSignal(
                hexContext.copy(), hexEntityRef, glyph, branches);

        hexContext.getAccessor().addComponent(
                initialTarget, ArcComponent.getComponentType(), arcComponent);
        hexContext.getAccessor().addComponent(
                initialTarget, HexSignal.getComponentType(), signal);

        RootGlyph rootGlyph = hexContext.getAccessor().getComponent(
                hexEntityRef, RootGlyph.getComponentType());
        if (rootGlyph != null) {
            rootGlyph.incrementExternalWaiters();
        }

        LOGGER.atInfo().log("arc: attached chain with %d branches to initial target", branches.size());
    }
}
