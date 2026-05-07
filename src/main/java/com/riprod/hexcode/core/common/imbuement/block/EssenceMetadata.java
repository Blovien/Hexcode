package com.riprod.hexcode.core.common.imbuement.block;

import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.ColorLight;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.riprod.hexcode.core.state.execution.component.HexColors;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class EssenceMetadata {

    private static final Pattern ESSENCE_ID = Pattern
            .compile("^Ingredient_([A-Za-z]+)_Essence(_Concentrated)?$");

    private EssenceMetadata() {
    }

    @Nonnull
    public static Optional<Parsed> parse(@Nullable String itemId) {
        if (itemId == null || itemId.isEmpty()) return Optional.empty();
        Matcher m = ESSENCE_ID.matcher(itemId);
        if (!m.matches()) return Optional.empty();
        String element = m.group(1).toLowerCase();
        boolean concentrated = m.group(2) != null;
        return Optional.of(new Parsed(itemId, element, concentrated));
    }

    @Nullable
    public static HexColors colorsFor(@Nonnull String itemId) {
        Item item = Item.getAssetMap() != null ? Item.getAssetMap().getAsset(itemId) : null;
        if (item == null) return null;
        ColorLight light = item.getLight();
        if (light == null) return null;
        Color primary = new Color(light.red, light.green, light.blue);
        HexColors colors = new HexColors();
        colors.setPrimaryColor(primary);
        colors.setSecondaryColor(primary.clone());
        colors.setPrimaryAlpha(1.0f);
        return colors;
    }

    public static final class Parsed {
        private final String itemId;
        private final String element;
        private final boolean concentrated;

        Parsed(String itemId, String element, boolean concentrated) {
            this.itemId = itemId;
            this.element = element;
            this.concentrated = concentrated;
        }

        public String getItemId() { return itemId; }
        public String getElement() { return element; }
        public boolean isConcentrated() { return concentrated; }

        @Override
        public String toString() {
            return "Essence{" + itemId + ", element=" + element + ", concentrated=" + concentrated + "}";
        }
    }
}
