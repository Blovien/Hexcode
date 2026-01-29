package com.riprod.hexcode.entity;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * ECS Component attached to HexNode entities for system tracking.
 *
 * <p>This component enables:
 * <ul>
 *   <li>Querying all HexNode entities via ECS systems</li>
 *   <li>Tracking owner player for FacePlayerSystem</li>
 *   <li>Debugging/identification via nodeId</li>
 * </ul>
 *
 * <p>Replaces GlyphComponent with a simpler structure focused on the unified HexNode system.
 */
public class HexNodeComponent implements Component<EntityStore> {
    private static ComponentType<EntityStore, HexNodeComponent> componentType;

    /**
     * CODEC for serialization - required for ECS component registration.
     */
    public static final BuilderCodec<HexNodeComponent> CODEC =
        BuilderCodec.builder(HexNodeComponent.class, HexNodeComponent::new)
            .append(
                new KeyedCodec<>("NodeId", Codec.STRING),
                (c, v) -> c.nodeId = v,
                c -> c.nodeId
            )
            .add()
            .build();

    /** Reference to the owner player entity */
    private transient Ref<EntityStore> ownerPlayerRef;

    /** Unique identifier for this node (for debugging/identification) */
    private String nodeId;

    /**
     * Default constructor required for ECS component registration.
     */
    public HexNodeComponent() {
        this.ownerPlayerRef = null;
        this.nodeId = "";
    }

    /**
     * Create a new HexNodeComponent.
     *
     * @param ownerPlayerRef Reference to the player who owns this node
     * @param nodeId         Unique identifier for this node
     */
    public HexNodeComponent(Ref<EntityStore> ownerPlayerRef, String nodeId) {
        this.ownerPlayerRef = ownerPlayerRef;
        this.nodeId = nodeId;
    }

    /**
     * Get the owner player reference.
     *
     * @return Reference to the player who owns this node
     */
    public Ref<EntityStore> getOwnerPlayerRef() {
        return ownerPlayerRef;
    }

    /**
     * Set the owner player reference.
     *
     * @param ownerPlayerRef Reference to the player
     */
    public void setOwnerPlayerRef(Ref<EntityStore> ownerPlayerRef) {
        this.ownerPlayerRef = ownerPlayerRef;
    }

    /**
     * Get the node ID.
     *
     * @return The unique identifier for this node
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Set the node ID.
     *
     * @param nodeId The unique identifier for this node
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Get the component type for registration.
     */
    public static ComponentType<EntityStore, HexNodeComponent> getComponentType() {
        return componentType;
    }

    /**
     * Set the component type (called during plugin initialization).
     */
    public static void setComponentType(ComponentType<EntityStore, HexNodeComponent> type) {
        componentType = type;
    }

    /**
     * Clone this component (required by Component interface).
     */
    @Override
    public Component<EntityStore> clone() {
        return new HexNodeComponent(ownerPlayerRef, nodeId);
    }
}
