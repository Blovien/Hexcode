package com.riprod.hexcode.utils;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
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

  public static void line(World world, Vector3d start, Vector3d end, Vector3f color,
      double thickness, float time, boolean fade) {
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
    DebugUtils.add(world, DebugShape.Cube, matrix, color, 1.0f, time, fade);
  }
}
