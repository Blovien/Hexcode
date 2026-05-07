package com.riprod.hexcode.core.state.execution.component;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.imbuement.block.BlockImbuementCapacity;

public class BlockHexRoot implements HexRoot {

    private final Vector3i blockPos;
    private final BlockImbuementCapacity.Capacity capacity;

    public BlockHexRoot(Vector3i blockPos, BlockImbuementCapacity.Capacity capacity) {
        this.blockPos = blockPos;
        this.capacity = capacity;
    }

    public Vector3i getBlockPos() {
        return blockPos;
    }

    public BlockImbuementCapacity.Capacity getCapacity() {
        return capacity;
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public Ref<EntityStore> getSourceRef() {
        return null;
    }

    @Override
    public void addDependency(HexContext ctx, Ref<EntityStore> ref) {
    }

    @Override
    public boolean tryConsumeMana(float cost, ComponentAccessor<EntityStore> accessor) {
        return true;
    }

    @Override
    public float getCurrentMana(ComponentAccessor<EntityStore> accessor) {
        return Float.MAX_VALUE;
    }

    @Override
    public boolean addMana(float amount, ComponentAccessor<EntityStore> accessor) {
        return false;
    }

    public float resolveVolatility(ComponentAccessor<EntityStore> accessor) {
        return (float) capacity.getSlots() * capacity.getVolatilityPerSlot();
    }

    public float resolveSpellPower(ComponentAccessor<EntityStore> accessor) {
        return 1.0f;
    }

    @Override
    public HexRoot copy() {
        return new BlockHexRoot(blockPos, capacity);
    }
}
