package com.riprod.hexcode.core.execute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.core.execute.component.HexGraph;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;

public class Compiler {

  private Compiler() {
  }

  public static HexGraph compile(GlyphComponent root) {
    Map<UUID, Glyph> nodes = new HashMap<>();
    flatten(root, nodes);
    return new HexGraph(root.getId(), nodes);
  }

  private static void flatten(GlyphComponent glyph, Map<UUID, Glyph> out) {
    if (out.containsKey(glyph.getId()))
      return;

    Glyph node = new Glyph();
    node.setGlyphId(glyph.getGlyphId());
    node.setAccuracy(glyph.getAccuracy());
    node.setSpeed(glyph.getSpeed());
    List<Integer> nums = glyph.getNumbers();
    for (int i = 0; i < nums.size(); i++)
      node.setNumber(i, nums.get(i));
    List<Integer> vars = glyph.getVariables();
    for (int i = 0; i < vars.size(); i++)
      node.setVariable(i, vars.get(i));

    List<UUID> next = new ArrayList<>();
    for (GlyphComponent child : glyph.getChildren()) {
      next.add(child.getId());
    }
    node.setNext(next);

    out.put(glyph.getId(), node);

    for (GlyphComponent child : glyph.getChildren()) {
      flatten(child, out);
    }
  }
}
