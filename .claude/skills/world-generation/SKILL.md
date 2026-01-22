# Hytale World Generation System

## Overview
The World Generation System creates terrain, structures, and biomes using a procedural pipeline with zones, biomes, caves, and prefabs.

## Package Locations
- Providers: `com.hypixel.hytale.server.core.universe.world.worldgen.provider`
- Hytale generator: `com.hypixel.hytale.server.worldgen`
- Generated chunks: `com.hypixel.hytale.server.core.universe.world.worldgen`

## World Gen Providers

### IWorldGenProvider Interface

```java
public interface IWorldGenProvider {
    IWorldGen getGenerator() throws WorldGenLoadException;
}
```

### Built-in Providers

| Provider | Description |
|----------|-------------|
| `HytaleWorldGenProvider` | Full procedural terrain |
| `FlatWorldGenProvider` | Flat terrain |
| `VoidWorldGenProvider` | Empty void |
| `DummyWorldGenProvider` | No generation |
| `HandleProvider` | Custom handle-based |

### HytaleWorldGenProvider

```java
public class HytaleWorldGenProvider implements IWorldGenProvider {
    public static final String ID = "Hytale";

    String name = "Default";  // Generator name
    String path;              // Custom path
}
```

### World Config Usage

```json
{
  "WorldGenProvider": {
    "Type": "Hytale",
    "Name": "MyWorldGen",
    "Path": "path/to/worldgen/config"
  }
}
```

## ChunkGenerator

```java
public class ChunkGenerator implements IBenchmarkableWorldGen, IWorldMapProvider {
    public static final int POOL_SIZE = Math.max(2,
        (int) Math.ceil(Runtime.getRuntime().availableProcessors() * 0.75));

    // Get zone/biome at coordinates
    public ZoneBiomeResult getZoneBiomeResultAt(int seed, int x, int z);

    // Get height
    public int getHeight(int seed, int x, int z);

    // Get cave data
    public Cave getCave(CaveType caveType, int seed, int x, int z);

    // Get spawn points
    public Transform[] getSpawnPoints(int seed);
}
```

## Generation Pipeline

1. **Zone Generation** - Determine zone at position
2. **Biome Generation** - Determine biome within zone
3. **Heightmap** - Generate terrain height
4. **Block Placement** - Place blocks based on layers
5. **Cave Carving** - Carve caves and caverns
6. **Prefab Placement** - Place structures
7. **Population** - Add details (ores, plants)

## Zones

Large-scale regions with distinct generation:

### Zone Record

```java
public record Zone(
    int id,
    String name,
    ZoneDiscoveryConfig discoveryConfig,
    CaveGenerator caveGenerator,
    BiomePatternGenerator biomePatternGenerator,
    UniquePrefabContainer uniquePrefabContainer
) {}
```

### Zone Configuration

```json
{
  "Id": 1,
  "Name": "Plains",
  "DiscoveryConfig": {
    "DiscoverKey": "zone_plains",
    "DiscoverRadius": 100
  },
  "CaveGenerator": "DefaultCaves",
  "BiomePattern": "plains_biomes.png"
}
```

## Biomes

### Biome Class

```java
public abstract class Biome {
    int id;
    String name;
    BiomeInterpolation interpolation;
    IHeightThresholdInterpreter heightmapInterpreter;

    // Containers
    CoverContainer coverContainer;
    LayerContainer layerContainer;
    PrefabContainer prefabContainer;
    TintContainer tintContainer;
    EnvironmentContainer environmentContainer;
    WaterContainer waterContainer;
    FadeContainer fadeContainer;

    // Noise
    NoiseProperty heightmapNoise;

    // Visual
    int mapColor;
}
```

### Biome Containers

| Container | Description |
|-----------|-------------|
| `CoverContainer` | Surface blocks |
| `LayerContainer` | Block layers |
| `PrefabContainer` | Structures |
| `TintContainer` | Block tinting |
| `EnvironmentContainer` | Environment |
| `WaterContainer` | Water settings |
| `FadeContainer` | Transitions |

### Layer Container

```json
{
  "Layers": [
    { "Block": "Grass", "Depth": 1 },
    { "Block": "Dirt", "Depth": 3 },
    { "Block": "Stone", "Depth": -1 }
  ]
}
```

## Cave Generation

### Cave Configuration

```json
{
  "CaveType": "Standard",
  "Frequency": 0.5,
  "MinHeight": 10,
  "MaxHeight": 64,
  "Radius": {
    "Min": 2,
    "Max": 5
  }
}
```

## Prefabs

Pre-built structures:

### PrefabContainer Configuration

```json
{
  "Prefabs": [
    {
      "Id": "Village_House",
      "Chance": 0.1,
      "MinDistance": 100,
      "GroundLevel": "Surface"
    }
  ]
}
```

### Unique Prefabs

One-per-world prefabs:

```java
public class UniquePrefabContainer {
    public static class UniquePrefabEntry {
        boolean isSpawnLocation();
        Vector3i getPosition();
        Vector3d getSpawnOffset();
        Rotation getRotation();
    }
}
```

## Populators

Add details after terrain:

| Populator | Description |
|-----------|-------------|
| `BlockPopulator` | Additional blocks |
| `CavePopulator` | Carve caves |
| `WaterPopulator` | Water features |
| `PrefabPopulator` | Structures |

### Populator Interface

```java
public interface IPopulator {
    void populate(GeneratedChunk chunk, Random random);
}
```

## Generated Chunk

```java
public class GeneratedChunk {
    GeneratedBlockChunk blockChunk;
    GeneratedBlockStateChunk blockStateChunk;
    GeneratedEntityChunk entityChunk;
}
```

## Configuration Files

### World.json

```json
{
  "Zones": "zones/",
  "DefaultZone": "Plains",
  "SeaLevel": 64,
  "WorldHeight": 256,
  "Caves": {
    "Enabled": true,
    "Types": ["Standard", "Cavern"]
  }
}
```

### Directory Structure

```
worldgen/
├── World.json
├── zones/
│   ├── Plains/
│   │   ├── zone.json
│   │   └── biomes/
│   │       ├── Grassland.json
│   │       └── Forest.json
│   └── Mountains/
│       ├── zone.json
│       └── biomes/
│           └── Alpine.json
└── prefabs/
    ├── village/
    └── dungeons/
```

## Custom World Generation

### Implementing Provider

```java
public class MyWorldGenProvider implements IWorldGenProvider {
    public static final BuilderCodec<MyWorldGenProvider> CODEC =
        BuilderCodec.builder(MyWorldGenProvider.class, MyWorldGenProvider::new)
            // Add configuration fields
            .build();

    @Override
    public IWorldGen getGenerator() throws WorldGenLoadException {
        return new MyChunkGenerator();
    }
}
```

### Registering Provider

```java
@Override
protected void setup() {
    getCodecRegistry().register(
        IWorldGenProvider.CODEC,
        "MyPlugin_WorldGen",
        MyWorldGenProvider.CODEC
    );
}
```

## Noise Functions

```java
public class NoiseProperty {
    // Noise configuration
}

public interface IHeightThresholdInterpreter {
    // Interpret height thresholds
}
```

## World Bounds

```java
public interface IWorldBounds {
    int getMinX();
    int getMaxX();
    int getMinZ();
    int getMaxZ();
}

public interface IChunkBounds {
    int getMinChunkX();
    int getMaxChunkX();
    int getMinChunkZ();
    int getMaxChunkZ();
}
```

## Caching

```java
public class ChunkGeneratorCache {
    // Cache zone/biome results
    // Cache heights
    // Cache interpolated values
}

public class CaveGeneratorCache {
    // Cache cave data
}

public class PrefabLoadingCache {
    // Cache loaded prefabs
}
```

## Biome Configuration Example

```json
{
  "Id": "MyPlugin_Forest",
  "Name": "Custom Forest",
  "MapColor": "#228B22",
  "Heightmap": {
    "Noise": {
      "Scale": 100,
      "Octaves": 4,
      "Persistence": 0.5
    },
    "BaseHeight": 64,
    "HeightVariation": 20
  },
  "Layers": [
    { "Block": "Grass", "Depth": 1 },
    { "Block": "Dirt", "Depth": 4 },
    { "Block": "Stone", "Depth": -1 }
  ],
  "Cover": {
    "Trees": {
      "Type": "Oak",
      "Density": 0.3
    },
    "Grass": {
      "Type": "TallGrass",
      "Density": 0.5
    }
  },
  "Prefabs": [
    {
      "Id": "Forest_Cabin",
      "Chance": 0.05
    }
  ]
}
```

## Best Practices

1. **Use caching**: Leverage built-in caches
2. **Async generation**: Use thread pool for heavy operations
3. **Seed consistency**: Ensure deterministic from seed
4. **Validate output**: Use `ValidatableWorldGen`
5. **Profile performance**: Use benchmarking tools
6. **Modular design**: Separate zones, biomes, populators

## Documentation Reference
- World generation overview: `/docs/src/content/docs/api-reference/worldgen/overview.mdx`
- World system: `/docs/src/content/docs/api-reference/world/overview.mdx`
- Block system: `/docs/src/content/docs/api-reference/blocks/overview.mdx`
- Prefabs: `/docs/src/content/docs/api-reference/assets/world/prefabs.mdx`
