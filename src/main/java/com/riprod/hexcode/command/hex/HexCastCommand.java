package com.riprod.hexcode.command.hex;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.saved.SavedHexAsset;
import com.riprod.hexcode.core.common.imbuement.ImbuementExecutor;
import com.riprod.hexcode.core.common.imbuement.ImbuementExecutor.Request;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.stats.HexcodeEntityStatTypes;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.PlayerHexRoot;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;

public class HexCastCommand extends AbstractPlayerCommand {

    @Nonnull
    private final RequiredArg<String> hexIdArg =
            this.withRequiredArg("hexId", "saved hex asset id", ArgTypes.STRING);

    public HexCastCommand() {
        super("cast", "cast a saved hex by its asset id");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        String hexId = hexIdArg.get(context);

        SavedHexAsset asset = SavedHexAsset.getAssetMap().getAsset(hexId);
        if (asset == null) {
            playerRef.sendMessage(Message.raw("no saved hex found with id: " + hexId));
            return;
        }
        
        Hex hex = asset.getHex().clone();
        String name = asset.getDisplayName() != null ? asset.getDisplayName() : hexId;
        
        playerRef.sendMessage(Message.raw("cast hex: " + name));
    }
}
