package com.riprod.hexcode.builtin.glyphs.value;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.common.glyphs.variables.RotationVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;
import com.riprod.hexcode.utils.VelocityUtil;

public class RotationValue implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private HexVar compute(Glyph glyph, HexContext hexContext) {
        HexVar xVar = glyph.readSlot(RotationValueSlots.X, hexContext);
        HexVar yVar = glyph.readSlot(RotationValueSlots.Y, hexContext);
        HexVar zVar = glyph.readSlot(RotationValueSlots.Z, hexContext);

        int count = (xVar != null ? 1 : 0) + (yVar != null ? 1 : 0) + (zVar != null ? 1 : 0);

        if (count == 1) {
            HexVar single = xVar != null ? xVar : (yVar != null ? yVar : zVar);
            // absolute positions don't resolve as rotations
            if (single instanceof PositionVar posVar && posVar.isAbsolute()) {
                return null;
            }
            if (SpellVarUtil.isVectorVar(single)) {
                Vector3f rot = SpellVarUtil.resolveAsRotation(single, hexContext.getAccessor());
                return rot != null ? new RotationVar(rot) : null;
            }
        }

        var accessor = hexContext.getAccessor();
        return new RotationVar(new Vector3f(
                (float) SpellVarUtil.resolveRotationAxis(xVar, 0, accessor),
                (float) SpellVarUtil.resolveRotationAxis(yVar, 1, accessor),
                (float) SpellVarUtil.resolveRotationAxis(zVar, 2, accessor)));
    }

    @Override
    public HexVar readValue(Glyph glyph, HexContext hexContext) {
        return compute(glyph, hexContext);
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar result = compute(glyph, hexContext);

        if (result != null) {
            glyph.writeOutput(result, hexContext);
        }

        // effect mode: if a target is wired, apply rotation to it
        if (result instanceof RotationVar rotVar && rotVar.getValue() != null) {
            HexVar target = glyph.readSlot(RotationValueSlots.TARGET, hexContext);
            EntityVar entityVar = SpellVarUtil.resolveEntityVar(target, hexContext);
            if (entityVar != null) {
                applyToEntity(entityVar, rotVar.getValue(), hexContext);
            } else {
                BlockVar blockVar = SpellVarUtil.resolveBlockVar(target, hexContext);
                if (blockVar != null && blockVar.getValue() != null) {
                    applyToBlock(blockVar.getValue(), rotVar.getValue(), hexContext);
                }
            }
        }

        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }

    private void applyToEntity(EntityVar entityVar, Vector3f rotation, HexContext hexContext) {
        Ref<EntityStore> ref = entityVar.getRef(hexContext.getAccessor());
        if (ref == null || !ref.isValid()) return;

        // projectile path: re-aim velocity while preserving speed
        if (VelocityUtil.isProjectile(ref, hexContext.getAccessor())) {
            applyToProjectile(ref, rotation, hexContext);
            return;
        }

        try {
            TransformComponent tc = hexContext.getAccessor().getComponent(ref,
                    TransformComponent.getComponentType());
            if (tc != null) {
                tc.teleportRotation(rotation);
            }
            HeadRotation hr = hexContext.getAccessor().getComponent(ref,
                    HeadRotation.getComponentType());
            if (hr != null) {
                hr.teleportRotation(rotation);
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("rotation glyph: could not rotate entity: %s", e.getMessage());
        }
    }

    private void applyToProjectile(Ref<EntityStore> ref, Vector3f rotation, HexContext hexContext) {
        Velocity vel = hexContext.getAccessor().getComponent(ref, Velocity.getComponentType());
        double speed = 0.0;
        if (vel != null) {
            Vector3d current = vel.getVelocity();
            if (current != null) {
                speed = Math.sqrt(current.x * current.x + current.y * current.y + current.z * current.z);
            }
        }
        if (speed <= 0.0001) speed = 1.0;

        double yawRad = Math.toRadians(rotation.y);
        double pitchRad = Math.toRadians(rotation.x);
        double cosPitch = Math.cos(pitchRad);
        Vector3d newVel = new Vector3d(
                -Math.sin(yawRad) * cosPitch * speed,
                -Math.sin(pitchRad) * speed,
                Math.cos(yawRad) * cosPitch * speed);

        try {
            VelocityUtil.applyVelocity(ref, newVel, ChangeVelocityType.Set,
                    new VelocityConfig(), hexContext.getAccessor());
        } catch (Exception e) {
            LOGGER.atWarning().log("rotation glyph: could not re-aim projectile: %s", e.getMessage());
        }
    }

    private void applyToBlock(Vector3i pos, Vector3f rotation, HexContext hexContext) {
        try {
            World world = hexContext.getAccessor().getExternalData().getWorld();
            int blockId = world.getBlock(pos.x, pos.y, pos.z);
            if (blockId == BlockType.EMPTY_ID) return;
            BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
            if (blockType == null) return;
            int rotY = quarterTurn(rotation.y);
            int rotP = quarterTurn(rotation.x);
            int rotR = quarterTurn(rotation.z);
            world.getChunk(ChunkUtil.indexChunkFromBlock(pos.x, pos.z))
                    .setBlock(pos.x, pos.y, pos.z, blockId, blockType, rotY, rotP, rotR);
        } catch (Exception e) {
            LOGGER.atWarning().log("rotation glyph: could not rotate block: %s", e.getMessage());
        }
    }

    private static int quarterTurn(float degrees) {
        int steps = Math.round(degrees / 90f);
        return ((steps % 4) + 4) % 4;
    }
}
