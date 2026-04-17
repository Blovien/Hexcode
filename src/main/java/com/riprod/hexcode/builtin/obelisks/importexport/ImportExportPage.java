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
import com.riprod.hexcode.core.state.crafting.session.HexcodeSessionComponent;
import com.riprod.hexcode.core.state.crafting.session.SessionUtils;

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
        HexcodeSessionComponent session = SessionUtils.resolveSessionByPlayer(ref, store);
        if (session == null) {
            updateStatus("no active session found");
            return;
        }

        Ref<EntityStore> ownerRef = session.getOwnerRef();
        if (ownerRef == null || !ownerRef.isValid() || !ownerRef.equals(ref)) {
            updateStatus("you don't have permission to export from this pedestal");
            return;
        }

        List<Ref<EntityStore>> previewRefs = session.getHexPreviewRefs();
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

        HexcodeSessionComponent importSession = SessionUtils.resolveSessionByPlayer(ref, store);
        if (importSession == null) {
            updateStatus("not in a crafting session");
            return;
        }

        Ref<EntityStore> ownerRef2 = importSession.getOwnerRef();
        if (ownerRef2 == null || !ownerRef2.isValid() || !ownerRef2.equals(ref)) {
            updateStatus("you don't own this pedestal");
            return;
        }

        importSession.setPendingImportHex(hex);

        StringBuilder msg = new StringBuilder("importing hex...");
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
