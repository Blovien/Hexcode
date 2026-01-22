# Hytale ECS (Entity Component System)

## Overview
Hytale uses an Entity Component System (ECS) architecture for game object management. This separates data (Components) from behavior (Systems) and uses Entities as identifiers with Stores managing component data.

## Package Locations
- ECS Core: `com.hypixel.hytale.server.ecs`
- Components: `com.hypixel.hytale.server.core.modules.entity.component`
- Entity Store: `com.hypixel.hytale.server.core.universe.world.storage.EntityStore`
- Chunk Store: `com.hypixel.hytale.server.core.universe.world.storage.ChunkStore`

## Core Concepts

### Store Types

| Store | Purpose | Examples |
|-------|---------|----------|
| `EntityStore` | Dynamic entities | Players, mobs, items, projectiles |
| `ChunkStore` | Block-level data | Block states, placed blocks |

### Key Classes

| Class | Description |
|-------|-------------|
| `Store<S>` | Container managing components for a store type |
| `Ref<S>` | Reference to entity in store (like pointer) |
| `Holder<S>` | Portable entity data (can exist outside store) |
| `Component<S>` | Data attached to entity |
| `ComponentType<S,C>` | Type registration for component |
| `Query<S>` | Filter for finding entities |

## Components

Components are pure data containers:

```java
public class HealthComponent implements Component<EntityStore> {
    private int health;
    private int maxHealth;

    public HealthComponent(int maxHealth) {
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public int getMaxHealth() { return maxHealth; }

    // CODEC for serialization
    public static final BuilderCodec<HealthComponent> CODEC =
        BuilderCodec.builder(HealthComponent.class, () -> new HealthComponent(20))
            .append(new KeyedCodec<>("Health", Codec.INTEGER),
                (c, v) -> c.health = v, c -> c.health)
            .add()
            .append(new KeyedCodec<>("MaxHealth", Codec.INTEGER),
                (c, v) -> c.maxHealth = v, c -> c.maxHealth)
            .add()
            .build();
}
```

### Registering Components

```java
@Override
protected void setup() {
    ComponentType<EntityStore, HealthComponent> healthType =
        getEntityStoreRegistry().registerComponent(
            HealthComponent.class,
            () -> new HealthComponent(20),
            HealthComponent.CODEC
        );
}
```

## Working with Refs

`Ref<S>` is an entity reference:

```java
// Check validity
if (ref.isValid()) {
    // Entity still exists
}

// Get from PlayerRef
PlayerRef player = event.getPlayerRef();
Ref<EntityStore> playerRef = player.getReference();

// Get component from ref
Store<EntityStore> store = world.getEntityStore().getStore();
HealthComponent health = store.getComponent(ref, healthComponentType);

// Check if has component
boolean hasHealth = store.hasComponent(ref, healthComponentType);
```

## Working with Stores

```java
// Get store from world
EntityStore entityStore = world.getEntityStore();
Store<EntityStore> store = entityStore.getStore();

// Add component
store.addComponent(ref, componentType, new MyComponent());

// Remove component
store.removeComponent(ref, componentType);

// Get component
MyComponent comp = store.getComponent(ref, componentType);

// Spawn new entity
Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
holder.addComponent(transformType, new TransformComponent(x, y, z, 0, 0, 0));
holder.addComponent(uuidType, new UUIDComponent(UUID.randomUUID()));
Ref<EntityStore> newRef = store.spawn(holder);

// Remove entity
store.despawn(ref);
```

## Holders

`Holder<S>` is portable entity data:

```java
// Create holder
Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

// Add components
holder.addComponent(componentType, component);

// Spawn into store
Ref<EntityStore> ref = store.spawn(holder);

// Convert entity to holder (for transfer/serialization)
Holder<EntityStore> entityHolder = entity.toHolder();
```

## Queries

Queries filter entities by components:

```java
// Single component query
Query<EntityStore> healthQuery = healthComponentType;

// Multiple required components (AND)
Query<EntityStore> andQuery = new AndQuery<>(
    healthComponentType,
    transformComponentType
);

// Any of components (OR)
Query<EntityStore> orQuery = new OrQuery<>(
    playerType,
    npcType
);

// Excluding components
Query<EntityStore> excludeQuery = new ExcludeQuery<>(
    healthQuery,
    invulnerableComponentType
);
```

## Systems

Systems process entities matching a query:

```java
public class HealthRegenSystem extends EntityTickingSystem<EntityStore> {
    private final ComponentType<EntityStore, HealthComponent> healthType;

    @Override
    public Query<EntityStore> getQuery() {
        return healthType;
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        HealthComponent health = chunk.getComponent(index, healthType);

        if (health.getHealth() < health.getMaxHealth()) {
            health.setHealth(health.getHealth() + 1);
        }
    }
}
```

### System Types

| Type | Base Class | Description |
|------|------------|-------------|
| Ticking | `EntityTickingSystem` | Runs every game tick |
| Ref | `RefSystem` | Handles entity add/remove |
| One-shot | `EntitySystem` | Runs once per invocation |

### RefSystem Example

```java
public class EntityTrackerSystem extends RefSystem<EntityStore> {
    @Override
    public Query<EntityStore> getQuery() {
        return myComponentType;
    }

    @Override
    public void onEntityAdded(Ref<EntityStore> ref, AddReason reason,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        // Entity with component was added
    }

    @Override
    public void onEntityRemoved(Ref<EntityStore> ref, RemoveReason reason,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        // Entity with component was removed
    }
}
```

## CommandBuffer

Deferred operations for thread safety:

```java
public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
                 Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
    Ref<EntityStore> ref = chunk.getRef(index);

    // Queue operations (executed after tick)
    buffer.addComponent(ref, componentType, new MyComponent());
    buffer.removeComponent(ref, otherType);
    buffer.despawn(ref);
}
```

## Common Built-in Components

| Component | Package | Description |
|-----------|---------|-------------|
| `TransformComponent` | `modules.entity.component` | Position and rotation |
| `BoundingBox` | `modules.entity.component` | Collision bounds |
| `UUIDComponent` | `modules.entity.component` | Unique identifier |
| `ModelComponent` | `modules.entity.component` | Visual model |
| `Velocity` | `modules.physics.component` | Movement velocity |
| `PhysicsValues` | `modules.physics.component` | Mass, drag, gravity |

### TransformComponent

```java
TransformComponent transform = store.getComponent(ref, transformType);

// Position
Vector3d pos = transform.getPosition();
double x = pos.x, y = pos.y, z = pos.z;
pos.assign(newX, newY, newZ);

// Rotation (pitch, yaw, roll)
Vector3f rot = transform.getRotation();
float pitch = rot.x, yaw = rot.y, roll = rot.z;
```

## Component Accessor

Alternative way to access components:

```java
// From system context
IComponentAccessor accessor = ...;

TransformComponent transform = accessor.getComponent(ref, transformType);
if (accessor.hasComponent(ref, healthType)) {
    HealthComponent health = accessor.getComponent(ref, healthType);
}
```

## Best Practices

1. **Composition over inheritance**: Use components for data, not class hierarchies
2. **Small components**: Single-purpose components are easier to compose
3. **Systems for behavior**: Keep components data-only, logic in systems
4. **Query efficiently**: Use specific queries to minimize iteration
5. **Use CommandBuffer**: Defer modifications during system ticks
6. **Check validity**: Always check `ref.isValid()` before operations
7. **Thread safety**: Use appropriate thread for store operations

## Documentation Reference
- ECS overview: `/docs/src/content/docs/core-concepts/ecs-overview.mdx`
- Entity overview: `/docs/src/content/docs/api-reference/entities/overview.mdx`
- Component catalog: `/docs/src/content/docs/api-reference/ecs/component-catalog.mdx`
