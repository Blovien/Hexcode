package com.riprod.hexcode.core.execute.system;

import java.util.List;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.drawing.component.DrawnShapeComponent;
import com.riprod.hexcode.core.drawing.system.GlyphCreationManager;
import com.riprod.hexcode.core.drawing.system.InterfaceManager;
import com.riprod.hexcode.core.execute.Executor;
import com.riprod.hexcode.core.execute.component.HexGraph;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.player.component.HexcasterComponent;
import com.riprod.hexcode.player.system.CasterInventory;

public class ExecutionManager {
  public static InteractionState BeginExecution(ComponentAccessor<EntityStore> accessor,
      HexcasterComponent hexcaster,
      Ref<EntityStore> playerRef) {

    HexStaffComponent hexStaff = CasterInventory.getHexStaffComponent(accessor, playerRef);
    HexGraph activeHex = hexStaff.getActiveSpell();

    Executor.beginExecution(activeHex, playerRef, accessor);

    return InteractionState.Finished;
  }
}
