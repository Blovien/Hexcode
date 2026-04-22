package com.riprod.hexcode.builtin.glyphs.effect.domain;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.domain.component.DomainZoneComponent;
import com.riprod.hexcode.builtin.glyphs.effect.domain.style.DomainStyle;
import com.riprod.hexcode.core.common.construct.HexConstructSpawner;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.component.Slot;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.utils.SpellVarUtil;

public class DomainGlyph implements GlyphHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final double DEFAULT_RADIUS = 5.0;
    private static final double MIN_RADIUS = 2.0;
    private static final double MAX_RADIUS = 15.0;
    private static final double DEFAULT_DURATION = 10.0;
    private static final double DEFAULT_POWER = 1.0;
    private static final float BASE_MANA_COST = 20.0f;
    private static final float BASE_TRIGGER_COST = 5.0f;

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        Ref<EntityStore> casterRef = hexContext.getCasterRef();
        if (casterRef == null || !casterRef.isValid()) {
            Executor.fail(hexContext);
            return;
        }

        HexVar targetVar = glyph.readSlot(DomainGlyphSlots.TARGET, hexContext);
        if (targetVar == null) {
            LOGGER.atInfo().log("domain: no target provided");
            Executor.fail(hexContext);
            return;
        }

        Vector3d anchorPos = SpellVarUtil.resolvePosition(targetVar, hexContext.getAccessor());
        if (anchorPos == null) {
            LOGGER.atInfo().log("domain: invalid target position");
            Executor.fail(hexContext);
            return;
        }

        double radius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS,
                SpellVarUtil.resolveNumberOrDefault(
                        glyph.readSlot(DomainGlyphSlots.MAGNITUDE, hexContext), DEFAULT_RADIUS)));

        float durationSeconds = SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot(DomainGlyphSlots.DURATION, hexContext), DEFAULT_DURATION).floatValue();

        float power = SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot(DomainGlyphSlots.POWER, hexContext), DEFAULT_POWER).floatValue();
        power = Math.max(0.1f, power);

        float upfrontCost = BASE_MANA_COST * (1 + (power - 1) * 0.5f);
        if (!hexContext.getRoot().tryConsumeMana(upfrontCost, hexContext.getAccessor())) {
            LOGGER.atInfo().log("domain: insufficient mana for upfront cost %.1f", upfrontCost);
            Executor.fail(hexContext);
            return;
        }

        float baseDrainPerSecond = BASE_MANA_COST * ((float) radius / 5.0f) * 0.1f;

        List<String> conditionalBranchIds = glyph.getNextLinks();
        Slot immediateSlot = glyph.getSlot(DomainGlyphSlots.IMMEDIATE);
        String[] immediateLinks = immediateSlot != null ? immediateSlot.getLinks() : null;
        List<String> immediateBranchIds = (immediateLinks != null && immediateLinks.length > 0)
                ? Arrays.asList(immediateLinks) : null;

        UUIDComponent casterUuidComp = hexContext.getAccessor().getComponent(
                casterRef, UUIDComponent.getComponentType());
        UUID casterUuid = casterUuidComp != null ? casterUuidComp.getUuid() : UUID.randomUUID();

        EntityVar zoneEntityVar = new EntityVar(UUID.randomUUID(), null);
        glyph.writeSelfOutput(zoneEntityVar, hexContext);

        Holder<EntityStore> holder = HexConstructSpawner.create(
                hexContext.getAccessor(), hexContext, glyph,
                "domain", durationSeconds, baseDrainPerSecond,
                immediateBranchIds, conditionalBranchIds, null,
                new Vector3d(anchorPos));

        DomainZoneComponent zoneComp = new DomainZoneComponent(
                (float) radius, baseDrainPerSecond, BASE_TRIGGER_COST, power, casterUuid, casterRef);

        Vector3f debugColor = DomainStyle.resolveColor(hexContext.getColors());
        Vector3d debugScale = new Vector3d(radius * 2, radius * 2, radius * 2);
        DebugComponent debugComp = new DebugComponent(DebugShape.Sphere, debugColor, debugScale, 0.1f);
        debugComp.setOpacity(0.15f);
        debugComp.setIntervalMultiplier(0.01f);
        debugComp.setFlags(DebugUtils.FLAG_NO_WIREFRAME);

        holder.ensureComponent(PropComponent.getComponentType());
        holder.ensureComponent(ProjectileModule.get().getProjectileComponentType());
        holder.ensureComponent(EffectControllerComponent.getComponentType());
        holder.addComponent(DebugComponent.getComponentType(), debugComp);
        holder.addComponent(DomainZoneComponent.getComponentType(), zoneComp);

        Ref<EntityStore> zoneRef = hexContext.getAccessor().addEntity(holder, AddReason.SPAWN);
        zoneComp.setZoneRef(zoneRef);

        UUIDComponent zoneUuidComp = holder.getComponent(UUIDComponent.getComponentType());
        if (zoneUuidComp != null) {
            zoneEntityVar = new EntityVar(zoneUuidComp.getUuid(), zoneRef);
            glyph.writeSelfOutput(zoneEntityVar, hexContext);
        }

        DomainStyle.renderSpawn(anchorPos, (float) radius, hexContext.getColors(), hexContext.getAccessor());

        RootGlyph rootGlyph = hexContext.getAccessor().getComponent(
                hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
        if (rootGlyph != null) {
            rootGlyph.addDependent(zoneRef);
        }
    }
}
