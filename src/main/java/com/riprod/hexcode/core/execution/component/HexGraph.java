package com.riprod.hexcode.core.execution.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.riprod.hexcode.components.Glyph;

public class HexGraph {
  public UUID rootId;
  public Map<UUID, Glyph> nodes = new HashMap<>();

  public HexGraph() {
  }

  public HexGraph(UUID rootId, Map<UUID, Glyph> nodes) {
    this.rootId = rootId;
    this.nodes = nodes;
  }

  public Glyph getNode(UUID id) {
    return nodes.get(id);
  }

  public Glyph getRoot() {
    return nodes.get(rootId);
  }

  private static final MapCodec<Glyph, Map<String, Glyph>> NODES_CODEC = new MapCodec<>(Glyph.CODEC, HashMap::new);

  public static final BuilderCodec<HexGraph> CODEC = BuilderCodec
      .builder(HexGraph.class, HexGraph::new)
      .append(new KeyedCodec<>("Root", Codec.UUID_STRING),
          (g, v) -> g.rootId = v, g -> g.rootId)
      .add()
      .append(new KeyedCodec<>("Glyph", NODES_CODEC),
          (g, map) -> {
            for (Map.Entry<String, Glyph> entry : map.entrySet()) {
              g.nodes.put(UUID.fromString(entry.getKey()), entry.getValue());
            }
          },
          g -> {
            Map<String, Glyph> map = new HashMap<>();
            for (Map.Entry<UUID, Glyph> entry : g.nodes.entrySet()) {
              map.put(entry.getKey().toString(), entry.getValue());
            }
            return map;
          })
      .add()
      .build();

  @Override
  public HexGraph clone() {
    Map<UUID, Glyph> clonedNodes = new HashMap<>();
    for (Map.Entry<UUID, Glyph> entry : nodes.entrySet()) {
      Glyph node = entry.getValue();
      Glyph clonedNode = new Glyph();
      clonedNode.setGlyphId(node.getGlyphId());
      clonedNode.setAccuracy(node.getAccuracy());
      clonedNode.setSpeed(node.getSpeed());
      node.getNumbers().forEach(clonedNode::setNumber);
      node.getVariables().forEach(clonedNode::setVariable);
      clonedNode.setNext(new ArrayList<>(node.getNext()));
      clonedNodes.put(entry.getKey(), clonedNode);
    }
    return new HexGraph(rootId, clonedNodes);
  }

  @Override
  public String toString() {
    if (rootId == null || nodes.isEmpty()) return "HexGraph[empty]";
    StringBuilder sb = new StringBuilder();
    sb.append("HexGraph (").append(nodes.size()).append(" nodes)\n");
    toStringWalk(rootId, sb, "", true, new java.util.HashSet<>());
    return sb.toString().stripTrailing();
  }

  private void toStringWalk(UUID id, StringBuilder sb, String prefix, boolean last, java.util.Set<UUID> visited) {
    Glyph node = nodes.get(id);
    String connector = last ? "└── " : "├── ";
    String shortId = id.toString().substring(0, 8);

    if (node == null) {
      sb.append(prefix).append(connector).append(shortId).append(" [missing]\n");
      return;
    }

    sb.append(prefix).append(connector).append(node.getGlyphId())
        .append(" (").append(shortId).append(")")
        .append(" acc=").append(String.format("%.2f", node.getAccuracy()))
        .append(" spd=").append(String.format("%.2f", node.getSpeed()));
    if (!node.getNumbers().isEmpty()) sb.append(" nums=").append(node.getNumbers());
    if (!node.getVariables().isEmpty()) sb.append(" vars=").append(node.getVariables());

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