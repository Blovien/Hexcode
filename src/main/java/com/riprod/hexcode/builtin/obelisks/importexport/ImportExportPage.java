package com.riprod.hexcode.builtin.obelisks.importexport;

import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexes.codec.DecodeIssue;
import com.riprod.hexcode.core.common.hexes.codec.DecodeResult;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hexes.utils.HexUtils;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.CraftingData;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;

public class ImportExportPage extends InteractiveCustomUIPage<ImportExportPage.PageEventData> {

    private String fieldValue = "";

    public ImportExportPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageEventData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {

        cmd.append("Hexcode/Obelisks/ImportExport.ui");
        cmd.set("#StatusLabel.Text", "");
        cmd.set("#DataField.Value", fieldValue);

        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged, "#DataField",
                EventData.of("Action", "sync").append("@Data", "#DataField.Value"),
                false);

        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#ExportButton",
                EventData.of("Action", "export"));

        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#ImportButton",
                EventData.of("Action", "import"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PageEventData data) {

        if ("sync".equals(data.action)) {
            fieldValue = data.data != null ? data.data : "";
        } else if ("export".equals(data.action)) {
            handleExport(ref, store);
        } else if ("import".equals(data.action)) {
            handleImport(ref, store);
        }
    }

    private void handleExport(Ref<EntityStore> ref, Store<EntityStore> store) {
        HexcasterCraftingComponent castingComp = store.getComponent(ref, HexcasterCraftingComponent.getComponentType());
        if (castingComp == null) {
            updateStatus("no hexcaster data found");
            return;
        }

        World world = store.getExternalData().getWorld();

        Vector3i pedestalPos = castingComp.getPedestalLocation();
        if (pedestalPos == null) {
            updateStatus("Pedestal location not found");
            return;
        }

        PedestalBlockComponent pedestalComp = BlockModule.getComponent(PedestalBlockComponent.getComponentType(), world,
                pedestalPos.x, pedestalPos.y, pedestalPos.z);

        CraftingData playerData = pedestalComp.getCraftingDataComponent();

        // access control check
        Ref<EntityStore> ownerRef = playerData.getOwnerRef();
        if (ownerRef == null || !ownerRef.isValid() || !ownerRef.equals(ref)) {
            updateStatus("you don't have permission to export from this pedestal");
            return;
        }

        // get active hex
        List<Ref<EntityStore>> previewRefs = playerData.getHexPreviewRefs();
        if (previewRefs == null || previewRefs.isEmpty())
            return;

        Ref<EntityStore> activeHexRef = previewRefs.get(0);
        if (activeHexRef == null || !activeHexRef.isValid())
            return;

        HexComponent hexComp = store.getComponent(activeHexRef, HexComponent.getComponentType());
        if (hexComp == null)
            return;

        Hex hex = hexComp.getHex();
        
        String encoded = HexUtils.serialize(hex);
        if (encoded == null) {
            updateStatus("failed to serialize hex");
            return;
        }

        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#DataField.Value", encoded);
        cmd.set("#StatusLabel.Text", "exported hex");
        sendUpdate(cmd);
    }

    private void handleImport(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (fieldValue.isEmpty()) {
            updateStatus("paste hex data first");
            return;
        }

        DecodeResult result = HexUtils.deserializeWithResult(fieldValue);
        if (result.getHex() == null) {
            StringBuilder msg = new StringBuilder("invalid hex data");
            for (DecodeIssue issue : result.getIssues()) {
                msg.append("\n").append(issue);
            }
            updateStatus(msg.toString());
            return;
        }

        Hex hex = result.getHex();
        HexUtils.validate(hex);

        HexcasterCraftingComponent craftingComp = store.getComponent(ref, HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) {
            updateStatus("not in crafting mode");
            return;
        }

        World world = store.getExternalData().getWorld();
        Vector3i pedestalPos = craftingComp.getPedestalLocation();
        if (pedestalPos == null) {
            updateStatus("pedestal not found");
            return;
        }

        PedestalBlockComponent pedestalComp = BlockModule.getComponent(
                PedestalBlockComponent.getComponentType(), world,
                pedestalPos.x, pedestalPos.y, pedestalPos.z);
        if (pedestalComp == null) {
            updateStatus("pedestal not found");
            return;
        }

        CraftingData playerData = pedestalComp.getCraftingDataComponent();
        Ref<EntityStore> ownerRef = playerData.getOwnerRef();
        if (ownerRef == null || !ownerRef.isValid() || !ownerRef.equals(ref)) {
            updateStatus("you don't own this pedestal");
            return;
        }

        int activeSlot = playerData.getActiveSlotIndex();
        List<Hex> hexes = playerData.getHexes();
        if (activeSlot < 0 || activeSlot >= hexes.size()) {
            updateStatus("no active hex slot");
            return;
        }

        hexes.set(activeSlot, hex);

        List<Ref<EntityStore>> previewRefs = playerData.getHexPreviewRefs();
        if (previewRefs != null && activeSlot < previewRefs.size()) {
            Ref<EntityStore> activeRef = previewRefs.get(activeSlot);
            if (activeRef != null && activeRef.isValid()) {
                HexComponent hexComp = store.getComponent(activeRef, HexComponent.getComponentType());
                if (hexComp != null) {
                    Hex existing = hexComp.getHex();
                    existing.replaceWith(hex);
                }
            }
        }

        StringBuilder msg = new StringBuilder("imported hex — replacing active slot");
        for (DecodeIssue issue : result.getIssues()) {
            if (issue.getSeverity() != DecodeIssue.Severity.INFO) {
                msg.append("\n").append(issue);
            }
        }
        updateStatus(msg.toString());
    }

    private void updateStatus(String message) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#StatusLabel.Text", message);
        sendUpdate(cmd);
    }

    public static class PageEventData {
        public String action;
        public String data;

        public static final BuilderCodec<PageEventData> CODEC = BuilderCodec
                .builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (d, v) -> d.action = v, d -> d.action)
                .add()
                .append(new KeyedCodec<>("@Data", Codec.STRING),
                        (d, v) -> d.data = v, d -> d.data)
                .add()
                .build();
    }
}
