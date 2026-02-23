package com.riprod.hexcode.core.crafting.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PedestalComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, PedestalComponent> componentType;

    private Vector3i blockPosition;
    private PedestalState pedestalState = PedestalState.OFF;
    private Ref<EntityStore> essenceDisplayRef = null;
    private Ref<EntityStore> bookDisplayRef = null;
    private String essenceItemId = null;
    private String bookItemId = null;
    private List<Vector3i> obeliskPositions = new ArrayList<>();
    private Set<Ref<EntityStore>> playersInRange = new HashSet<>();
    private float detectionRadius = 8.0f;
    private float transitionTimer = 0.0f;
    private float activatingDuration = 2.0f;
    private float deactivatingDuration = 1.5f;
    private int obeliskScanCounter = 0;
    private int obeliskPowerTotal = 0;

    public PedestalComponent() {
    }

    public static void setComponentType(ComponentType<EntityStore, PedestalComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, PedestalComponent> getComponentType() {
        return componentType;
    }

    public Vector3i getBlockPosition() {
        return blockPosition;
    }

    public void setBlockPosition(Vector3i blockPosition) {
        this.blockPosition = blockPosition;
    }

    public PedestalState getPedestalState() {
        return pedestalState;
    }

    public void setPedestalState(PedestalState pedestalState) {
        this.pedestalState = pedestalState;
    }

    public Ref<EntityStore> getEssenceDisplayRef() {
        return essenceDisplayRef;
    }

    public void setEssenceDisplayRef(@Nullable Ref<EntityStore> essenceDisplayRef) {
        this.essenceDisplayRef = essenceDisplayRef;
    }

    public Ref<EntityStore> getBookDisplayRef() {
        return bookDisplayRef;
    }

    public void setBookDisplayRef(@Nullable Ref<EntityStore> bookDisplayRef) {
        this.bookDisplayRef = bookDisplayRef;
    }

    public String getEssenceItemId() {
        return essenceItemId;
    }

    public void setEssenceItemId(@Nullable String essenceItemId) {
        this.essenceItemId = essenceItemId;
    }

    public String getBookItemId() {
        return bookItemId;
    }

    public void setBookItemId(@Nullable String bookItemId) {
        this.bookItemId = bookItemId;
    }

    public List<Vector3i> getObeliskPositions() {
        return obeliskPositions;
    }

    public void setObeliskPositions(List<Vector3i> obeliskPositions) {
        this.obeliskPositions = obeliskPositions;
    }

    public Set<Ref<EntityStore>> getPlayersInRange() {
        return playersInRange;
    }

    public float getDetectionRadius() {
        return detectionRadius;
    }

    public void setDetectionRadius(float detectionRadius) {
        this.detectionRadius = detectionRadius;
    }

    public float getTransitionTimer() {
        return transitionTimer;
    }

    public void setTransitionTimer(float transitionTimer) {
        this.transitionTimer = transitionTimer;
    }

    public float getActivatingDuration() {
        return activatingDuration;
    }

    public float getDeactivatingDuration() {
        return deactivatingDuration;
    }

    public int getObeliskScanCounter() {
        return obeliskScanCounter;
    }

    public void setObeliskScanCounter(int obeliskScanCounter) {
        this.obeliskScanCounter = obeliskScanCounter;
    }

    public int getObeliskPowerTotal() {
        return obeliskPowerTotal;
    }

    public void setObeliskPowerTotal(int obeliskPowerTotal) {
        this.obeliskPowerTotal = obeliskPowerTotal;
    }

    @Nonnull
    @Override
    public PedestalComponent clone() {
        PedestalComponent copy = new PedestalComponent();
        copy.blockPosition = this.blockPosition;
        copy.pedestalState = this.pedestalState;
        copy.essenceDisplayRef = this.essenceDisplayRef;
        copy.bookDisplayRef = this.bookDisplayRef;
        copy.essenceItemId = this.essenceItemId;
        copy.bookItemId = this.bookItemId;
        copy.obeliskPositions = new ArrayList<>(this.obeliskPositions);
        copy.playersInRange = new HashSet<>(this.playersInRange);
        copy.detectionRadius = this.detectionRadius;
        copy.transitionTimer = this.transitionTimer;
        copy.activatingDuration = this.activatingDuration;
        copy.deactivatingDuration = this.deactivatingDuration;
        copy.obeliskScanCounter = this.obeliskScanCounter;
        copy.obeliskPowerTotal = this.obeliskPowerTotal;
        return copy;
    }
}
