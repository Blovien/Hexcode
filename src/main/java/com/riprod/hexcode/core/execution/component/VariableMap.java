
package com.riprod.hexcode.core.execution.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.riprod.hexcode.core.glyphs.variables.HexVar;

public class VariableMap {

  private final Map<Integer, HexVar> slots = new HashMap<>();

  public VariableMap() {
  }

  public HexVar get(int slot) {
    return slots.computeIfAbsent(slot, k -> null);
  }

  public void set(int slot, HexVar var) {
    slots.put(slot, var);
  }

  public void clear(int slot) {
    slots.remove(slot);
  }

  public void clearAll() {
    slots.clear();
  }
  public VariableMap copy() {
    VariableMap copy = new VariableMap();
    for (Map.Entry<Integer, HexVar> entry : slots.entrySet()) {
      copy.slots.put(entry.getKey(), entry.getValue());
    }
    return copy;
  }
  private static final MapCodec<HexVar, Map<String, HexVar>> SLOTS_CODEC =
    new MapCodec<>(HexVar.CODEC, HashMap::new);

  public static final BuilderCodec<VariableMap> CODEC = BuilderCodec
    .builder(VariableMap.class, VariableMap::new)
    .append(new KeyedCodec<>("Slots", SLOTS_CODEC),
      (vm, map) -> {
        for (Map.Entry<String, HexVar> entry : map.entrySet()) {
        int key = Integer.parseInt(entry.getKey());
        vm.slots.put(key, entry.getValue());
        }
      },
      vm -> {
        Map<String, HexVar> map = new HashMap<>();
        for (Map.Entry<Integer, HexVar> entry : vm.slots.entrySet()) {
        map.put(entry.getKey().toString(), entry.getValue());
        }
        return map;
      })
    .add()
    .build();
}