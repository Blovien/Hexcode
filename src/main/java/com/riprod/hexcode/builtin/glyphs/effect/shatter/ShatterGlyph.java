package com.riprod.hexcode.builtin.glyphs.effect.shatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.projectile.ProjectilePhysicsConfig;
import com.riprod.hexcode.builtin.glyphs.effect.shatter.component.ShatterComponent;
import com.riprod.hexcode.builtin.glyphs.effect.shatter.style.ShatterStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.trigger.component.TriggerComponent;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class ShatterGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Shatter";

    private static final double MAX_DISTANCE = 48.0;
    private static final String SHARD_MODEL = "Glyph_Shatter";
    private static final float SHARD_SCALE = 0.35f;
    private static final int DEFAULT_COUNT = 5;
    private static final double DEFAULT_SPREAD = 30.0;
    private static final double DEFAULT_SPEED = 20.0;
    private static final double DEFAULT_GRAVITY = 10.0;
    private static final int MAX_COUNT = 16;
    private static final double MANA_REFERENCE_COUNT = 5.0;

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        int count = (int) SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("count", hexContext), (double) DEFAULT_COUNT).intValue();
        if (count < 1) count = 1;
        float countScale = (float) (count / MANA_REFERENCE_COUNT);

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;
        float finalCost = baseCost * castMultiplier * countScale;

        return hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar sourceVar = glyph.readSlot("source", hexContext);
        HexVar directionVar = glyph.readSlot("direction", hexContext);
        HexVar countVar = glyph.readSlot("count", hexContext);
        HexVar spreadVar = glyph.readSlot("spread", hexContext);
        HexVar speedVar = glyph.readSlot("speed", hexContext);
        HexVar gravityVar = glyph.readSlot("gravity", hexContext);

        if (sourceVar == null) {
            LOGGER.atWarning().log("shatter: no source provided");
            Executor.fail(hexContext);
            return;
        }

        Vector3d spawnPos = SpellVarUtil.resolveEyePosition(sourceVar, hexContext.getAccessor());
        if (spawnPos == null) {
            spawnPos = SpellVarUtil.resolveAsPosition(sourceVar, hexContext.getAccessor());
        }
        if (spawnPos == null) {
            LOGGER.atWarning().log("shatter: could not resolve spawn position");
            Executor.fail(hexContext);
            return;
        }

        Vector3d centralDir = SpellVarUtil.resolveDirection(
                directionVar, spawnPos, hexContext.getAccessor());
        if (centralDir == null) {
            LOGGER.atWarning().log("shatter: could not resolve direction");
            Executor.fail(hexContext);
            return;
        }

        int count = (int) SpellVarUtil.resolveNumberOrDefault(countVar, (double) DEFAULT_COUNT).intValue();
        if (count < 1) count = 1;
        if (count > MAX_COUNT) count = MAX_COUNT;

        double spreadDeg = SpellVarUtil.resolveNumberOrDefault(spreadVar, DEFAULT_SPREAD);
        double speed = SpellVarUtil.resolveNumberOrDefault(speedVar, DEFAULT_SPEED);
        if (speed <= 0) speed = DEFAULT_SPEED;
        double gravity = SpellVarUtil.resolveNumberOrDefault(gravityVar, DEFAULT_GRAVITY);

        double spreadRad = Math.toRadians(spreadDeg);
        List<Vector3d> shardDirections = computeConeDirections(centralDir, count, spreadRad);

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(SHARD_MODEL);
        Model model = Model.createScaledModel(modelAsset, SHARD_SCALE);

        List<String> nextGlyphs = glyph.getNextLinks();

        for (Vector3d dir : shardDirections) {
            Vector3d shardSpawn = new Vector3d(spawnPos).add(new Vector3d(dir).scale(1.0));
            spawnShard(hexContext, shardSpawn, dir, speed, gravity, model,
                    nextGlyphs, glyph);
        }

        ShatterStyle.renderLaunch(spawnPos, centralDir, hexContext.getColors(), hexContext.getAccessor());
    }

    private List<Vector3d> computeConeDirections(Vector3d center, int count, double spreadRad) {
        Vector3d forward = new Vector3d(center).normalize();

        Vector3d arbitrary = (Math.abs(forward.y) < 0.9)
                ? new Vector3d(0, 1, 0) : new Vector3d(1, 0, 0);

        Vector3d right = cross(arbitrary, forward).normalize();
        Vector3d up = cross(forward, right).normalize();

        List<Vector3d> directions = new ArrayList<>();

        if (count == 1) {
            directions.add(new Vector3d(forward));
            return directions;
        }

        directions.add(new Vector3d(forward));

        int ringCount = count - 1;
        double cosSpread = Math.cos(spreadRad);
        double sinSpread = Math.sin(spreadRad);

        for (int i = 0; i < ringCount; i++) {
            double azimuth = (2.0 * Math.PI * i) / ringCount;

            Vector3d dir = new Vector3d(forward).scale(cosSpread)
                    .add(new Vector3d(right).scale(sinSpread * Math.cos(azimuth)))
                    .add(new Vector3d(up).scale(sinSpread * Math.sin(azimuth)));
            dir.normalize();
            directions.add(dir);
        }

        return directions;
    }

    private static Vector3d cross(Vector3d a, Vector3d b) {
        return new Vector3d(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x
        );
    }

    private void spawnShard(HexContext hexContext, Vector3d position, Vector3d direction,
            double speed, double gravity, Model model,
            List<String> nextGlyphs, Glyph glyph) {

        Vector3f rotation = new Vector3f();
        rotation.setYaw((float) Math.atan2(-direction.x, direction.z));
        rotation.setPitch((float) Math.asin(-direction.y));

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(new Vector3d(position), rotation));
        holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
        holder.ensureComponent(UUIDComponent.getComponentType());
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
        holder.addComponent(PersistentModel.getComponentType(),
                new PersistentModel(model.toReference()));
        holder.addComponent(BoundingBox.getComponentType(),
                new BoundingBox(model.getBoundingBox()));
        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(hexContext.getAccessor().getExternalData().takeNextNetworkId()));
        holder.ensureComponent(PropComponent.getComponentType());
        holder.ensureComponent(ProjectileModule.get().getProjectileComponentType());
        holder.addComponent(Velocity.getComponentType(), new Velocity());

        Vector3d launchVelocity = new Vector3d(direction).scale(speed);
        new ProjectilePhysicsConfig(gravity, 0).apply(holder, hexContext.getCasterRef(),
                launchVelocity, hexContext.getAccessor(), false);

        holder.addComponent(HexSignal.getComponentType(),
                new HexSignal(hexContext.copy(), hexContext.getRoot().getRootEntityRef(),
                        glyph, nextGlyphs));
        holder.addComponent(ShatterComponent.getComponentType(),
                new ShatterComponent(hexContext.getCasterRef(), MAX_DISTANCE, new Vector3d(position)));
        holder.addComponent(TriggerComponent.getComponentType(),
                new TriggerComponent("shatter", -1, null));

        Ref<EntityStore> shardRef = hexContext.getAccessor().addEntity(holder, AddReason.SPAWN);

        RootGlyph execComp = hexContext.getAccessor().getComponent(
                hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
        if (execComp != null) {
            execComp.addDependent(shardRef);
        }
    }
}
