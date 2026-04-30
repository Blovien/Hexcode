package com.riprod.hexcode.builtin.glyphs.phase;

import java.util.List;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;
import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.state.NoState;
import com.riprod.hexcode.core.state.execution.HexExecuter;

public class PhaseConstructHandler implements ConstructHandler<NoState> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float BASE_CRUSH_DAMAGE = 4.0f;
    private static int damageCauseIndex = -1;

    @Override
    public boolean onTick(float dt, HexStatus<NoState> status, ConstructTickContext ctx) {
        PhaseComponent phase = ctx.getChunk().getComponent(
                ctx.getIndex(), PhaseComponent.getComponentType());
        if (phase == null)
            return true;

        if (phase.decrementDuration(dt)) {
            return true;
        }

        return !drainSustain(dt, status);
    }

    @Override
    public void onCleanup(HexStatus<NoState> status, ConstructTickContext ctx) {
        PhaseComponent phase = ctx.getBuffer().getComponent(
                ctx.getEntityRef(), PhaseComponent.getComponentType());
        if (phase != null) {
            World world = ctx.getBuffer().getExternalData().getWorld();

            for (PhasedBlock block : phase.getPhasedBlocks()) {
                Vector3i pos = block.getPosition();
                Vector3d blockCenter = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);

                applyCrushDamage(pos, blockCenter, ctx.getBuffer());

                int blockId = BlockType.getAssetMap().getIndex(block.getBlockTypeId());
                BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
                world.getChunk(ChunkUtil.indexChunkFromBlock(pos.x, pos.z))
                        .setBlock(pos.x, pos.y, pos.z, blockId, blockType, block.getRotationIndex(), 0, 0);

                PhaseStyle.renderPhaseIn(blockCenter, status.getHexContext().getColors(),
                        ctx.getBuffer());
            }

            LOGGER.atInfo().log("phase: restored %d blocks", phase.getPhasedBlocks().size());
            HexExecuter.continueExecution(status.getTriggeringGlyph().getNextLinks(), status.getHexContext());
        }

        ctx.getBuffer().tryRemoveEntity(ctx.getEntityRef(), RemoveReason.REMOVE);

    }

    private void applyCrushDamage(Vector3i pos, Vector3d blockCenter,
            CommandBuffer<EntityStore> buffer) {
        Vector3d min = new Vector3d(pos.x, pos.y, pos.z);
        Vector3d max = new Vector3d(pos.x + 1.0, pos.y + 1.0, pos.z + 1.0);
        List<Ref<EntityStore>> entities = TargetUtil.getAllEntitiesInBox(min, max, buffer);

        if (damageCauseIndex < 0) {
            damageCauseIndex = DamageCause.getAssetMap().getIndex("Environment");
        }

        for (Ref<EntityStore> ref : entities) {
            if (ref == null || !ref.isValid())
                continue;

            TransformComponent tc = buffer.getComponent(ref, TransformComponent.getComponentType());
            if (tc == null)
                continue;

            if (damageCauseIndex >= 0) {
                DamageCause cause = DamageCause.getAssetMap().getAsset(damageCauseIndex);
                if (cause != null) {
                    Damage damage = new Damage(
                            new Damage.EnvironmentSource("hex_phase"), cause, BASE_CRUSH_DAMAGE);
                    DamageSystems.executeDamage(ref, buffer, damage);
                }
            }

            PhaseStyle.renderCrush(blockCenter, null, buffer);
        }
    }
}
