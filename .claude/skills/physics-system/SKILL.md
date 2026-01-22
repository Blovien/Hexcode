# Hytale Physics System

## Overview
The Physics System handles entity movement, collision detection, and physical simulation using an ECS-based approach with velocity components, collision detection, and configurable hitboxes.

## Package Locations
- Physics: `com.hypixel.hytale.server.core.modules.physics`
- Collision: `com.hypixel.hytale.server.core.modules.collision`
- Hitbox: `com.hypixel.hytale.server.core.modules.entity.hitboxcollision`
- Velocity: `com.hypixel.hytale.server.core.modules.splitvelocity`

## Core Components

### Velocity Component

```java
public class Velocity {
    // Set velocity
    public void set(double x, double y, double z);
    public void set(Vector3d velocity);

    // Add force
    public void addForce(Vector3d force);

    // Get velocity
    public Vector3d getVelocity();
    public Vector3d getClientVelocity();
    public double getSpeed();

    // Instructions for velocity changes
    public void addInstruction(Vector3d velocity, VelocityConfig config,
                               ChangeVelocityType type);
    public List<Instruction> getInstructions();
}
```

### PhysicsValues Component

```java
public class PhysicsValues {
    // Default values
    public static final double DEFAULT_MASS = 1.0;
    public static final double DEFAULT_DRAG_COEFFICIENT = 0.5;
    public static final boolean DEFAULT_INVERTED_GRAVITY = false;

    // Properties
    public double getMass();
    public void setMass(double mass);

    public double getDragCoefficient();
    public void setDragCoefficient(double coefficient);

    public boolean isInvertedGravity();
    public void setInvertedGravity(boolean inverted);
}
```

### BoundingBox Component

```java
public class BoundingBox {
    // Main bounding box
    public Box getBoundingBox();
    public void setBoundingBox(Box box);

    // Detail boxes for complex shapes
    public Map<String, DetailBox[]> getDetailBoxes();
    public void setDetailBoxes(Map<String, DetailBox[]> boxes);
}
```

## Collision Module

```java
public class CollisionModule extends JavaPlugin {
    // Get singleton
    public static CollisionModule get();

    // Find collisions along movement path
    public static CollisionResult findCollisions(
        Box collider,
        Vector3d position,
        Vector3d velocity,
        CollisionResult result,
        IComponentAccessor accessor
    );

    // Validate position against collisions
    public static boolean validatePosition(
        World world,
        Box collider,
        Vector3d position,
        CollisionResult result
    );

    // Block collisions (far distance)
    public static void findBlockCollisionsIterative(...);

    // Block collisions (short distance)
    public static void findBlockCollisionsShortDistance(...);

    // Entity collisions
    public static void findCharacterCollisions(...);

    // Intersection detection
    public static void findIntersections(...);
}
```

## Physics Simulation Flow

1. **Force Accumulation**
   - External forces, accelerations, impulses
   - Gravity based on entity properties
   - Fluid forces (buoyancy, drag)

2. **Velocity Update**
   - Forces integrated to velocity
   - Resistance applied
   - Velocity clamped to limits

3. **Movement Computation**
   - Delta position from velocity
   - Swept collision detection
   - Path adjustment for collisions

4. **Collision Response**
   - Block collisions: bounce, slide, stop
   - Entity collisions: impact callbacks
   - Trigger blocks: events fired
   - Damage blocks: damage applied

5. **State Update**
   - Position updated
   - Velocity updated (post-collision)
   - Physics state set

## Physics Constants

```java
// Fluid densities
public static final double DENSITY_AIR = 1.2;
public static final double DENSITY_WATER = 998.0;

// Material masks
public static final int MASK_EMPTY = 1;
public static final int MASK_FLUID = 2;
public static final int MASK_SOLID = 4;
public static final int MASK_SUBMERGED = 8;
public static final int MASK_DAMAGE = 16;
```

## Integration Methods

| Method | Class | Accuracy | Performance |
|--------|-------|----------|-------------|
| Symplectic Euler | `PhysicsBodyStateUpdaterSymplecticEuler` | Low | Fast |
| Midpoint | `PhysicsBodyStateUpdaterMidpoint` | Medium | Medium |
| Runge-Kutta 4 | `PhysicsBodyStateUpdaterRK4` | High | Slow |

## Physics States

| State | Description |
|-------|-------------|
| `Active` | Actively simulating |
| `Resting` | Stationary on support |
| `Inactive` | Simulation disabled |

## VelocityConfig

```java
public class VelocityConfig {
    float groundResistance = 0.82f;
    float groundResistanceMax;
    float airResistance = 0.96f;
    float airResistanceMax;
    float threshold = 1.0f;
    ThresholdStyle style;  // Linear or non-linear
}
```

## Usage Examples

### Apply Knockback

```java
public void applyKnockback(Ref<EntityStore> entityRef,
                           IComponentAccessor accessor,
                           Vector3d knockback) {
    Velocity velocity = accessor.getComponent(entityRef, Velocity.getComponentType());
    velocity.addForce(knockback);
}
```

### Check Collision

```java
public boolean checkCollision(World world,
                              Ref<EntityStore> entityRef,
                              IComponentAccessor accessor,
                              Vector3d movement) {
    BoundingBox bbox = accessor.getComponent(entityRef, BoundingBox.getComponentType());
    Box collider = bbox.getBoundingBox();

    TransformComponent transform = accessor.getComponent(entityRef,
        TransformComponent.getComponentType());
    Vector3d position = transform.getPosition();

    CollisionResult result = new CollisionResult();
    CollisionModule.findCollisions(collider, position, movement, result, accessor);

    return result.hasBlockCollisions();
}
```

### Modify Physics Values

```java
public void makeFloaty(Ref<EntityStore> entityRef, IComponentAccessor accessor) {
    PhysicsValues physics = accessor.getComponent(entityRef,
        PhysicsValues.getComponentType());

    physics.setMass(0.1);
    physics.setDragCoefficient(0.8);
    physics.setInvertedGravity(false);
}
```

## Hitbox Configuration

### Block Hitbox
```json
{
  "Id": "MyPlugin_CustomBlock",
  "HitboxType": "Full"
}
```

Hitbox types: `Full`, `Half`, `Slab`, `Stairs`, `Custom`

### Custom Hitbox
```json
{
  "HitboxType": "Custom",
  "CustomHitbox": {
    "Boxes": [
      {
        "Min": [0, 0, 0],
        "Max": [1, 0.5, 1]
      },
      {
        "Min": [0.25, 0.5, 0.25],
        "Max": [0.75, 1, 0.75]
      }
    ]
  }
}
```

### Entity Hitbox

```json
{
  "BoundingBox": {
    "Width": 0.6,
    "Height": 1.8
  },
  "DetailBoxes": {
    "head": {
      "Min": [-0.2, 1.5, -0.2],
      "Max": [0.2, 1.8, 0.2]
    }
  }
}
```

## Movement System

### Ground Detection

```java
// Check if entity is on ground
MovementStates states = accessor.getComponent(ref, MovementStates.getComponentType());
boolean onGround = states.isOnGround();
```

### Movement Properties

| Property | Description |
|----------|-------------|
| `walkSpeed` | Base walk speed |
| `runSpeed` | Running speed |
| `jumpForce` | Jump velocity |
| `swimSpeed` | Swimming speed |
| `climbSpeed` | Climbing speed |

## Projectile Physics

```json
{
  "Id": "MyPlugin_Arrow",
  "Physics": {
    "Gravity": 0.05,
    "Drag": 0.01,
    "InitialSpeed": 3.0
  },
  "Collision": {
    "OnHitBlock": "stick",
    "OnHitEntity": "damage"
  }
}
```

## Best Practices

1. **Check validity**: Always verify ref is valid before physics operations
2. **Thread safety**: Run physics operations on correct thread
3. **Batch updates**: Group physics changes when possible
4. **Reasonable values**: Use sensible mass and drag values
5. **Test edge cases**: Test at high speeds and complex geometry
6. **Profile performance**: Monitor physics system performance

## Documentation Reference
- Physics overview: `/docs/src/content/docs/api-reference/physics/overview.mdx`
- Collision: `/docs/src/content/docs/api-reference/physics/collision.mdx`
- Hitboxes: `/docs/src/content/docs/api-reference/physics/hitboxes.mdx`
- Movement: `/docs/src/content/docs/api-reference/physics/movement.mdx`
