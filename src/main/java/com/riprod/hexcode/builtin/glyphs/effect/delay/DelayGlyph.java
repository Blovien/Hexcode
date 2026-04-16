package com.riprod.hexcode.builtin.glyphs.effect.delay;

import java.util.List;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.delay.style.DelayStyle;
import com.riprod.hexcode.core.common.construct.HexConstructSpawner;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.utils.SpellVarUtil;

public class DelayGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "Glyph_Delay";
    private static final String MODEL_ID = "Glyph_Delay";

    private static final Vector3f MOUNT_OFFSET = new Vector3f(0f, 2.0f, 0f);

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        float seconds = SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("duration", hexContext), 1.0).floatValue();
        if (seconds <= 0f) {
            Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        List<String> nextLinks = glyph.getNextLinks();
        if (nextLinks.isEmpty()) {
            return;
        }

        HexVar var = hexContext.getVariable("1");

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        Vector3d spawnPos = SpellVarUtil.resolvePosition(var, hexContext.getAccessor());

        if (spawnPos == null) {
            Ref<EntityStore> casterRef = hexContext.getCasterRef();
            if (casterRef != null && casterRef.isValid()) {
                spawnPos = accessor.getComponent(casterRef, TransformComponent.getComponentType()).getPosition();
            } else {
                spawnPos = new Vector3d();
            }
        }

        DelayStyle.render(hexContext);

        Holder<EntityStore> holder = HexConstructSpawner.create(
                accessor, hexContext, glyph,
                null, seconds, 0f,
                null, null, nextLinks,
                spawnPos);

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(MODEL_ID);
        if (modelAsset != null) {
            Model model = Model.createUnitScaleModel(modelAsset);
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(PersistentModel.getComponentType(),
                    new PersistentModel(model.toReference()));
        } else {
            LOGGER.atWarning().log("delay: model asset '%s' not found", MODEL_ID);
        }

        Ref<EntityStore> delayRef = accessor.addEntity(holder, AddReason.SPAWN);

        RootGlyph rootGlyph = accessor.getComponent(
                hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
        if (rootGlyph != null) {
            rootGlyph.addDependent(delayRef);
        }
    }
}
