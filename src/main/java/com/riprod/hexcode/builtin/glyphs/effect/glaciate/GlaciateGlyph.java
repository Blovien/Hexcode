package com.riprod.hexcode.builtin.glyphs.effect.glaciate;

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
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollision;
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollisionConfig;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.component.GlaciateComponent;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.style.GlaciateStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.utils.SpellVarUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GlaciateGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Glaciate";

    private static final String ICE_MODEL = "Glaciate_Ice";
    private static final float ICE_SCALE = 2.0f;
    private static final double DEFAULT_HEIGHT = 10.0;
    private static final double DEFAULT_DURATION = 5.0;
    private static final float DEFAULT_DAMAGE_RADIUS = 2.0f;
    private static final float DEFAULT_DAMAGE_MULTIPLIER = 1.0f;
    private static final String HARD_COLLISION_CONFIG = "Hexcode_Glaciate_HardCollision";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targetVar = glyph.resolveInput("target", hexContext);
        if (targetVar == null || targetVar.size() == 0) {
            LOGGER.atWarning().log("glaciate: no target provided");
            return;
        }

        HexVar offsetVar = glyph.resolveInput("offset", hexContext);
        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION);
        if (duration <= 0) duration = DEFAULT_DURATION;

        Integer outputSlot = glyph.resolveOutput("result", hexContext);

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(ICE_MODEL);
        if (modelAsset == null) {
            LOGGER.atWarning().log("glaciate: model asset not found: %s", ICE_MODEL);
            return;
        }

        HitboxCollisionConfig collisionConfig = HitboxCollisionConfig.getAssetMap()
                .getAsset(HARD_COLLISION_CONFIG);

        for (int i = 0; i < targetVar.size(); i++) {
            Vector3d targetPos = SpellVarUtil.resolvePositionAt(targetVar, i, hexContext.getAccessor());
            if (targetPos == null) continue;

            Vector3d spawnPos = resolveSpawnPosition(targetPos, offsetVar, hexContext);
            spawnIceBlock(glyph, hexContext, spawnPos, outputSlot, (float) duration,
                    modelAsset, collisionConfig);
        }
    }

    private Vector3d resolveSpawnPosition(Vector3d targetPos, HexVar offsetVar, HexContext hexContext) {
        if (offsetVar == null) {
            return new Vector3d(targetPos).add(new Vector3d(0, DEFAULT_HEIGHT, 0));
        }

        // entity input → absolute position (entity's world position)
        if (offsetVar instanceof EntityVar) {
            Vector3d absPos = SpellVarUtil.resolvePosition(offsetVar, hexContext.getAccessor());
            if (absPos != null) return absPos;
            return new Vector3d(targetPos).add(new Vector3d(0, DEFAULT_HEIGHT, 0));
        }

        // position input → relative offset from target
        if (offsetVar instanceof PositionVar posVar && posVar.size() > 0) {
            return new Vector3d(targetPos).add(new Vector3d(posVar.getAt(0)));
        }

        // number input → height shorthand (0, number, 0)
        if (offsetVar instanceof NumberVar) {
            double height = SpellVarUtil.resolveNumberOrDefault(offsetVar, DEFAULT_HEIGHT);
            return new Vector3d(targetPos).add(new Vector3d(0, height, 0));
        }

        return new Vector3d(targetPos).add(new Vector3d(0, DEFAULT_HEIGHT, 0));
    }

    private void spawnIceBlock(Glyph glyph, HexContext hexContext, Vector3d spawnPos,
            Integer outputSlot, float duration,
            ModelAsset modelAsset, HitboxCollisionConfig collisionConfig) {
        Model model = Model.createScaledModel(modelAsset, ICE_SCALE);
        Vector3f rotation = new Vector3f();

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(new Vector3d(spawnPos), rotation));
        holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
        UUID iceUuid = UUID.randomUUID();
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(iceUuid));
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

        if (collisionConfig != null) {
            holder.addComponent(HitboxCollision.getComponentType(),
                    new HitboxCollision(collisionConfig));
        }

        List<String> allNext = glyph.getNext();
        List<String> firstBranch = !allNext.isEmpty() ? List.of(allNext.get(0)) : null;
        List<String> entryNext = allNext.size() > 1
                ? List.copyOf(allNext.subList(1, allNext.size()))
                : List.of();

        Map<String, Integer> outputSlots = new HashMap<>();
        if (outputSlot != null) outputSlots.put("result", outputSlot);

        holder.addComponent(HexSignal.getComponentType(),
                new HexSignal(hexContext.copy(), hexContext.getRoot().getRootEntityRef(),
                        glyph, entryNext, outputSlots));
        holder.addComponent(GlaciateComponent.getComponentType(),
                new GlaciateComponent(duration, DEFAULT_DAMAGE_RADIUS, DEFAULT_DAMAGE_MULTIPLIER,
                        firstBranch));

        GlaciatePhysicsConfig.INSTANCE.apply(holder, hexContext.getCasterRef(),
                Vector3d.ZERO, hexContext.getAccessor(), false);

        Ref<EntityStore> iceRef = hexContext.getAccessor().addEntity(holder, AddReason.SPAWN);

        if (outputSlot != null) {
            hexContext.setVariable(outputSlot, new EntityVar(iceUuid, iceRef));
        }

        RootGlyph execComp = hexContext.getAccessor().getComponent(
                hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
        if (execComp != null) {
            execComp.incrementExternalWaiters();
        }

        GlaciateStyle.renderSpawn(spawnPos, hexContext.getColors(), hexContext.getAccessor());
    }
}
