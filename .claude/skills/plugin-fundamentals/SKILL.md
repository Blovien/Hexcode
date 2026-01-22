# Hytale Plugin Fundamentals

## Overview
This skill covers the fundamental aspects of creating Hytale server plugins, including project setup, plugin lifecycle, manifest configuration, and registry systems.

## Package Locations
- Plugin base: `com.hypixel.hytale.server.core.plugin.JavaPlugin`
- Manifest: `plugin.json` in plugin JAR root
- Registries: `com.hypixel.hytale.server.core.registry`

## Plugin Base Class

All plugins extend `JavaPlugin`:

```java
package com.example.myplugin;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    @Override
    protected void setup() {
        // Called when plugin is loaded
        getLogger().info("Plugin setup!");
    }

    @Override
    protected void shutdown() {
        // Called when plugin is unloaded
        getLogger().info("Plugin shutdown!");
    }
}
```

## Plugin Manifest (plugin.json)

Required fields:
```json
{
  "Name": "MyPlugin",
  "Version": "1.0.0",
  "MinAPIVersion": "0.0.1",
  "EntryPoint": "com.example.myplugin.MyPlugin"
}
```

Optional fields:
| Field | Type | Description |
|-------|------|-------------|
| `Description` | string | Plugin description |
| `Author` | string | Plugin author |
| `Website` | string | Plugin website URL |
| `Dependencies` | string[] | Required plugin names |
| `SoftDependencies` | string[] | Optional plugin names |
| `IncludesAssetPack` | boolean | Whether plugin includes assets |
| `PermissionBase` | string | Base permission node |
| `CommandBase` | string | Base command path |

## Plugin Lifecycle

```
Server Start
    ↓
Plugin Discovery (scan plugins/)
    ↓
Dependency Resolution
    ↓
Plugin Load (constructor)
    ↓
setup() called ← Register events, commands, systems
    ↓
Server Running
    ↓
shutdown() called ← Cleanup, save data
    ↓
Server Stop
```

### Lifecycle Hooks

```java
@Override
protected void setup() {
    // Register event handlers
    getEventRegistry().register(PlayerConnectEvent.class, event -> {
        getLogger().info("Player connected: " + event.getPlayerRef().getUsername());
    });

    // Register commands
    getCommandRegistry().register(new MyCommand());

    // Access registries
    ComponentType<EntityStore, MyComponent> type =
        getEntityStoreRegistry().registerComponent(MyComponent.class, ...);
}

@Override
protected void shutdown() {
    // Save persistent data
    // Deregister packet adapters
    // Clean up resources
}
```

## Available Registries

Access via `JavaPlugin` methods:

| Method | Registry | Purpose |
|--------|----------|---------|
| `getEventRegistry()` | EventRegistry | Event listeners |
| `getCommandRegistry()` | CommandRegistry | Commands |
| `getEntityStoreRegistry()` | EntityStoreRegistry | Entity components |
| `getChunkStoreRegistry()` | ChunkStoreRegistry | Block/chunk components |
| `getBlockStateRegistry()` | BlockStateRegistry | Block state types |
| `getCodecRegistry()` | CodecRegistry | Custom codecs |
| `getAssetRegistry()` | AssetRegistry | Custom asset types |

## Registry Pattern

Registries use typed keys for compile-time safety:

```java
// Component registration
ComponentType<EntityStore, MyComponent> myComponentType =
    getEntityStoreRegistry().registerComponent(
        MyComponent.class,
        MyComponent::new,
        MyComponent.CODEC
    );

// Event registration
getEventRegistry().register(EventClass.class, handler);
getEventRegistry().register(EventClass.class, priority, handler);

// Command registration
getCommandRegistry().register(commandInstance);
```

## Logger

```java
getLogger().info("Info message");
getLogger().warn("Warning message");
getLogger().error("Error message");
getLogger().debug("Debug message");
```

## Accessing Server Systems

```java
// Get universe (world container)
Universe universe = Universe.get();

// Get worlds
World defaultWorld = universe.getDefaultWorld();
Collection<World> allWorlds = universe.getWorlds();

// Get players
List<PlayerRef> players = universe.getPlayers();
PlayerRef player = universe.getPlayerByUsername("name", NameMatching.DEFAULT);

// Get other plugins
JavaPlugin otherPlugin = getServer().getPluginManager().getPlugin("OtherPlugin");
```

## Asset Packs

When `IncludesAssetPack: true`, include assets in JAR:

```
my-plugin.jar/
├── plugin.json
└── assets/
    ├── blocktype/
    │   └── MyPlugin_CustomBlock.json
    ├── item/
    │   └── MyPlugin_CustomItem.json
    ├── soundevent/
    └── particle/
```

Asset ID format: `{PluginName}_{AssetName}`

## Thread Safety

- Plugin setup/shutdown run on main thread
- Event handlers run on appropriate thread (check event documentation)
- World operations must run on world's thread
- Use `world.runOnThread(() -> { ... })` for thread safety

## Best Practices

1. **Unique prefixes**: Always prefix asset IDs, permission nodes, and commands with plugin name
2. **Clean shutdown**: Deregister adapters and save data in `shutdown()`
3. **Error handling**: Catch exceptions to prevent plugin crashes
4. **Lazy initialization**: Initialize heavy resources when first needed
5. **Configuration**: Use codecs for type-safe config files
6. **Logging**: Use appropriate log levels (debug for development, info for normal operation)

## Documentation Reference
- Setup guide: `/docs/src/content/docs/getting-started/setup.mdx`
- First plugin: `/docs/src/content/docs/getting-started/first-plugin.mdx`
- Plugin lifecycle: `/docs/src/content/docs/getting-started/plugin-lifecycle.mdx`
- Plugin manifest: `/docs/src/content/docs/getting-started/plugin-manifest.mdx`
- Registries: `/docs/src/content/docs/core-concepts/registries.mdx`
