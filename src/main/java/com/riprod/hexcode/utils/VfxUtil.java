package com.riprod.hexcode.utils;

import javax.annotation.Nullable;

import java.util.List;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class VfxUtil {

  private VfxUtil() {
  }

  public static void particle(String systemId, Vector3d pos, ComponentAccessor<EntityStore> accessor) {
    ParticleUtil.spawnParticleEffect(systemId, pos, accessor);
  }

  public static void sound(String soundId, Vector3d pos, ComponentAccessor<EntityStore> accessor) {
    int index = SoundEvent.getAssetMap().getIndex(soundId);
    if (index >= 0) {
      SoundUtil.playSoundEvent3d(index, SoundCategory.SFX, pos, accessor);
    }
  }

  public static void effect(String particleId, String soundId, Vector3d pos,
      ComponentAccessor<EntityStore> accessor) {
    particle(particleId, pos, accessor);
    sound(soundId, pos, accessor);
  }

  private static int flowPhase = 0;

  public static void advanceFlowPhase() {
    flowPhase = (flowPhase + 1) % 4;
  }

  public static void particleAlongPath(String systemId, Vector3d source, Vector3d target,
      int count, ComponentAccessor<EntityStore> accessor) {
    if (count < 1)
      count = 1;
    double phaseOffset = (double) flowPhase / (count * 4);
    Vector3d point = new Vector3d();
    for (int i = 0; i < count; i++) {
      double t = (double) i / count + phaseOffset;
      if (t >= 1.0)
        t -= 1.0;
      point.x = source.x + (target.x - source.x) * t;
      point.y = source.y + (target.y - source.y) * t;
      point.z = source.z + (target.z - source.z) * t;
      ParticleUtil.spawnParticleEffect(systemId, point, accessor);
    }
  }

  public static void particleAlongPath(String systemId, Vector3d source, Vector3d target,
      int count, Color color, @Nullable Ref<EntityStore> playerRef,
      ComponentAccessor<EntityStore> accessor) {
    if (count < 1)
      count = 1;
    double phaseOffset = (double) flowPhase / (count * 4);

    SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource = accessor
        .getResource(EntityModule.get().getPlayerSpatialResourceType());
    List<Ref<EntityStore>> playerRefs = SpatialResource.getThreadLocalReferenceList();
    playerSpatialResource.getSpatialStructure().collect(source, (double) 25.0F, playerRefs);

    Vector3d point = new Vector3d();
    for (int i = 0; i < count; i++) {
      double t = (double) i / count + phaseOffset;
      if (t >= 1.0)
        t -= 1.0;
      point.x = source.x + (target.x - source.x) * t;
      point.y = source.y + (target.y - source.y) * t;
      point.z = source.z + (target.z - source.z) * t;

      ParticleUtil.spawnParticleEffect(systemId, point, 0.0f, 0.0f, 0.0f, 1.0f, color, playerRefs, accessor);
    }
  }

  public static void line(ComponentAccessor<EntityStore> accessor, World world, Vector3d start, Vector3d end,
      Vector3f color,
      double thickness, float time, int flags) {
    line(accessor, world, start, end, color, thickness, time, flags, null);
  }

  public static void line(ComponentAccessor<EntityStore> accessor, World world, Vector3d start, Vector3d end,
      Vector3f color,
      double thickness, float time, int flags, @Nullable Ref<EntityStore> ref) {
    double dirX = end.x - start.x;
    double dirY = end.y - start.y;
    double dirZ = end.z - start.z;
    double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
    if (length < 0.001)
      return;
    Matrix4d tmp = new Matrix4d();
    Matrix4d matrix = new Matrix4d();
    matrix.identity();
    matrix.translate(start.x, start.y, start.z);
    double angleY = Math.atan2(dirZ, dirX);
    matrix.rotateAxis(angleY + (Math.PI / 2), 0.0, 1.0, 0.0, tmp);
    double angleX = Math.atan2(Math.sqrt(dirX * dirX + dirZ * dirZ), dirY);
    matrix.rotateAxis(angleX, 1.0, 0.0, 0.0, tmp);
    matrix.translate(0.0, length / 2.0, 0.0);
    matrix.scale(thickness, length, thickness);
    int allFlags = flags | DebugUtils.FLAG_NO_WIREFRAME;

    if (ref == null || !ref.isValid()) {
      DebugUtils.add(world, DebugShape.Cube, matrix, color, 0.7f, time, allFlags);
      return;
    }


    PlayerRef playerRef = accessor.getComponent(ref, PlayerRef.getComponentType());
    if (playerRef != null) {
      DisplayDebug packet = new DisplayDebug(
          DebugShape.Cube, matrix.asFloatData(),
          new com.hypixel.hytale.protocol.Vector3f(
              color.x, color.y, color.z),
          time, (byte) allFlags, null, 0.7f);
      playerRef.getPacketHandler().write(packet);
    }
  }
}
