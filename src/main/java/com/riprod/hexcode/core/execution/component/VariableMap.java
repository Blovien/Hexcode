
package com.riprod.hexcode.core.execution.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

public class VariableMap {

  private final Map<Integer, List<SpellVar>> slots = new HashMap<>();

  public VariableMap() {
  }

  public List<SpellVar> get(int slot) {
    return slots.computeIfAbsent(slot, k -> new ArrayList<>());
  }

  public void set(int slot, List<SpellVar> vars) {
    List<SpellVar> list = get(slot);
    list.clear();
    list.addAll(vars);
  }

  public void add(int slot, SpellVar var) {
    get(slot).add(var);
  }

  public void clear(int slot) {
    slots.remove(slot);
  }

  public void clearAll() {
    slots.clear();
  }

  public VariableMap copy() {
    VariableMap copy = new VariableMap();
    for (Map.Entry<Integer, List<SpellVar>> entry : slots.entrySet()) {
      copy.slots.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    return copy;
  }

  private static final MapCodec<SpellVar[], Map<String, SpellVar[]>> SLOTS_CODEC = new MapCodec<>(
      new ArrayCodec<>(SpellVar.CODEC, SpellVar[]::new), HashMap::new);

  public static final BuilderCodec<VariableMap> CODEC = BuilderCodec
      .builder(VariableMap.class, VariableMap::new)
      .append(new KeyedCodec<>("Slots", SLOTS_CODEC),
          (vm, map) -> {
            for (Map.Entry<String, SpellVar[]> entry : map.entrySet()) {
              int key = Integer.parseInt(entry.getKey());
              vm.slots.put(key, new ArrayList<>(List.of(entry.getValue())));
            }
          },
          vm -> {
            Map<String, SpellVar[]> map = new HashMap<>();
            for (Map.Entry<Integer, List<SpellVar>> entry : vm.slots.entrySet()) {
              map.put(entry.getKey().toString(),
                  entry.getValue().toArray(SpellVar[]::new));
            }
            return map;
          })
      .add()
      .build();
}