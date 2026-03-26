package com.riprod.hexcode.builtin.glyphs.effect.rupture;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.rupture.component.RuptureComponent;
import com.riprod.hexcode.builtin.glyphs.effect.rupture.component.SpikeEntry;
import com.riprod.hexcode.builtin.glyphs.effect.rupture.style.RuptureStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class RuptureGlyph implements GlyphHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Rupture";

    private static final String SPIKE_MODEL = "Rupture_Spike";
    private static final float SPIKE_SCALE = 0.5f;

    private static final double DEFAULT_RADIUS = 3.0;
    private static final double MIN_RADIUS = 1.0;
    private static final double MAX_RADIUS = 8.0;

    private static final double DEFAULT_DAMAGE = 8.0;
    private static final double MIN_DAMAGE = 2.0;
    private static final double MAX_DAMAGE = 25.0;

    private static final double DEFAULT_DURATION = 100.0;
    private static final double MIN_DURATION = 40.0;
    private static final double MAX_DURATION = 400.0;

    private static final int DAMAGE_COOLDOWN_TICKS = 20;
    private static final int MAX_SPIKES = 64;
    private static final int GROUND_SCAN_RANGE = 3;
    private static final float DENSITY = 0.5f;

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        double radius = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("radius", hexContext), DEFAULT_RADIUS),
                MIN_RADIUS, MAX_RADIUS);
        double duration = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION),
                MIN_DURATION, MAX_DURATION);

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;

        float radiusScale = (float) Math.pow(radius / DEFAULT_RADIUS, 2.0);
        float durationScale = (float) (duration / DEFAULT_DURATION);
        float finalCost = baseCost * castMultiplier * radiusScale * durationScale;

        boolean consumed = hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
        if (!consumed) {
            LOGGER.atInfo().log("rupture: insufficient mana, need %.1f", finalCost);
        }
        return consumed;
    }

    @Override
    public boolean resolveVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;

        double radius = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("radius", hexContext), DEFAULT_RADIUS),
                MIN_RADIUS, MAX_RADIUS);
        double damage = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("damage", hexContext), DEFAULT_DAMAGE),
                MIN_DAMAGE, MAX_DAMAGE);

        int extraRolls = Math.max(0,
                (int) (radius / DEFAULT_RADIUS + damage / DEFAULT_DAMAGE) - 1);

        if (!tracker.rollAndIncrement(glyph)) {
            LOGGER.atInfo().log("rupture: fizzled on primary volatility roll");
            return false;
        }

        for (int i = 0; i < extraRolls; i++) {
            if (!tracker.rollAndIncrement(glyph)) {
                LOGGER.atInfo().log("rupture: fizzled on extra volatility roll %d/%d", i + 1, extraRolls);
                return false;
            }
        }

        return true;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        World world = accessor.getExternalData().getWorld();

        double radius = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("radius", hexContext), DEFAULT_RADIUS),
                MIN_RADIUS, MAX_RADIUS);
        double damage = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("damage", hexContext), DEFAULT_DAMAGE),
                MIN_DAMAGE, MAX_DAMAGE);
        double duration = clamp(SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION),
                MIN_DURATION, MAX_DURATION);

        Vector3d center = resolveCenter(glyph, hexContext, accessor);
        if (center == null) {
            LOGGER.atInfo().log("rupture: could not resolve center position");
            return;
        }

        int centerBlockX = (int) Math.floor(center.x);
        int centerBlockY = (int) Math.floor(center.y);
        int centerBlockZ = (int) Math.floor(center.z);

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(SPIKE_MODEL);
        if (modelAsset == null) {
            LOGGER.atWarning().log("rupture: spike model asset not found: %s", SPIKE_MODEL);
            return;
        }

        List<SpikeEntry> spikes = new ArrayList<>();
        int intRadius = (int) Math.ceil(radius);
        long seed = centerBlockX * 73856093L ^ centerBlockZ * 19349663L ^ centerBlockY * 83492791L;

        for (int dx = -intRadius; dx <= intRadius && spikes.size() < MAX_SPIKES; dx++) {
            for (int dz = -intRadius; dz <= intRadius && spikes.size() < MAX_SPIKES; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;

                long hash = (dx * 73856093L ^ dz * 19349663L ^ seed) & 0xFFFFFFFFL;
                if ((hash % 100) >= (long) (DENSITY * 100)) continue;

                int worldX = centerBlockX + dx;
                int worldZ = centerBlockZ + dz;

                int groundY = findGround(world, worldX, centerBlockY, worldZ);
                if (groundY < 0) continue;
                if (Math.abs(groundY - centerBlockY) > 2) continue;

                Vector3d spikePos = new Vector3d(worldX + 0.5, groundY + 1.0, worldZ + 0.5);

                float yaw = ((hash % 4) * 90.0f) * (float) (Math.PI / 180.0);
                Vector3f rotation = new Vector3f(0, yaw, 0);

                Ref<EntityStore> spikeRef = spawnSpikeEntity(
                        spikePos, rotation, modelAsset, accessor);
                if (spikeRef != null) {
                    spikes.add(new SpikeEntry(spikePos, spikeRef));
                }
            }
        }

        if (spikes.isEmpty()) {
            LOGGER.atInfo().log("rupture: no valid spike positions found");
            return;
        }

        Integer outputSlot = glyph.resolveOutput("result", hexContext);
        if (outputSlot != null) {
            hexContext.setVariable(outputSlot, new PositionVar(new Vector3d(center)));
        }

        spawnTrackerEntity(glyph, hexContext, spikes, (int) duration,
                (float) damage, center, radius, accessor);

        RuptureStyle.renderSeismicBurst(center, hexContext.getColors(), accessor);

        LOGGER.atInfo().log("rupture: spawned %d spikes, radius=%.1f, duration=%d ticks",
                spikes.size(), radius, (int) duration);
    }

    private Vector3d resolveCenter(Glyph glyph, HexContext hexContext,
            CommandBuffer<EntityStore> accessor) {
        HexVar targetVar = glyph.resolveInput("target", hexContext);
        if (targetVar != null && targetVar.size() > 0) {
            if (targetVar instanceof PositionVar) {
                return SpellVarUtil.resolvePosition(targetVar, accessor);
            }
            if (targetVar instanceof EntityVar) {
                Vector3d pos = SpellVarUtil.resolvePosition(targetVar, accessor);
                if (pos != null) return pos;
            }
        }

        Ref<EntityStore> casterRef = hexContext.getCasterRef();
        if (casterRef == null || !casterRef.isValid()) return null;

        TransformComponent tc = accessor.getComponent(casterRef, TransformComponent.getComponentType());
        if (tc == null) return null;

        return tc.getPosition().clone();
    }

    private int findGround(World world, int x, int centerY, int z) {
        for (int y = centerY + GROUND_SCAN_RANGE; y >= centerY - GROUND_SCAN_RANGE; y--) {
            int blockId = world.getBlock(x, y, z);
            if (blockId != BlockType.EMPTY_ID) {
                int aboveId = world.getBlock(x, y + 1, z);
                if (aboveId == BlockType.EMPTY_ID) {
                    return y;
                }
            }
        }
        return -1;
    }

    private Ref<EntityStore> spawnSpikeEntity(Vector3d position, Vector3f rotation,
            ModelAsset modelAsset, CommandBuffer<EntityStore> accessor) {
        Model model = Model.createScaledModel(modelAsset, SPIKE_SCALE);

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(new Vector3d(position), rotation));
        holder.ensureComponent(UUIDComponent.getComponentType());
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
        holder.addComponent(PersistentModel.getComponentType(),
                new PersistentModel(model.toReference()));
        holder.addComponent(BoundingBox.getComponentType(),
                new BoundingBox(model.getBoundingBox()));
        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(accessor.getExternalData().takeNextNetworkId()));
        holder.ensureComponent(PropComponent.getComponentType());

        return accessor.addEntity(holder, AddReason.SPAWN);
    }

    private void spawnTrackerEntity(Glyph glyph, HexContext hexContext,
            List<SpikeEntry> spikes, int durationTicks, float spikeDamage,
            Vector3d center, double radius, CommandBuffer<EntityStore> accessor) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.ensureComponent(UUIDComponent.getComponentType());
        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(accessor.getExternalData().takeNextNetworkId()));
        holder.addComponent(HexSignal.getComponentType(),
                new HexSignal(hexContext.copy(), hexContext.getRoot().getRootEntityRef(),
                        glyph, glyph.getNext(), null));
        holder.addComponent(RuptureComponent.getComponentType(),
                new RuptureComponent(spikes, durationTicks, spikeDamage,
                        DAMAGE_COOLDOWN_TICKS, center, radius));

        accessor.addEntity(holder, AddReason.SPAWN);

        RootGlyph execComp = accessor.getComponent(
                hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
        if (execComp != null) {
            execComp.incrementExternalWaiters();
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
