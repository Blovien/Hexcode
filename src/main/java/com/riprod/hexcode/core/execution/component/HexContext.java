package com.riprod.hexcode.core.execution.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.glyphs.variables.HexVar;

public class HexContext {
    private HexRoot root;
    private Ref<EntityStore> casterRef;
    private CommandBuffer<EntityStore> accessor;
    private ComponentAccessor<ChunkStore> chunkAccessor;
    private Map<UUID, Glyph> spellGraph;
    private Map<Integer, HexVar> variables;

    /** Initial HexContext object before initialization */
    public HexContext(HexRoot root, Map<UUID, Glyph> spellGraph) {
        this.root = root;
        this.casterRef = root.getSourceRef();
        this.accessor = null;
        this.chunkAccessor = null;
        this.spellGraph = spellGraph;
        this.variables = new HashMap<>();
    }

    public HexContext(HexRoot root, CommandBuffer<EntityStore> accessor,
            ComponentAccessor<ChunkStore> chunkAccessor, Map<UUID, Glyph> spellGraph) {
        this.root = root;
        this.casterRef = root.getSourceRef();
        this.accessor = accessor;
        this.chunkAccessor = chunkAccessor;
        this.spellGraph = spellGraph;
        this.variables = new HashMap<>();
    }

    /** Getters and Setters */
    public HexRoot getRoot() {
        return root;
    }

    public Ref<EntityStore> getCasterRef() {
        return casterRef;
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

    public Map<UUID, Glyph> getSpellGraph() {
        return spellGraph;
    }

    public Glyph getGlyph(UUID id) {
        return spellGraph.get(id);
    }

    public Map<Integer, HexVar> getVariables() {
        return variables;
    }

    public HexVar getVariable(int slot) {
        return variables.get(slot);
    }

    public void setVariables(Map<Integer, HexVar> variables) {
        this.variables = variables;
    }

    public void setVariable(int slot, HexVar value) {
        this.variables.put(slot, value);
    }

    /** Utility Functions */

    public HexContext copy() {
        // shallow copy everything except the variables map, which should be a deep copy
        HexContext copy = new HexContext(this.root, this.accessor, this.chunkAccessor, this.spellGraph);
        copy.variables = new HashMap<>(this.variables);
        return copy;
    }

    public void toStringWalk(UUID id, StringBuilder sb, String prefix, boolean last, Set<UUID> visited) {
        Glyph node = spellGraph.get(id);
        String connector = last ? "└── " : "├── ";
        String shortId = id.toString().substring(0, 8);

        if (node == null) {
            sb.append(prefix).append(connector).append(shortId).append(" [missing]\n");
            return;
        }

        sb.append(prefix).append(connector).append(node.getGlyphId())
                .append(" (").append(shortId).append(")")
                .append(" acc=").append(String.format("%.2f", node.getVolatility()))
                .append(" spd=").append(String.format("%.2f", node.getEfficiency()));
        if (!node.getInputs().isEmpty())
            sb.append(" nums=").append(node.getInputs());
        if (!node.getOutputs().isEmpty())
            sb.append(" vars=").append(node.getOutputs());

        if (!visited.add(id)) {
            sb.append(" [cycle]\n");
            return;
        }

        sb.append("\n");
        String childPrefix = prefix + (last ? "    " : "│   ");
        for (int i = 0; i < node.getNext().size(); i++) {
            toStringWalk(node.getNext().get(i), sb, childPrefix, i == node.getNext().size() - 1, visited);
        }

        visited.remove(id);
    }
}
