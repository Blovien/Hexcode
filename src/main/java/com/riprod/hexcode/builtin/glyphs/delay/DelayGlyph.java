package com.riprod.hexcode.builtin.glyphs.delay;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.delay.style.DelayStyle;
import com.riprod.hexcode.core.common.construct.system.HexConstructSpawner;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexVarUtil;

public class DelayGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public String getId() { return ID; }

    public static final String ID = "Delay";
    private static final String MODEL_ID = "Delay";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        float seconds = HexVarUtil.numberOrDefault(
                glyph.readSlot(DelayGlyphSlots.DURATION, hexContext), 1.0).floatValue();

        if (seconds <= 0f) {
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        List<String> nextLinks = glyph.getNextLinks();
        if (nextLinks.isEmpty()) {
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        HexVar var = hexContext.getVariable(Glyph.DEFAULT_SLOT);
        Vector3d spawnPos = HexVarUtil.position(var, accessor);
        if (spawnPos == null) {
            Ref<EntityStore> casterRef = hexContext.getCasterRef();
            if (casterRef != null && casterRef.isValid()) {
                TransformComponent tc = accessor.getComponent(
                        casterRef, TransformComponent.getComponentType());
                spawnPos = tc != null ? tc.getPosition() : new Vector3d();
            } else {
                spawnPos = new Vector3d();
            }
        }

        DelayStyle.render(hexContext);

        DelayState state = new DelayState(seconds, new ArrayList<>(nextLinks), hexContext.getColors());

        Holder<EntityStore> holder = HexConstructSpawner.createWithState(
                accessor, hexContext, glyph, DelayGlyph.ID, spawnPos, state);

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

        hexContext.getRoot().addDependency(hexContext, delayRef);
    }
}
