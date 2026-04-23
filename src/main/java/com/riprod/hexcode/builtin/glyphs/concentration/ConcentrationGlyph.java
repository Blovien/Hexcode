package com.riprod.hexcode.builtin.glyphs.concentration;

import java.util.List;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.concentration.component.ConcentrationTriggerComponent;
import com.riprod.hexcode.core.common.construct.system.HexConstructSpawner;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexcasterExecutionComponent;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class ConcentrationGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
public String getId() { return ID; };

public static final String ID = "Concentration";
    public static final String HANDLER_ID = "concentration";
    private static final String MODEL_ID = "Concentration";

    private static final float INFINITE_LIFETIME = 86400f;
    private static final Vector3f MOUNT_OFFSET = new Vector3f(0f, 1.4f, 1.2f);

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        Ref<EntityStore> casterRef = hexContext.getCasterRef();
        if (casterRef == null || !casterRef.isValid()) {
            HexExecuter.fail(hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        HexcasterExecutionComponent execComp = accessor.getComponent(
                casterRef, HexcasterExecutionComponent.getComponentType());
        if (execComp == null || !execComp.isHoldingPrimary()) {
            LOGGER.atInfo().log("concentration: caster not holding primary, failing");
            HexExecuter.fail(hexContext);
            return;
        }

        TransformComponent casterTransform = accessor.getComponent(
                casterRef, TransformComponent.getComponentType());
        if (casterTransform == null) {
            HexExecuter.fail(hexContext);
            return;
        }

        ConcentrationTriggerComponent marker = new ConcentrationTriggerComponent(casterRef);

        Holder<EntityStore> holder = HexConstructSpawner.create(
                accessor, hexContext, glyph,
                HANDLER_ID, INFINITE_LIFETIME, 0f,
                null, null, List.of(),
                casterTransform.getPosition());

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

        Ref<EntityStore> constructRef = accessor.addEntity(holder, AddReason.SPAWN);

        HexExecuter.continueFromSlot(glyph, "Next", hexContext);

        RootGlyph rootGlyph = accessor.getComponent(
                hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
        if (rootGlyph != null) {
            rootGlyph.addDependent(constructRef);
        }
    }
}
