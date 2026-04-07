package com.riprod.hexcode.builtin.glyphs.effect.bolt;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.bolt.style.BoltStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class BoltGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Bolt";

    private static int damageCauseIndex = -1;

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar target = glyph.resolveSlot("target", hexContext);
        if (target == null) {
            LOGGER.atWarning().log("bolt: no target provided");
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        Ref<EntityStore> casterRef = hexContext.getCasterRef();

        TransformComponent casterTc = accessor.getComponent(casterRef, TransformComponent.getComponentType());
        if (casterTc == null) {
            LOGGER.atWarning().log("bolt: caster has no transform");
            return;
        }

        Vector3d sourcePos = casterTc.getPosition().clone();
        World world = accessor.getExternalData().getWorld();
        Vector3f color = BoltStyle.resolveColor(hexContext);
        Double slotIndexD = SpellVarUtil.resolveNumber(glyph.resolveSlot("result", hexContext));
        Integer outputSlot = slotIndexD != null ? slotIndexD.intValue() : null;

        if (target instanceof EntityVar entityVar) {
            handleEntityTarget(glyph, hexContext, entityVar, accessor, casterRef, sourcePos, world, color, outputSlot);
        } else if (target instanceof BlockVar blockVar) {
            handleBlockTarget(glyph, hexContext, blockVar, accessor, casterRef, sourcePos, world, color, outputSlot);
        } else {
            LOGGER.atWarning().log("bolt: target is not entity or block");
            return;
        }

        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }

    private void handleEntityTarget(Glyph glyph, HexContext hexContext, EntityVar entityVar,
            CommandBuffer<EntityStore> accessor, Ref<EntityStore> casterRef,
            Vector3d sourcePos, World world, Vector3f color, Integer outputSlot) {

        Ref<EntityStore> targetRef = entityVar.getRef(accessor);
        if (targetRef == null || !targetRef.isValid()) {
            LOGGER.atWarning().log("bolt: entity target ref invalid");
            return;
        }

        TransformComponent targetTc = accessor.getComponent(targetRef, TransformComponent.getComponentType());
        if (targetTc == null) {
            LOGGER.atWarning().log("bolt: target has no transform");
            return;
        }

        Vector3d targetPos = targetTc.getPosition();

        BoltStyle.renderBolt(accessor, world, sourcePos, targetPos, color);
        BoltStyle.renderImpact(accessor, targetPos);
        BoltStyle.applyShockEffect(accessor, targetRef);

        double damageAmount = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveSlot("power", hexContext), 5.0);
        applyDamage(accessor, targetRef, (float) damageAmount);

        if (outputSlot != null) {
            UUIDComponent targetUuid = accessor.getComponent(targetRef, UUIDComponent.getComponentType());
            if (targetUuid != null) {
                EntityVar resultVar = new EntityVar(EntityVar.createRef(targetUuid.getUuid(), targetRef));
                hexContext.setVariable(outputSlot, resultVar);
            }
        }

        LOGGER.atInfo().log("bolt: hit entity for %.1f damage", damageAmount);
    }

    private void handleBlockTarget(Glyph glyph, HexContext hexContext, BlockVar blockVar,
            CommandBuffer<EntityStore> accessor, Ref<EntityStore> casterRef,
            Vector3d sourcePos, World world, Vector3f color, Integer outputSlot) {

        Vector3i blockPos = blockVar.getValue();
        if (blockPos == null) {
            LOGGER.atWarning().log("bolt: block target position is null");
            return;
        }

        Vector3d targetPos = new Vector3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5);

        BoltStyle.renderBolt(accessor, world, sourcePos, targetPos, color);
        BoltStyle.renderImpact(accessor, targetPos);

        triggerBlockInteraction(accessor, casterRef, world, blockPos);

        if (outputSlot != null) {
            BlockVar resultVar = new BlockVar(blockPos);
            hexContext.setVariable(outputSlot, resultVar);
        }

        LOGGER.atInfo().log("bolt: hit block at %d %d %d", blockPos.x, blockPos.y, blockPos.z);
    }

    private void triggerBlockInteraction(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> casterRef, World world, Vector3i blockPos) {

        InteractionManager manager = accessor.getComponent(casterRef, InteractionModule.get().getInteractionManagerComponent());
        if (manager == null) {
            LOGGER.atInfo().log("bolt: no interaction manager on caster, skipping block interaction");
            return;
        }

        BlockType blockType = world.getBlockType(blockPos);
        if (blockType == null || blockType.isUnknown()) {
            return;
        }

        String interactionId = blockType.getInteractions().get(InteractionType.Use);
        if (interactionId == null) {
            return;
        }

        RootInteraction rootInteraction = RootInteraction.getAssetMap().getAsset(interactionId);
        if (rootInteraction == null) {
            return;
        }

        InteractionContext ctx = InteractionContext.forInteraction(
                manager, casterRef, InteractionType.Use, accessor);
        BlockPosition blockPosition = new BlockPosition(blockPos.x, blockPos.y, blockPos.z);
        ctx.getMetaStore().putMetaObject(Interaction.TARGET_BLOCK, blockPosition);
        ctx.getMetaStore().putMetaObject(Interaction.TARGET_BLOCK_RAW, blockPosition);

        InteractionChain chain = manager.initChain(InteractionType.Use, ctx, rootInteraction, false);
        manager.queueExecuteChain(chain);
    }

    private static void applyDamage(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> targetRef, float amount) {
        if (damageCauseIndex < 0) {
            damageCauseIndex = DamageCause.getAssetMap().getIndex("Environment");
        }
        if (damageCauseIndex == Integer.MIN_VALUE) {
            LOGGER.atWarning().log("bolt: Environment damage cause not found");
            return;
        }

        DamageCause cause = DamageCause.getAssetMap().getAsset(damageCauseIndex);
        if (cause == null) {
            LOGGER.atWarning().log("bolt: could not resolve damage cause");
            return;
        }

        Damage damage = new Damage(new Damage.EnvironmentSource("hex_bolt"), cause, amount);
        DamageSystems.executeDamage(targetRef, accessor, damage);
    }
}
