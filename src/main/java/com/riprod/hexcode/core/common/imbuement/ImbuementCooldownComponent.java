package com.riprod.hexcode.core.common.imbuement;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ImbuementCooldownComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, ImbuementCooldownComponent> componentType;

    private final Map<String, Long> cooldowns = new HashMap<>();

    public static void setComponentType(ComponentType<EntityStore, ImbuementCooldownComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, ImbuementCooldownComponent> getComponentType() {
        return componentType;
    }

    public boolean isOnCooldown(String slotKey) {
        Long expiry = cooldowns.get(slotKey);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    public void setCooldown(String slotKey, long durationMs) {
        cooldowns.put(slotKey, System.currentTimeMillis() + durationMs);
    }

    @Nonnull
    @Override
    public ImbuementCooldownComponent clone() {
        ImbuementCooldownComponent copy = new ImbuementCooldownComponent();
        copy.cooldowns.putAll(this.cooldowns);
        return copy;
    }
}
