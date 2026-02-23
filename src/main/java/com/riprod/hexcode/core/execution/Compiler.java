package com.riprod.hexcode.core.execution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.core.execution.component.HexGraph;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.values.HexVal;

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
    List<HexVal> nums = glyph.getInputs();
    node.setInputs(nums);
    List<Integer> vars = glyph.getOutputs();
    node.setOutputs(vars);

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
