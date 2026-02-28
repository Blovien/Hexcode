package com.riprod.hexcode.core.execution.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.glyphs.variables.HexVar;
import com.riprod.hexcode.core.hexes.component.Hex;

public class HexContext {
    private HexRoot root;
    private Ref<EntityStore> casterRef;
    private CommandBuffer<EntityStore> accessor;
    private ComponentAccessor<ChunkStore> chunkAccessor;
    private Hex hex;
    private Map<Integer, HexVar> variables;

    /** Initial HexContext object before initialization */
    public HexContext(HexRoot root, Hex hex) {
        this.root = root;
        this.casterRef = root.getSourceRef();
        this.accessor = null;
        this.chunkAccessor = null;
        this.hex = hex;
        this.variables = new HashMap<>();
    }

    // For the codec
    public HexContext() {
        this.variables = new HashMap<>();
    }

    public HexContext(HexRoot root, CommandBuffer<EntityStore> accessor,
            ComponentAccessor<ChunkStore> chunkAccessor, Hex hex) {
        this.root = root;
        this.casterRef = root.getSourceRef();
        this.accessor = accessor;
        this.chunkAccessor = chunkAccessor;
        this.hex = hex;
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

    public Hex gethex() {
        return hex;
    }

    public Glyph getGlyph(String id) {
        return hex.get(id);
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
        HexContext copy = new HexContext(this.root, this.accessor, this.chunkAccessor, this.hex);
        copy.variables = new HashMap<>(this.variables);
        return copy;
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

    public static final BuilderCodec<HexContext> CODEC = BuilderCodec
            .builder(HexContext.class, HexContext::new)
            .append(new KeyedCodec<>("HexGraph", Hex.CODEC),
                    (c, v) -> c.hex = v,
                    c -> c.hex)
            .add()
            .append(new KeyedCodec<>("Variables", new MapCodec<>(HexVar.CODEC, HashMap::new)),
                    // Deserialize: Map<String, HexVar> -> Map<Integer, HexVar>
                    (c, v) -> {
                        Map<Integer, HexVar> intKeyed = new HashMap<>();
                        for (Map.Entry<String, HexVar> entry : v.entrySet()) {
                            try {
                                intKeyed.put(Integer.parseInt(entry.getKey()), entry.getValue());
                            } catch (NumberFormatException e) {
                                // Optionally handle invalid keys
                            }
                        }
                        c.variables = intKeyed;
                    },
                    // Serialize: Map<Integer, HexVar> -> Map<String, HexVar>
                    c -> {
                        Map<String, HexVar> strKeyed = new HashMap<>();
                        for (Map.Entry<Integer, HexVar> entry : c.variables.entrySet()) {
                            strKeyed.put(entry.getKey().toString(), entry.getValue());
                        }
                        return strKeyed;
                    })
            .add()
            .build();

    public HexContext clone() {

        HexContext copy = new HexContext(this.root, this.accessor, this.chunkAccessor, this.hex.clone());
        copy.variables = new HashMap<>(this.variables);

        return copy;
    }
}
