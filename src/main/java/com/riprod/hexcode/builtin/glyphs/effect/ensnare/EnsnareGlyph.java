package com.riprod.hexcode.builtin.glyphs.effect.ensnare;

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
import com.riprod.hexcode.builtin.glyphs.effect.ensnare.component.EnsnareComponent;
import com.riprod.hexcode.builtin.glyphs.effect.ensnare.component.SpikeEntry;
import com.riprod.hexcode.builtin.glyphs.effect.ensnare.style.EnsnareStyle;
import com.riprod.hexcode.core.common.construct.HexConstructSpawner;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class EnsnareGlyph implements GlyphHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Ensnare";

    private static final String SPIKE_MODEL = "Ensnare_Spike";
    private static final float SPIKE_SCALE = 0.5f;

    private static final double DEFAULT_RADIUS = 3.0;
    private static final double DEFAULT_DAMAGE = 8.0;
    private static final double DEFAULT_DURATION = 5.0;

    private static final double RADIUS_THRESHOLD = 8.0;
    private static final double DAMAGE_THRESHOLD = 25.0;
    private static final double DURATION_THRESHOLD = 20.0;

    private static final float DAMAGE_COOLDOWN_SECONDS = 1.0f;
    private static final int MAX_SPIKES = 64;
    private static final int GROUND_SCAN_RANGE = 3;
    private static final float DENSITY = 0.5f;

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null)
            return true;

        double radius = Math.max(0, SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("radius", hexContext), DEFAULT_RADIUS));
        double duration = Math.max(0, SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("duration", hexContext), DEFAULT_DURATION));

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;

        float radiusScale = (float) harshScale(radius, DEFAULT_RADIUS, RADIUS_THRESHOLD, 2.0);
        float durationScale = (float) harshScale(duration, DEFAULT_DURATION, DURATION_THRESHOLD, 1.5);
        float finalCost = baseCost * castMultiplier * radiusScale * durationScale;

        boolean consumed = hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
        if (!consumed) {
            LOGGER.atInfo().log("ensnare: insufficient mana, need %.1f", finalCost);
        }
        return consumed;
    }

    @Override
    public boolean resolveVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;

        double radius = Math.max(0, SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("radius", hexContext), DEFAULT_RADIUS));
        double damage = Math.max(0, SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("damage", hexContext), DEFAULT_DAMAGE));

        float scale = (float) Math.max(1.0,
                harshScale(radius, DEFAULT_RADIUS, RADIUS_THRESHOLD, 2.0)
                + harshScale(damage, DEFAULT_DAMAGE, DAMAGE_THRESHOLD, 2.0));

        float cost = VolatilityTracker.computeGlyphCost(glyph) * scale;
        return tracker.consumeVolatility(cost);
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        World world = accessor.getExternalData().getWorld();

        double radius = Math.max(0, SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("radius", hexContext), DEFAULT_RADIUS));
        double damage = Math.max(0, SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("damage", hexContext), DEFAULT_DAMAGE));
        double duration = Math.max(0, SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("duration", hexContext), DEFAULT_DURATION));

        Vector3d center = SpellVarUtil.resolvePosition(
                glyph.readSlot("target", hexContext), accessor);
        if (center == null) {
            LOGGER.atInfo().log("ensnare: could not resolve center position");
            Executor.fail(hexContext);
            return;
        }

        int centerBlockX = (int) Math.floor(center.x);
        int centerBlockY = (int) Math.floor(center.y);
        int centerBlockZ = (int) Math.floor(center.z);

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(SPIKE_MODEL);
        if (modelAsset == null) {
            LOGGER.atWarning().log("ensnare: spike model asset not found: %s", SPIKE_MODEL);
            Executor.fail(hexContext);
            return;
        }

        List<SpikeEntry> spikes = new ArrayList<>();
        int intRadius = (int) Math.ceil(radius);
        long seed = centerBlockX * 73856093L ^ centerBlockZ * 19349663L ^ centerBlockY * 83492791L;

        for (int dx = -intRadius; dx <= intRadius && spikes.size() < MAX_SPIKES; dx++) {
            for (int dz = -intRadius; dz <= intRadius && spikes.size() < MAX_SPIKES; dz++) {
                if (dx * dx + dz * dz > radius * radius)
                    continue;

                long hash = (dx * 73856093L ^ dz * 19349663L ^ seed) & 0xFFFFFFFFL;
                if ((hash % 100) >= (long) (DENSITY * 100))
                    continue;

                int worldX = centerBlockX + dx;
                int worldZ = centerBlockZ + dz;

                int groundY = findGround(world, worldX, centerBlockY, worldZ);
                if (groundY < 0)
                    continue;
                if (Math.abs(groundY - centerBlockY) > 2)
                    continue;

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
            LOGGER.atInfo().log("ensnare: no valid spike positions found");
            Executor.fail(hexContext);
            return;
        }

        glyph.writeSlot("result", new PositionVar(new Vector3d(center), true), hexContext);

        spawnTrackerEntity(glyph, hexContext, spikes, (float) duration,
                (float) damage, center, radius, accessor);

        EnsnareStyle.renderSeismicBurst(center, hexContext.getColors(), accessor);

        LOGGER.atInfo().log("ensnare: spawned %d spikes, radius=%.1f, duration=%.1fs",
                spikes.size(), radius, duration);
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
            List<SpikeEntry> spikes, float durationSeconds, float spikeDamage,
            Vector3d center, double radius, CommandBuffer<EntityStore> accessor) {
        Holder<EntityStore> holder = HexConstructSpawner.create(
                accessor, hexContext, glyph,
                "ensnare", durationSeconds, 0,
                null, null, glyph.getNextLinks(),
                new Vector3d(center));

        holder.addComponent(EnsnareComponent.getComponentType(),
                new EnsnareComponent(spikes, durationSeconds, spikeDamage,
                        DAMAGE_COOLDOWN_SECONDS, center, radius));

        Ref<EntityStore> trackerRef = accessor.addEntity(holder, AddReason.SPAWN);

        RootGlyph execComp = accessor.getComponent(
                hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
        if (execComp != null) {
            execComp.addDependent(trackerRef);
        }
    }

    private static double harshScale(double value, double reference, double threshold, double exponent) {
        double ratio = value / reference;
        if (value <= threshold) return ratio;
        double base = threshold / reference;
        double excess = (value - threshold) / reference;
        return base + excess * Math.pow(value / threshold, exponent);
    }
}
