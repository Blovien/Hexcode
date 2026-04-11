package com.riprod.hexcode.builtin.obelisks.importexport;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.obelisk.component.ObeliskBlockComponent;
import com.riprod.hexcode.core.common.obelisk.interfaces.ObeliskInterface;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;

public class ImportExportObelisk implements ObeliskInterface {

    @Override
    public void onEnterCrafting(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            ObeliskBlockComponent obelisk) {

        PlayerRef ref = buffer.getComponent(playerRef, PlayerRef.getComponentType());
        if (ref == null) return;
        
        Vector3i pedestalLoc = obelisk.getRegisteredPedestalLoc();
        if (pedestalLoc == null) return;
        Holder<ChunkStore> pedestalHolder = buffer.getExternalData().getWorld().getBlockComponentHolder(pedestalLoc.x, pedestalLoc.y, pedestalLoc.z);

        PedestalBlockComponent pedestal = pedestalHolder.getComponent(PedestalBlockComponent.getComponentType());

        HexcasterCraftingComponent craftingComp = buffer.getComponent(playerRef, HexcasterCraftingComponent.getComponentType());

        Player player = buffer.getComponent(playerRef, Player.getComponentType());
        if (player == null) return;

        Store<EntityStore> store = buffer.getExternalData().getWorld().getEntityStore().getStore();
        player.getPageManager().openCustomPage(playerRef, store, new ImportExportPage(ref));
    }
}
