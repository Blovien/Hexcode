package com.riprod.hexcode.builtin.glyphs.effect.concentration;

import java.util.List;
import java.util.UUID;

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
import com.riprod.hexcode.builtin.glyphs.effect.concentration.component.ConcentrationTriggerComponent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.trigger.component.TriggerComponent;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.HexcasterExecutionComponent;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class ConcentrationGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "Glyph_Concentration";
    public static final String TRIGGER_HANDLER_ID = "concentration";
    private static final String MODEL_ID = "Glyph_Concentration";

    // effectively infinite — the trigger is ended by the release watchdog in onTick
    private static final float INFINITE_LIFETIME = 86400f;

    // position in front of the player's chest
    private static final Vector3f MOUNT_OFFSET = new Vector3f(0f, 1.4f, 1.2f);

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        Ref<EntityStore> casterRef = hexContext.getCasterRef();
        if (casterRef == null || !casterRef.isValid()) {
            Executor.fail(hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        HexcasterExecutionComponent execComp = accessor.getComponent(
                casterRef, HexcasterExecutionComponent.getComponentType());
        if (execComp == null || !execComp.isHoldingPrimary()) {
            LOGGER.atInfo().log("concentration: caster not holding primary, failing");
            Executor.fail(hexContext);
            return;
        }

        TransformComponent casterTransform = accessor.getComponent(
                casterRef, TransformComponent.getComponentType());
        if (casterTransform == null) {
            Executor.fail(hexContext);
            return;
        }

        HexSignal signal = new HexSignal(
                hexContext.copy(),
                hexContext.getRoot().getRootEntityRef(),
                glyph,
                List.of());

        TriggerComponent triggerComp = new TriggerComponent(TRIGGER_HANDLER_ID, INFINITE_LIFETIME, null);
        ConcentrationTriggerComponent marker = new ConcentrationTriggerComponent(casterRef);

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(casterTransform.getPosition(), new Vector3f()));
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));
        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(accessor.getExternalData().takeNextNetworkId()));
        holder.addComponent(TriggerComponent.getComponentType(), triggerComp);
        holder.addComponent(HexSignal.getComponentType(), signal);
        holder.addComponent(ConcentrationTriggerComponent.getComponentType(), marker);

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

        Ref<EntityStore> triggerRef = accessor.addEntity(holder, AddReason.SPAWN);

        Executor.continueFromSlot(glyph, "Next", hexContext);

        RootGlyph rootGlyph = accessor.getComponent(
                hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
        if (rootGlyph != null) {
            rootGlyph.addDependent(triggerRef);
        }
    }
}
