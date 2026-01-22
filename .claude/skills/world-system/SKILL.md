# Hytale World System

## Overview
The World System manages worlds, chunks, block storage, lighting, and environments. It provides the foundation for all world-related operations.

## Package Locations
- World: `com.hypixel.hytale.server.core.universe.world.World`
- Universe: `com.hypixel.hytale.server.core.universe.Universe`
- Chunk: `com.hypixel.hytale.server.core.universe.world.chunk`
- Lighting: `com.hypixel.hytale.server.core.universe.world.lighting`
- Environment: `Assets/Server/Environments/`

## Universe

The Universe is the top-level container for all worlds:

```java
// Get universe instance
Universe universe = Universe.get();

// Get worlds
World defaultWorld = universe.getDefaultWorld();
Collection<World> allWorlds = universe.getWorlds();
World namedWorld = universe.getWorld("world_name");

// Get players
List<PlayerRef> players = universe.getPlayers();
PlayerRef player = universe.getPlayerByUsername("name", NameMatching.DEFAULT);
PlayerRef playerByUuid = universe.getPlayerByUUID(uuid);
```

## World

```java
World world = Universe.get().getDefaultWorld();

// Basic info
String name = world.getName();
UUID worldId = world.getWorldId();
long seed = world.getSeed();

// World bounds
int minY = world.getMinY();
int maxY = world.getMaxY();
int seaLevel = world.getSeaLevel();

// Time
long worldTime = world.getWorldTime();
float timeOfDay = world.getTimeOfDay();
boolean isDay = world.isDay();
boolean isNight = world.isNight();

// Weather
Weather weather = world.getWeather();
world.setWeather(newWeather);
```

## Block Operations

```java
// Get block type
BlockType type = world.getBlockType(x, y, z);
int blockId = world.getBlock(x, y, z);

// Set block
world.setBlock(x, y, z, blockType);

// With settings
SetBlockSettings settings = new SetBlockSettings();
settings.setNotifyNeighbors(true);
settings.setUpdateLighting(true);
settings.setTriggerBlockUpdate(true);
world.setBlock(x, y, z, blockType, settings);

// Check if position is valid
boolean valid = world.isValidPosition(x, y, z);

// Check if chunk is loaded
boolean loaded = world.isChunkLoaded(chunkX, chunkZ);
```

## Chunks

### Getting Chunks

```java
// Get chunk at block position
WorldChunk chunk = world.getChunk(blockX >> 4, blockZ >> 4);

// Get chunk at chunk coordinates
WorldChunk chunk = world.getChunkAt(chunkX, chunkZ);

// Load chunk
world.loadChunk(chunkX, chunkZ);

// Unload chunk
world.unloadChunk(chunkX, chunkZ);
```

### Chunk Properties

```java
WorldChunk chunk = world.getChunk(chunkX, chunkZ);

// Chunk position
int cx = chunk.getChunkX();
int cz = chunk.getChunkZ();

// Block access within chunk (local coordinates 0-15)
BlockType type = chunk.getBlockType(localX, y, localZ);
chunk.setBlock(localX, y, localZ, blockType);

// Block state
BlockState state = chunk.getState(localX, y, localZ);

// Mark dirty
chunk.markDirty();
chunk.markNeedsSave();
```

### Chunk Constants

```java
// Chunk dimensions
int CHUNK_SIZE = 16;      // X and Z
int CHUNK_HEIGHT = 256;   // Y (may vary by world)
```

## Entity Store

World entities are managed through the EntityStore:

```java
EntityStore entityStore = world.getEntityStore();
Store<EntityStore> store = entityStore.getStore();

// Spawn entity
Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
// ... add components ...
Ref<EntityStore> ref = store.spawn(holder);

// Query entities
// ... use systems and queries ...
```

## Player Operations

```java
// Get players in world
Collection<PlayerRef> worldPlayers = world.getPlayerRefs();

// Teleport player to world
player.teleport(world, x, y, z);

// Get player position
Transform transform = player.getTransform();
Vector3d position = transform.getPosition();
```

## Lighting System

### Light Levels

```java
// Get light level at position
int blockLight = world.getBlockLight(x, y, z);
int skyLight = world.getSkyLight(x, y, z);
int combinedLight = world.getLightLevel(x, y, z);
```

### Dynamic Light

```json
{
  "Id": "MyPlugin_LightBlock",
  "Light": {
    "Color": "#FFAA00",
    "Intensity": 15
  }
}
```

Light intensity: 0-15

### Lighting Updates

```java
SetBlockSettings settings = new SetBlockSettings();
settings.setUpdateLighting(true);
world.setBlock(x, y, z, blockType, settings);
```

## Environment System

### Environment Asset
```json
{
  "Id": "MyPlugin_Environment",
  "Sky": {
    "Color": "#87CEEB",
    "SunColor": "#FFFFA0"
  },
  "Fog": {
    "Color": "#C0C0C0",
    "Density": 0.01,
    "Start": 100,
    "End": 500
  },
  "Ambient": {
    "Color": "#404040"
  }
}
```

### Environment Properties

| Property | Type | Description |
|----------|------|-------------|
| `Sky.Color` | color | Sky background color |
| `Sky.SunColor` | color | Sun color |
| `Fog.Color` | color | Fog color |
| `Fog.Density` | float | Fog density |
| `Fog.Start` | float | Fog start distance |
| `Fog.End` | float | Fog end distance |
| `Ambient.Color` | color | Ambient light color |

## Weather System

### Weather Asset
```json
{
  "Id": "MyPlugin_RainWeather",
  "DisplayName": "Rain",
  "Fog": {
    "Color": "#808080",
    "Density": 0.02
  },
  "Clouds": {
    "Enabled": true,
    "Density": 0.8
  },
  "Precipitation": "Rain",
  "AmbientSound": "Weather_Rain"
}
```

### Weather in Code
```java
// Get current weather
Weather weather = world.getWeather();

// Set weather
Weather rain = Weather.getAssetMap().get("Weather_Rain");
world.setWeather(rain);
```

## World Instances

For dungeons and special areas:

```json
{
  "Id": "MyPlugin_DungeonInstance",
  "Type": "Instanced",
  "Template": "dungeon_template",
  "PlayerLimit": 4,
  "Timeout": 3600
}
```

## Connected Blocks

For blocks that connect visually:

```json
{
  "ConnectedBlockRuleSet": {
    "Rules": [
      {
        "Condition": { "Type": "SameBlock" },
        "Connect": true
      }
    ]
  }
}
```

## Thread Safety

World operations must run on the world's thread:

```java
world.runOnThread(() -> {
    // Safe to modify world here
    world.setBlock(x, y, z, blockType);
});
```

## Scheduled Tasks

```java
// Schedule task on world thread
world.schedule(() -> {
    // Task code
}, delay, TimeUnit.MILLISECONDS);

// Repeating task
world.scheduleAtFixedRate(() -> {
    // Repeating task code
}, initialDelay, period, TimeUnit.MILLISECONDS);
```

## World Events

```java
// Player added to world
getEventRegistry().register(AddPlayerToWorldEvent.class, event -> {
    PlayerRef player = event.getPlayerRef();
    World world = event.getWorld();
});

// Player removed from world
getEventRegistry().register(DrainPlayerFromWorldEvent.class, event -> {
    PlayerRef player = event.getPlayerRef();
});
```

## Prefabs

For placing pre-built structures:

```json
{
  "Id": "MyPlugin_Structure",
  "Prefab": "structures/my_building.prefab",
  "Rotation": "Random",
  "GroundLevel": "Surface"
}
```

## World Configuration

```json
{
  "Name": "MyWorld",
  "Seed": 12345,
  "WorldGenProvider": {
    "Type": "Hytale",
    "Name": "Default"
  },
  "Environment": "MyPlugin_Environment",
  "SpawnPoint": [0, 64, 0]
}
```

## Best Practices

1. **Check chunk loading**: Always check if chunk is loaded before access
2. **Use batch operations**: Group block changes when possible
3. **Thread safety**: Use `runOnThread()` for world modifications
4. **Lighting updates**: Consider performance when updating many lights
5. **Clean up**: Properly unload resources when worlds are removed
6. **Spawn validation**: Validate spawn positions before teleporting

## Documentation Reference
- World overview: `/docs/src/content/docs/api-reference/world/overview.mdx`
- Lighting: `/docs/src/content/docs/api-reference/world/lighting.mdx`
- Dynamic lighting: `/docs/src/content/docs/api-reference/world/dynamic-lighting.mdx`
- Connected blocks: `/docs/src/content/docs/api-reference/world/connected-blocks.mdx`
- Environments: `/docs/src/content/docs/api-reference/assets/world/environments.mdx`
- Weather: `/docs/src/content/docs/api-reference/assets/world/weather.mdx`
- Instances: `/docs/src/content/docs/api-reference/assets/world/instances.mdx`
- Prefabs: `/docs/src/content/docs/api-reference/assets/world/prefabs.mdx`
