package com.riprod.hexcode.command.hex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.bson.BsonDocument;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.BsonUtil;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.core.state.execution.component.HexcasterExecutionComponent;

public class HexInspectCommand extends AbstractPlayerCommand {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final FlagArg detailedFlag;

    public HexInspectCommand() {
        super("inspect", "Print the glyph tree of the active hex on the held staff");
        addAliases("i");

        this.detailedFlag = this.withFlagArg("detailed", "raw JSON dump of hex data");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        boolean detailed = detailedFlag.get(context);

        HexStaffComponent staff = CasterInventory.getHexStaffComponent(store, playerEntityRef);
        HexcasterExecutionComponent execComp = store.getComponent(playerEntityRef, HexcasterExecutionComponent.getComponentType());
        if (staff == null) {
            send(playerRef, "You are not holding a hex staff");
            return;
        }
        if (execComp == null) {
            send(playerRef, "No execution component found on player");
            return;
        }

        Hex hex = execComp.getActiveHex();
        if (hex == null) {
            send(playerRef, "No active hex on staff");
            return;
        }

        if (detailed) {
            printDetailed(playerRef, hex);
        } else {
            printFormatted(playerRef, hex, staff.getStyleId());
        }
    }

    private void printDetailed(PlayerRef playerRef, Hex hex) {
        ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
        BsonDocument doc = Hex.CODEC.encode(hex, extraInfo);
        String json = BsonUtil.toJson(doc);

        send(playerRef, "== Hex Raw Data ==");
        send(playerRef, json);
    }

    private void printFormatted(PlayerRef playerRef, Hex hex, String style) {
        Map<String, Integer> indexMap = new HashMap<>();
        List<Glyph> effectGlyphs = new ArrayList<>();
        Set<String> valueGlyphIds = new LinkedHashSet<>();
        int idx = 1;

        // walk execution chain to assign indices and collect effects
        String currentId = hex.getFirstGlyphId();
        Set<String> walked = new java.util.HashSet<>();
        List<String> execQueue = new ArrayList<>();
        if (currentId != null) execQueue.add(currentId);

        while (!execQueue.isEmpty()) {
            String id = execQueue.removeFirst();
            if (id == null || walked.contains(id)) continue;
            walked.add(id);

            Glyph g = hex.get(id);
            if (g == null) continue;

            indexMap.put(id, idx++);
            effectGlyphs.add(g);
            execQueue.addAll(g.getNextLinks());
        }

        // assign indices to all value glyphs referenced by inputs/outputs
        for (Glyph g : hex.getGlyphs()) {
            if (!indexMap.containsKey(g.getId())) {
                indexMap.put(g.getId(), idx++);
                valueGlyphIds.add(g.getId());
            }
        }

        // collect value glyphs reachable from effect inputs/outputs
        for (Glyph effect : effectGlyphs) {
            collectValues(hex, effect, valueGlyphIds, indexMap);
        }

        List<String> lines = new ArrayList<>();
        lines.add("== Hex Staff (style: " + style + ") ==");
        lines.add("");

        // execution chain
        for (Glyph g : effectGlyphs) {
            int num = indexMap.get(g.getId());
            StringBuilder sb = new StringBuilder();
            sb.append("  ").append(num).append(". ").append(formatGlyph(g));

            lines.add(sb.toString());

            for (Map.Entry<String, com.riprod.hexcode.core.common.glyphs.component.Slot> entry : g.getSlots().entrySet()) {
                String key = entry.getKey();
                String[] links = entry.getValue().getLinks();
                if (links.length == 0) continue;
                if (com.riprod.hexcode.core.common.glyphs.component.Glyph.NEXT_SLOT.equals(key)) continue;
                StringBuilder slotSb = new StringBuilder("     ").append(key).append(": ");
                for (int i = 0; i < links.length; i++) {
                    Glyph v = hex.get(links[i]);
                    if (v == null) continue;
                    Integer linkIdx = indexMap.get(v.getId());
                    slotSb.append("#").append(linkIdx != null ? linkIdx : "?")
                            .append(" ").append(shortName(v.getGlyphId()));
                    if (i < links.length - 1) slotSb.append(", ");
                }
                lines.add(slotSb.toString());
            }

            String[] nextIds = g.getSlot(com.riprod.hexcode.core.common.glyphs.component.Glyph.NEXT_SLOT) != null
                    ? g.getSlot(com.riprod.hexcode.core.common.glyphs.component.Glyph.NEXT_SLOT).getLinks()
                    : new String[0];
            if (nextIds.length > 0) {
                StringBuilder nextSb = new StringBuilder("     next -> [");
                for (int i = 0; i < nextIds.length; i++) {
                    Integer nextIdx = indexMap.get(nextIds[i]);
                    nextSb.append(nextIdx != null ? nextIdx : "?");
                    if (i < nextIds.length - 1) nextSb.append(", ");
                }
                nextSb.append("]");
                lines.add(nextSb.toString());
            }
        }

        // values section
        if (!valueGlyphIds.isEmpty()) {
            lines.add("");
            lines.add("  Values:");

            for (String vId : valueGlyphIds) {
                Glyph v = hex.get(vId);
                if (v == null) continue;

                StringBuilder sb = new StringBuilder();
                sb.append("  #").append(indexMap.get(vId)).append(" ").append(formatGlyph(v));

                List<String> parts = new ArrayList<>();
                for (Map.Entry<String, com.riprod.hexcode.core.common.glyphs.component.Slot> entry : v.getSlots().entrySet()) {
                    String key = entry.getKey();
                    if (com.riprod.hexcode.core.common.glyphs.component.Glyph.NEXT_SLOT.equals(key)) continue;
                    for (String linkId : entry.getValue().getLinks()) {
                        Glyph nested = hex.get(linkId);
                        if (nested == null) continue;
                        parts.add(key + " <- #" + indexMap.get(nested.getId()) + " " + shortName(nested.getGlyphId()));
                    }
                }
                if (!parts.isEmpty()) {
                    sb.append(" | ").append(String.join(", ", parts));
                }

                lines.add(sb.toString());
            }
        }

        for (String line : lines) {
            send(playerRef, line);
        }
    }

    private void collectValues(Hex hex, Glyph glyph, Set<String> valueGlyphIds, Map<String, Integer> indexMap) {
        for (com.riprod.hexcode.core.common.glyphs.component.Slot slot : glyph.getSlots().values()) {
            for (String refId : slot.getLinks()) {
                Glyph v = hex.get(refId);
                if (v != null && valueGlyphIds.add(v.getId())) {
                    collectValues(hex, v, valueGlyphIds, indexMap);
                }
            }
        }
    }

    private String formatGlyph(Glyph glyph) {
        StringBuilder sb = new StringBuilder();
        sb.append(shortName(glyph.getGlyphId()));

        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset != null) {
            sb.append(" mana=").append(asset.getManaConsumption());
        }

        sb.append(" vol=").append(String.format("%.2f", glyph.getVolatility()));
        sb.append(" eff=").append(String.format("%.2f", glyph.getEfficiency()));
        return sb.toString();
    }

    private String shortName(String glyphId) {
        int colon = glyphId.indexOf(':');
        String name = colon >= 0 ? glyphId.substring(colon + 1) : glyphId;
        if (name.startsWith("glyph_")) {
            name = name.substring(6);
        }
        return name;
    }

    private void send(PlayerRef playerRef, String message) {
        playerRef.sendMessage(Message.raw(message));
        LOGGER.atInfo().log(message);
    }
}
