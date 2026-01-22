# Hytale NPC System

## Overview
The NPC (Non-Player Character) System manages AI-controlled entities using a role-based behavior system with support for flocking, spawning, and complex decision making.

## Package Locations
- NPC entities: `com.hypixel.hytale.server.npc`
- Flock system: `com.hypixel.hytale.server.flock`
- Role system: `com.hypixel.hytale.server.npc.role`
- Blackboard: `com.hypixel.hytale.server.npc.blackboard`

## Asset Locations
- NPC definitions: `Assets/Server/NPC/`
- NPC models: `Assets/Common/NPC/`
- Drops: `Assets/Server/Drops/`

## Core Components

### NPCEntity

```java
public class NPCEntity extends LivingEntity implements INonPlayerCharacter {
    // Role management
    public Role getRole();

    // Position tracking
    public Vector3d getLeashPoint();  // Spawn anchor
    public float getLeashHeading();

    // Spawning
    public Instant getSpawnInstant();
    public String getSpawnConfigurationName();

    // Pathing
    public PathManager getPathManager();

    // Events
    public AlarmStore getAlarmStore();
    public DamageData getDamageData();
}
```

### Getting Component Type

```java
ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
Query<EntityStore> npcQuery = npcType;
```

## Role System

Roles define NPC behavior, stats, and AI:

### Role Structure

```java
public class Role implements IAnnotatedComponentCollection {
    // Support objects
    CombatSupport combatSupport;
    StateSupport stateSupport;
    MarkedEntitySupport markedEntitySupport;
    WorldSupport worldSupport;
    EntitySupport entitySupport;
    PositionCache positionCache;

    // Stats
    int initialMaxHealth;
    double knockbackScale;
    double inertia;
    boolean invulnerable;

    // Movement
    Map<String, MotionController> motionControllers;
    MotionController activeMotionController;
    Steering bodySteering;
    Steering headSteering;

    // Behavior
    Instruction rootInstruction;
    Instruction interactionInstruction;
    Instruction deathInstruction;

    // Spawning
    String[] flockSpawnTypes;
    String[] flockAllowedRoles;
    boolean canLeadFlock;
}
```

### Role Configuration Properties

| Property | Type | Description |
|----------|------|-------------|
| `maxHealth` | int | Maximum health |
| `knockbackScale` | double | Knockback multiplier |
| `inertia` | double | Movement inertia |
| `invulnerable` | bool | Cannot take damage |
| `deathAnimationTime` | double | Death animation duration |
| `despawnAnimationTime` | float | Despawn animation duration |
| `dropListId` | string | Loot drop list |
| `balanceAsset` | string | Balance configuration |
| `hotbarItems` | string[] | Starting items |
| `armor` | string[] | Starting armor |

## NPC JSON Configuration

```json
{
  "Id": "MyPlugin_CustomNPC",
  "MaxHealth": 20,
  "Appearance": "Models/NPCs/custom_npc",
  "DropList": "MyPlugin_CustomDrops",
  "Behavior": {
    "Root": {
      "Type": "Selector",
      "Children": [
        {
          "Type": "Sequence",
          "Conditions": ["health < 50"],
          "Actions": ["Flee"]
        },
        {
          "Type": "Wander"
        }
      ]
    }
  }
}
```

## Motion Controllers

| Controller | Description |
|------------|-------------|
| `BodyMotionWander` | Random wandering |
| `BodyMotionMaintainDistance` | Keep distance from target |
| `BodyMotionFindWithTarget` | Pathfind to target |
| `BodyMotionMatchLook` | Match look direction |
| `BodyMotionFlock` | Flock movement |
| `BodyMotionTeleport` | Teleportation |

## Blackboard System

Shared memory for NPC decision making:

### Blackboard Views

| View | Description |
|------|-------------|
| `BlockTypeView` | Track block type changes |
| `BlockEventView` | Track block events |
| `EntityEventView` | Track entity events |

### Event Types

```java
public enum BlockEventType {
    PLACED,
    REMOVED,
    INTERACTED
}

public enum EntityEventType {
    SPAWNED,
    DESPAWNED,
    DAMAGED,
    KILLED
}
```

## Flock System

Flocks group NPCs for coordinated behavior:

### Flock Component

```java
public class Flock implements Component<EntityStore> {
    PersistentFlockData getFlockData();
    DamageData getDamageData();
    DamageData getLeaderDamageData();
    FlockRemovedStatus getRemovedStatus();
}
```

### FlockMembership Component

```java
public class FlockMembership implements Component<EntityStore> {
    UUID getFlockId();
    void setFlockId(UUID flockId);
    Ref<EntityStore> getFlockRef();
    Type getMembershipType();
}
```

### Membership Types

| Type | Leader | Description |
|------|--------|-------------|
| `JOINING` | No | Joining a flock |
| `MEMBER` | No | Regular member |
| `LEADER` | Yes | Flock leader |
| `INTERIM_LEADER` | Yes | Temporary leader |

### Flock Configuration

```json
{
  "Id": "MyPlugin_WolfPack",
  "MinSize": 2,
  "MaxSize": 6,
  "AllowedRoles": ["Wolf", "AlphaWolf"],
  "WeightAlignment": 1.0,
  "WeightSeparation": 1.5,
  "WeightCohesion": 1.0,
  "InfluenceRange": 20.0
}
```

### Flock Behaviors

| Behavior | Description |
|----------|-------------|
| Alignment | Match velocity with nearby members |
| Separation | Maintain distance from members |
| Cohesion | Stay close to flock center |

## Spawning System

### SpawnBeacon

Defines spawn locations:

```java
public class SpawnBeacon implements Component<EntityStore> {
    // Configured in assets
}
```

### Spawn Configuration

```json
{
  "Id": "MyPlugin_ZombieSpawn",
  "Role": "Zombie",
  "MinCount": 1,
  "MaxCount": 3,
  "SpawnChance": 0.5,
  "Conditions": {
    "TimeOfDay": "Night",
    "LightLevel": { "Max": 7 }
  }
}
```

### WorldNPCSpawn

Natural spawning configuration:

```java
LightType lightType;  // BLOCK, SKY, COMBINED
int minLightLevel;
int maxLightLevel;
```

### Spawn Suppression

```java
public class SpawnSuppressionComponent implements Component<EntityStore> {
    // Suppress spawns in area
}
```

## NPC Systems

| System | Description |
|--------|-------------|
| `NPCPreTickSystem` | Pre-tick setup |
| `StateEvaluatorSystem` | State transitions |
| `SteeringSystem` | Calculate steering |
| `ComputeVelocitySystem` | Compute velocity |
| `AvoidanceSystem` | Collision avoidance |
| `MovementStatesSystem` | Movement states |
| `RoleChangeSystem` | Handle role changes |
| `NPCDamageSystems` | Damage handling |
| `NPCDeathSystems` | Death handling |
| `NPCInteractionSystems` | Player interactions |

## Instructions and Actions

### Common Actions

```java
ActionRecomputePath       // Recalculate path
ActionOverrideAltitude    // Set altitude
ActionFlockJoin           // Join a flock
ActionFlockLeave          // Leave flock
ActionFlockBeacon         // Respond to beacon
ActionFlockSetTarget      // Set flock target
ActionTriggerSpawnBeacon  // Trigger spawn
```

### Sensors

```java
SensorOnGround            // Ground detection
SensorFlockLeader         // Leader detection
SensorFlockCombatDamage   // Combat damage sensing
SensorInflictedDamage     // Damage dealt sensing
```

## Creating NPCs Programmatically

```java
// Create NPC holder
Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

// Add NPCEntity component
NPCEntity npc = new NPCEntity();
holder.addComponent(NPCEntity.getComponentType(), npc);

// Add transform
TransformComponent transform = new TransformComponent(x, y, z, 0, 0, 0);
holder.addComponent(TransformComponent.getComponentType(), transform);

// Add UUID
holder.addComponent(UUIDComponent.getComponentType(),
    new UUIDComponent(UUID.randomUUID()));

// Spawn in world
Store<EntityStore> store = world.getEntityStore().getStore();
Ref<EntityStore> ref = store.spawn(holder);
```

## NPC Tracking with RefSystem

```java
public class MyNPCSystem extends RefSystem<EntityStore> {
    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    public void onEntityAdded(Ref<EntityStore> ref, AddReason reason,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        // Handle NPC spawn
    }

    @Override
    public void onEntityRemoved(Ref<EntityStore> ref, RemoveReason reason,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        // Handle NPC removal
    }
}
```

## Expression Syntax

NPCs support expression evaluation for conditions:

```java
"health < 50"
"distance(target) > 10"
"hasTag(\"hostile\")"
```

## Drop Configuration

```json
{
  "Id": "MyPlugin_CustomDrops",
  "Drops": [
    {
      "ItemId": "Ingredient_Bone",
      "MinQuantity": 0,
      "MaxQuantity": 2,
      "Chance": 0.5
    },
    {
      "ItemId": "Rare_Item",
      "MinQuantity": 1,
      "MaxQuantity": 1,
      "Chance": 0.05
    }
  ]
}
```

## Best Practices

1. **Use roles**: Define behavior through role configurations
2. **Leverage flocks**: Group related NPCs for coordination
3. **Optimize queries**: Use efficient ECS queries
4. **Handle lifecycle**: Properly handle add/remove events
5. **Use blackboards**: Share state through blackboard system
6. **Configure spawning**: Use spawn beacons for controlled spawning
7. **Prefix IDs**: Use plugin name prefix for custom NPCs

## Documentation Reference
- NPC overview: `/docs/src/content/docs/api-reference/npc/overview.mdx`
- NPC models: `/docs/src/content/docs/api-reference/assets/npcs/models.mdx`
- NPC behaviors: `/docs/src/content/docs/api-reference/assets/npcs/behaviors.mdx`
- NPC groups: `/docs/src/content/docs/api-reference/assets/npcs/groups.mdx`
- NPC attachments: `/docs/src/content/docs/api-reference/assets/npcs/attachments.mdx`
