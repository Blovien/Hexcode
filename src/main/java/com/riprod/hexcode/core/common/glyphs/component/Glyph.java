package com.riprod.hexcode.core.common.glyphs.component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.math.vector.Vector3f;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphRegistry;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class Glyph {
    public static final String NEXT_SLOT = "Next";

    private static final int MAX_RESOLVE_DEPTH = 8;
    private static final ThreadLocal<Integer> resolveDepth = ThreadLocal.withInitial(() -> 0);

    private String glyphId;
    private String id;
    private float volatility;
    private float efficiency;
    private Map<String, Slot> slots;
    private Vector3f relPosition;
    private Vector3f relRotation;

    public Glyph() {
        this.glyphId = "";
        this.id = "";
        this.volatility = 0;
        this.efficiency = 0;
        this.slots = new LinkedHashMap<>();
        this.relPosition = new Vector3f(0, 0, 0);
        this.relRotation = new Vector3f(0, 0, 0);
    }

    public Glyph(GlyphAsset glyphAsset, float volatility, float efficiency) {
        this.glyphId = glyphAsset.getId();
        this.id = UUID.randomUUID().toString();
        this.volatility = volatility;
        this.efficiency = efficiency;
        this.slots = new LinkedHashMap<>();
        this.relPosition = new Vector3f(0, 0, 0);
        this.relRotation = new Vector3f(0, 0, 0);
    }

    public String getGlyphId() {
        return glyphId;
    }

    public float getVolatility() {
        return volatility;
    }

    public void setVolatility(float volatility) {
        this.volatility = volatility;
    }

    public float getEfficiency() {
        return efficiency;
    }

    public void setEfficiency(float efficiency) {
        this.efficiency = efficiency;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Vector3f getPosition() {
        return relPosition;
    }

    public void setPosition(Vector3f position) {
        this.relPosition = position;
    }

    public Vector3f getRotation() {
        return relRotation;
    }

    public void setRotation(Vector3f rotation) {
        this.relRotation = rotation;
    }

    public Map<String, Slot> getSlots() {
        return slots;
    }

    @Nullable
    public Slot getSlot(String key) {
        return slots.get(key);
    }

    public Slot getOrCreateSlot(String key) {
        Slot existing = slots.get(key);
        if (existing != null) return existing;
        Slot created = new Slot();
        slots.put(key, created);
        return created;
    }

    public void addSlotLink(String key, String linkedGlyphId) {
        getOrCreateSlot(key).addLink(linkedGlyphId);
    }

    public void removeSlotLink(String key, String linkedGlyphId) {
        Slot slot = slots.get(key);
        if (slot == null) return;
        slot.removeLink(linkedGlyphId);
    }

    public void clearSlot(String key) {
        Slot slot = slots.get(key);
        if (slot == null) return;
        slot.clearLinks();
    }

    public void clearAllSlots() {
        for (Slot slot : slots.values()) {
            slot.clearLinks();
        }
    }

    @Nullable
    public HexVar resolveSlot(String key, HexContext hexContext) {
        int depth = resolveDepth.get();
        if (depth >= MAX_RESOLVE_DEPTH) return null;

        Slot slot = slots.get(key);
        if (slot == null) return null;

        String firstLink = slot.getFirstLink();
        if (firstLink == null) return null;

        Glyph linked = hexContext.getGlyph(firstLink);
        if (linked == null) return null;

        GlyphHandler handler = GlyphRegistry.get(linked.getGlyphId());
        if (handler == null || !handler.canResolveValue()) return null;

        resolveDepth.set(depth + 1);
        try {
            return handler.resolveValue(linked, hexContext);
        } finally {
            resolveDepth.set(depth);
        }
    }

    @Nullable
    public HexVar resolveSlotOrDefault(String key, HexContext hexContext, HexVar defaultValue) {
        HexVar resolved = resolveSlot(key, hexContext);
        return resolved != null ? resolved : defaultValue;
    }

    @Nullable
    public Integer getSlotIndex(String key, HexContext hexContext) {
        HexVar resolved = resolveSlot(key, hexContext);
        if (resolved == null) return null;
        Double number = com.riprod.hexcode.utils.SpellVarUtil.resolveNumber(resolved);
        return number != null ? number.intValue() : null;
    }

    public List<String> getNextLinks() {
        Slot slot = slots.get(NEXT_SLOT);
        if (slot == null) return List.of();
        String[] links = slot.getLinks();
        if (links.length == 0) return List.of();
        return java.util.Arrays.asList(links);
    }

    public List<HexVar> resolveSlotAll(String key, HexContext hexContext) {
        Slot slot = slots.get(key);
        if (slot == null) return List.of();

        String[] links = slot.getLinks();
        if (links.length == 0) return List.of();

        int depth = resolveDepth.get();
        if (depth >= MAX_RESOLVE_DEPTH) return List.of();

        List<HexVar> resolved = new ArrayList<>(links.length);
        for (String linkId : links) {
            Glyph linked = hexContext.getGlyph(linkId);
            if (linked == null) continue;
            GlyphHandler handler = GlyphRegistry.get(linked.getGlyphId());
            if (handler == null || !handler.canResolveValue()) continue;

            resolveDepth.set(depth + 1);
            try {
                HexVar value = handler.resolveValue(linked, hexContext);
                if (value != null) resolved.add(value);
            } finally {
                resolveDepth.set(depth);
            }
        }
        return resolved;
    }

    public static final BuilderCodec<Glyph> CODEC = buildCodec();

    @SuppressWarnings("unchecked")
    private static BuilderCodec<Glyph> buildCodec() {
        Codec<Map<String, Slot>> slotMapCodec =
                (Codec<Map<String, Slot>>) (Codec<?>) new MapCodec<>(Slot.CODEC, LinkedHashMap::new, false);
        return BuilderCodec
                .builder(Glyph.class, Glyph::new)
                .append(new KeyedCodec<>("GlyphId", Codec.STRING),
                        (n, v) -> n.glyphId = v, n -> n.glyphId)
                .add()
                .append(new KeyedCodec<>("Id", Codec.STRING),
                        (n, v) -> n.id = v, n -> n.id)
                .add()
                .append(new KeyedCodec<>("Accuracy", Codec.FLOAT),
                        (n, v) -> n.volatility = v, n -> n.volatility)
                .add()
                .append(new KeyedCodec<>("Speed", Codec.FLOAT),
                        (n, v) -> n.efficiency = v, n -> n.efficiency)
                .add()
                .<Map<String, Slot>>append(new KeyedCodec<>("Slots", slotMapCodec),
                        (n, v) -> n.slots = v != null ? new LinkedHashMap<>(v) : new LinkedHashMap<>(),
                        n -> n.slots)
                .add()
                .append(new KeyedCodec<>("RelativePosition", Codec.FLOAT_ARRAY),
                        (c, v) -> c.relPosition = new Vector3f(v[0], v[1], v[2]),
                        c -> new float[] { c.relPosition.x, c.relPosition.y, c.relPosition.z })
                .add()
                .append(new KeyedCodec<>("RelativeRotation", Codec.FLOAT_ARRAY),
                        (c, v) -> c.relRotation = new Vector3f(v[0], v[1], v[2]),
                        c -> new float[] { c.relRotation.x, c.relRotation.y, c.relRotation.z })
                .add()
                .build();
    }

    public Glyph clone() {
        Glyph clone = new Glyph();
        clone.glyphId = this.glyphId;
        clone.id = this.id;
        clone.volatility = this.volatility;
        clone.efficiency = this.efficiency;
        clone.slots = new LinkedHashMap<>();
        for (Map.Entry<String, Slot> entry : this.slots.entrySet()) {
            clone.slots.put(entry.getKey(), entry.getValue().clone());
        }
        clone.relPosition = new Vector3f(this.relPosition.x, this.relPosition.y, this.relPosition.z);
        clone.relRotation = new Vector3f(this.relRotation.x, this.relRotation.y, this.relRotation.z);
        return clone;
    }

    @Override
    public String toString() {
        return glyphId;
    }
}
