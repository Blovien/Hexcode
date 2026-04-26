package com.riprod.hexcode.builtin.glyphs.concentration;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.core.common.construct.system.HexConstructSpawner;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexcasterIdleComponent;

public class ConcentrationGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public String getId() { return ID; }

    public static final String ID = "Concentration";
    private static final String MODEL_ID = "Concentration";
    private static final float VOLATILITY_INCREASE = 0.5f;

    private static final Vector3f MOUNT_OFFSET = new Vector3f(0f, 1.4f, 1.2f);

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        Ref<EntityStore> casterRef = hexContext.getCasterRef();
        if (casterRef == null || !casterRef.isValid()) {
            LOGGER.atWarning().log("Concentration: caster not found");
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Concentration: caster not found");
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        HexcasterIdleComponent execComp = accessor.getComponent(
                casterRef, HexcasterIdleComponent.getComponentType());
        if (execComp == null || !execComp.isHoldingPrimary()) {
            LOGGER.atWarning().log("Concentration: caster not holding primary");
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Concentration: caster not holding primary");
            return;
        }

        TransformComponent casterTransform = accessor.getComponent(
                casterRef, TransformComponent.getComponentType());
        if (casterTransform == null) {
            LOGGER.atWarning().log("Concentration: caster has no transform");
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Concentration: caster has no transform");
            return;
        }

        Ref<EntityStore> visualRef = spawnVisual(accessor, casterTransform, casterRef);

        ConcentrationState state = new ConcentrationState(visualRef);
        HexConstructSpawner.applyWithState(
                accessor, casterRef, hexContext, glyph, ConcentrationGlyph.ID, state);

        var tracker = hexContext.getVolatilityTracker();

        float budget = tracker.getRemainingBudget();
        tracker.addBudget(budget * VOLATILITY_INCREASE);

        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }

    private Ref<EntityStore> spawnVisual(CommandBuffer<EntityStore> accessor,
            TransformComponent casterTransform, Ref<EntityStore> casterRef) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(casterTransform.getPosition(), new Vector3f()));
        holder.ensureComponent(UUIDComponent.getComponentType());
        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(accessor.getExternalData().takeNextNetworkId()));
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());
        holder.addComponent(MountedComponent.getComponentType(),
                new MountedComponent(casterRef, MOUNT_OFFSET, MountController.Minecart));

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(MODEL_ID);
        if (modelAsset != null) {
            Model model = Model.createUnitScaleModel(modelAsset);
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(PersistentModel.getComponentType(),
                    new PersistentModel(model.toReference()));
        } else {
            LOGGER.atWarning().log("concentration: model asset '%s' not found", MODEL_ID);
        }

        return accessor.addEntity(holder, AddReason.SPAWN);
    }
}
