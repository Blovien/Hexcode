package com.riprod.hexcode.core.state.execution.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.imbuement.ImbuementData;
import com.riprod.hexcode.core.state.execution.events.CastingEventData;

public class HexContext {
    private HexRoot root;
    private CommandBuffer<EntityStore> accessor;
    private ComponentAccessor<ChunkStore> chunkAccessor;
    private Hex hex;
    private Map<String, HexVar> variables;
    private VolatilityTracker volatilityTracker;
    private HexColors colors;
    private UUID executionId = UUID.randomUUID();

    public HexContext(CommandBuffer<EntityStore> accessor, ComponentAccessor<ChunkStore> chunkAccessor, CastingEventData castingData) {
        this.accessor = accessor;
        this.chunkAccessor = chunkAccessor;
        this.root = castingData.getHexRoot();
        this.hex = castingData.getHex();
        this.variables = new HashMap<>();
        this.volatilityTracker = castingData.getVolatilityTracker();
        this.colors = castingData.getColors();
        this.executionId = UUID.randomUUID();
    }

    // For the codec
    public HexContext() {
        this.variables = new HashMap<>();
    }

    /** Getters and Setters */
    public HexRoot getRoot() {
        return root;
    }

    public CommandBuffer<EntityStore> getAccessor() {
        return accessor;
    }

    public void UpdateAccessor(CommandBuffer<EntityStore> newAccessor) {
        this.accessor = newAccessor;
    }

    public ComponentAccessor<ChunkStore> getChunkAccessor() {
        return chunkAccessor;
    }

    public void UpdateChunkAccessor(ComponentAccessor<ChunkStore> newChunkAccessor) {
        this.chunkAccessor = newChunkAccessor;
    }

    public Hex gethex() {
        return hex;
    }

    public Glyph getGlyph(String id) {
        return hex.get(id);
    }

    public Map<String, HexVar> getVariables() {
        return variables;
    }

    public HexVar getVariable(String slot) {
        return variables.get(slot);
    }

    public void setVariables(Map<String, HexVar> variables) {
        this.variables = variables;
    }

    public void setVariable(String slot, HexVar value) {
        this.variables.put(slot, value);
    }

    public VolatilityTracker getVolatilityTracker() {
        return volatilityTracker;
    }

    public void setVolatilityTracker(VolatilityTracker volatilityTracker) {
        this.volatilityTracker = volatilityTracker;
        volatilityTracker.setExecutionId(this.executionId);
    }

    public HexColors getColors() {
        return colors;
    }

    public void setColors(HexColors colors) {
        this.colors = colors;
    }

    public float getMagicPowerMultiplier() {
        return volatilityTracker != null ? volatilityTracker.getMagicPowerMultiplier() : 1.0f;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    /** Utility Functions */

    public HexContext branch() {
        HexContext branch = new HexContext();
        // persistent variables
        branch.root = this.root;
        branch.accessor = this.accessor;
        branch.chunkAccessor = this.chunkAccessor;
        branch.hex = this.hex;
        branch.volatilityTracker = this.volatilityTracker;
        // copied variables
        branch.variables = new HashMap<>(this.variables);
        if (this.colors != null)
            branch.colors = this.colors.clone();
        return branch;
    }

    public void toStringWalk(String id, StringBuilder sb, String prefix, boolean last, Set<String> visited) {
        Glyph node = hex.get(id);
        String connector = last ? "└── " : "├── ";
        String shortId = id.toString().substring(0, 8);

        if (node == null) {
            sb.append(prefix).append(connector).append(shortId).append(" [missing]\n");
            return;
        }

        sb.append(prefix).append(connector).append(node.getGlyphId())
                .append(" (").append(shortId).append(")")
                .append(" acc=").append(String.format("%.2f", node.getVolatility()))
                .append(" spd=").append(String.format("%.2f", node.getEfficiency()))
                .append(" slots=").append(node.getSlots().keySet());

        if (!visited.add(id)) {
            sb.append(" [cycle]\n");
            return;
        }

        sb.append("\n");
        String childPrefix = prefix + (last ? "    " : "│   ");
        java.util.List<String> nextLinks = node.getNextLinks();
        for (int i = 0; i < nextLinks.size(); i++) {
            toStringWalk(nextLinks.get(i), sb, childPrefix, i == nextLinks.size() - 1, visited);
        }

        visited.remove(id);
    }

    public static final BuilderCodec<HexContext> CODEC = BuilderCodec
            .builder(HexContext.class, HexContext::new)
            .append(new KeyedCodec<>("HexGraph", Hex.CODEC),
                    (c, v) -> c.hex = v,
                    c -> c.hex)
            .add()
            .append(new KeyedCodec<>("Variables", new MapCodec<>(HexVar.CODEC, HashMap::new)),
                    (c, v) -> c.variables = v,
                    c -> c.variables)
            .add()
            .append(new KeyedCodec<>("VolatilityTracker", VolatilityTracker.CODEC),
                    (c, v) -> c.volatilityTracker = v,
                    c -> c.volatilityTracker)
            .add()
            .append(new KeyedCodec<>("HexColors", HexColors.CODEC),
                    (c, v) -> c.colors = v,
                    c -> c.colors)
            .add()
            .build();

    public HexContext copy() {
        HexContext branch = new HexContext();
        // persistent variables
        branch.root = this.root.copy();
        branch.accessor = this.accessor;
        branch.chunkAccessor = this.chunkAccessor;
        branch.hex = this.hex.clone();
        branch.volatilityTracker = this.volatilityTracker.copy();
        // copied variables
        branch.variables = new HashMap<>(this.variables);
        if (this.colors != null)
            branch.colors = this.colors.clone();
        return branch;
    }
}
