package com.riprod.hexcode.core.execution;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;

public class Compiler {

  private Compiler() {
  }

  public static Map<UUID, Glyph> compile(GlyphComponent root) {
    Map<UUID, Glyph> graph = new HashMap<>();
    
    compileNode(root, graph);
    return graph;
  }

  private static Glyph compileNode(GlyphComponent glyphComp, Map<UUID, Glyph> graph) {

    Glyph glyph = new Glyph(glyphComp);
    graph.put(glyph.getId(), glyph);

    for (GlyphComponent input : glyphComp.getChildren()) {
      Glyph child = compileNode(input, graph);
      glyph.addNext(child.getId());
    }

    return glyph;
  }
}
