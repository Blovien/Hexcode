# Hytale Interaction System

## Overview
The Interaction System handles all player interactions with blocks, entities, and items through a chain-based execution model with server-client synchronization.

## Package Locations
- Module: `com.hypixel.hytale.server.core.modules.interaction`
- Manager: `com.hypixel.hytale.server.core.entity`
- Configs: `com.hypixel.hytale.server.core.modules.interaction.interaction.config`

## Asset Locations
- Interactions: `Assets/Server/Item/Interactions/`
- Root interactions: `Assets/Server/Item/RootInteractions/`

## How Interactions Work

```
Player Input → InteractionManager → InteractionChain → Operations
     ↓                                      ↓
  MouseClick                         PlaceBlock, BreakBlock,
                                     UseEntity, Damage, etc.
```

## InteractionModule

```java
public class InteractionModule extends JavaPlugin {
    public static InteractionModule get();

    // Component types
    public ComponentType<EntityStore, InteractionManager> getInteractionManagerComponent();
    public ComponentType<ChunkStore, PlacedByInteractionComponent> getPlacedByComponentType();
    public ComponentType<ChunkStore, TrackedPlacement> getTrackedPlacementComponentType();

    // Resources
    public ResourceType<ChunkStore, BlockCounter> getBlockCounterResourceType();
}
```

## InteractionManager

Per-entity component managing interaction chains:

```java
public class InteractionManager implements Component<EntityStore> {
    public static final double MAX_REACH = 8.0;

    // Start interaction chain
    public boolean tryStartChain(
        Ref<EntityStore> ref,
        CommandBuffer<EntityStore> commandBuffer,
        InteractionType type,
        InteractionContext context,
        RootInteraction rootInteraction
    );

    // Start unconditionally
    public void startChain(
        Ref<EntityStore> ref,
        CommandBuffer<EntityStore> commandBuffer,
        InteractionType type,
        InteractionContext context,
        RootInteraction rootInteraction
    );

    // Process mouse input
    public void doMouseInteraction(
        Ref<EntityStore> ref,
        MouseInteraction mouseInteraction,
        ComponentAccessor<EntityStore> componentAccessor,
        CommandBuffer<EntityStore> commandBuffer
    );

    // Tick active chains
    public void tick();

    // Check if can run
    public boolean canRun(
        InteractionType type,
        RootInteraction rootInteraction,
        InteractionContext context
    );
}
```

## InteractionChain

Execution state of an interaction:

```java
public class InteractionChain {
    // State
    public InteractionState getServerState();
    public InteractionState getClientState();
    public void updateServerState(InteractionState state);

    // Context
    public InteractionType getType();
    public RootInteraction getRootInteraction();
    public InteractionContext getContext();

    // Progress
    public int getOperationCounter();
    public long getTimestamp();

    // Forking
    public InteractionChain fork(
        InteractionContext forkedContext,
        RootInteraction forkedRootInteraction
    );
}
```

## InteractionContext

```java
public class InteractionContext {
    // Entity access
    public Ref<EntityStore> getEntity();
    public CommandBuffer<EntityStore> getCommandBuffer();

    // Item context
    public ItemStack getHeldItem();
    public byte getHeldItemSlot();
    public ItemContext createHeldItemContext();

    // Metadata
    public DynamicMetaStore<InteractionContext> getMetaStore();

    // Factory
    public static InteractionContext forInteraction(
        InteractionManager manager,
        Ref<EntityStore> entity,
        InteractionType type,
        CommandBuffer<EntityStore> commandBuffer
    );
}
```

## Interaction Types

| Type | Description |
|------|-------------|
| `Held` | Primary held item (left click) |
| `HeldOffhand` | Secondary (right click) |
| `Equipped` | Equipment interactions |

## Interaction States

| State | Description |
|-------|-------------|
| `NotFinished` | Still executing |
| `Finished` | Completed successfully |
| `Failed` | Operation failed |
| `Cancelled` | Cancelled |

## Context Metadata Keys

| Key | Type | Description |
|-----|------|-------------|
| `TARGET_BLOCK` | Vector3i | Target block position |
| `TARGET_ENTITY` | Ref | Target entity |
| `TARGET_SLOT` | int | Inventory slot |
| `HIT_LOCATION` | Vector4d | Hit point |
| `HIT_DETAIL` | varies | Hit details |

## Accessing InteractionManager

```java
Ref<EntityStore> entityRef = /* entity reference */;
InteractionManager manager = componentAccessor.getComponent(
    entityRef,
    InteractionModule.get().getInteractionManagerComponent()
);
```

## Starting an Interaction

```java
// Get root interaction
RootInteraction root = RootInteraction.getAssetMap().getAsset("PlaceBlock");

// Create context
InteractionContext context = InteractionContext.forInteraction(
    manager,
    entityRef,
    InteractionType.Held,
    commandBuffer
);

// Set target in metadata
context.getMetaStore().put(Interaction.TARGET_BLOCK, targetBlockPos);

// Try to start (respects cooldowns)
boolean started = manager.tryStartChain(
    entityRef,
    commandBuffer,
    InteractionType.Held,
    context,
    root
);
```

## Checking Interaction Availability

```java
if (manager.canRun(InteractionType.Held, rootInteraction, context)) {
    // Interaction available
} else {
    // On cooldown or blocked
}
```

## RootInteraction Configuration

```java
public class RootInteraction {
    String id;
    String[] interactionIds;  // Operations to execute
    InteractionCooldown cooldown;
    Map<GameMode, RootInteractionSettings> settings;
    InteractionRules rules;

    public static AssetMap<RootInteraction> getAssetMap();
}
```

### JSON Example
```json
{
  "Id": "MyPlugin_CustomInteraction",
  "InteractionIds": [
    "MyPlugin_Operation1",
    "MyPlugin_Operation2"
  ],
  "Cooldown": {
    "CooldownId": "myplugin_cooldown",
    "Cooldown": 0.5
  }
}
```

## InteractionCooldown

```java
public class InteractionCooldown {
    String cooldownId;        // Shared cooldown identifier
    float cooldown;           // Duration in seconds
    float[] chargeTimes;      // Charge levels
    boolean skipCooldownReset;
    boolean interruptRecharge;
    boolean clickBypass;
}
```

## Built-in Interaction Types

Over 60 interaction types:

| Category | Examples |
|----------|----------|
| Block | PlaceBlock, BreakBlock, DamageBlock |
| Entity | UseEntity, Damage, Mount |
| Camera | Camera, CameraTarget, CameraShake |
| Effects | SpawnParticle, PlaySound, CreateLight |
| Combat | Knockback, AOECircle, AOECylinder |
| UI | OpenCustomUI, OpenInventory |
| Physics | LaunchProjectile, LaunchEntity |

## Overlap Behavior

| Behavior | Description |
|----------|-------------|
| `Allow` | Both can run |
| `Block` | New blocked |
| `Interrupt` | Old cancelled |

## Item Interactions

```json
{
  "Interactions": {
    "Primary": "Root_Weapon_Sword_Primary",
    "Secondary": "Root_Weapon_Sword_Secondary_Guard"
  },
  "InteractionVars": {
    "Swing_Left_Damage": {
      "Interactions": [{
        "Parent": "Weapon_Sword_Primary_Swing_Left_Damage",
        "DamageCalculator": {
          "BaseDamage": { "Physical": 10 }
        }
      }]
    }
  }
}
```

## Block Interactions

```json
{
  "Interactions": {
    "Primary": "MyPlugin_BlockPrimary",
    "Secondary": "MyPlugin_BlockSecondary"
  },
  "InteractionHint": "ui.interaction.open"
}
```

## Walking Chain for Data

```java
SingleCollector<BallisticData> collector = new SingleCollector<>();

manager.walkChain(
    entityRef,
    collector,
    InteractionType.Held,
    rootInteraction,
    componentAccessor
);

BallisticData data = collector.getResult();
```

## Custom Interaction Operations

Define operations in JSON:

```json
{
  "Id": "MyPlugin_CustomOperation",
  "Type": "Damage",
  "DamageCalculator": {
    "BaseDamage": { "Physical": 15 }
  },
  "Effects": {
    "WorldSoundEventId": "SFX_Hit",
    "WorldParticles": [{ "SystemId": "Impact_01" }]
  }
}
```

## Damage Calculator

```json
{
  "DamageCalculator": {
    "BaseDamage": {
      "Physical": 10,
      "Fire": 5
    },
    "Type": "Absolute",
    "Modifier": 1.0
  }
}
```

## Effects Configuration

```json
{
  "Effects": {
    "WorldSoundEventId": "SFX_Attack",
    "LocalSoundEventId": "SFX_Attack_Local",
    "WorldParticles": [{ "SystemId": "Particles_Attack" }],
    "Knockback": {
      "Force": 1.0,
      "VelocityY": 3.0
    }
  }
}
```

## Best Practices

1. **Use cooldowns**: Prevent spam with appropriate cooldowns
2. **Clean up**: Cancel chains when entity is removed
3. **Validate targets**: Check targets are valid before interaction
4. **Use context**: Store interaction data in context metadata
5. **Chain operations**: Compose complex behaviors from simple operations
6. **Test timing**: Ensure cooldowns and charges feel good

## Documentation Reference
- Interaction overview: `/docs/src/content/docs/api-reference/interaction/overview.mdx`
- Block tracking: `/docs/src/content/docs/api-reference/interaction/block-tracking.mdx`
- Custom interactions: `/docs/src/content/docs/api-reference/interaction/custom-interactions.mdx`
- Java operations: `/docs/src/content/docs/api-reference/interaction/java-operations.mdx`
