# Hytale Block System

## Overview
The Block System manages all block types, block states, textures, and block-related operations. Blocks are defined as JSON assets and can have custom behaviors through tick procedures and block states.

## Package Locations
- BlockType: `com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType`
- BlockState: `com.hypixel.hytale.server.core.universe.world.meta.BlockState` (deprecated)
- TickProcedure: `com.hypixel.hytale.server.core.asset.type.blocktick.config.TickProcedure`

## Asset Location
- Server definitions: `Assets/Server/BlockType/`
- Client textures: `Assets/Common/BlockTextures/`
- Client models: `Assets/Common/Blocks/`

## BlockType JSON Schema

### Basic Block
```json
{
  "Id": "MyPlugin_CustomBlock",
  "Group": "MyBlocks",
  "DrawType": "Cube",
  "Material": "Stone",
  "Opacity": "Solid",
  "Textures": [
    {
      "Up": "textures/block_top.png",
      "Down": "textures/block_bottom.png",
      "North": "textures/block_side.png",
      "South": "textures/block_side.png",
      "East": "textures/block_side.png",
      "West": "textures/block_side.png"
    }
  ],
  "HitboxType": "Full",
  "BlockSoundSetId": "Hytale/Stone"
}
```

### Key Properties

| Property | Type | Description |
|----------|------|-------------|
| `Id` | string | Unique block identifier |
| `Group` | string | Grouping for organization |
| `DrawType` | enum | Rendering type (Cube, Model, Empty) |
| `Material` | enum | Physical material type |
| `Opacity` | enum | Light blocking (Solid, Transparent, SemiTransparent) |
| `HitboxType` | string | Collision hitbox type |
| `BlockSoundSetId` | string | Sound set reference |

### Rendering Properties

| Property | Type | Description |
|----------|------|-------------|
| `Textures` | array | Face textures |
| `CustomModel` | string | Path to 3D model |
| `CustomModelTexture` | array | Model textures |
| `CustomModelScale` | float | Model scale (default 1.0) |
| `CustomModelAnimation` | string | Model animation |
| `Light` | object | Emitted light color/intensity |
| `Tint` | Color[] | Color tint per face |
| `BiomeTint` | int | Biome color application |
| `Particles` | array | Block-attached particles |

### Physics Properties

| Property | Type | Description |
|----------|------|-------------|
| `Material` | enum | Stone, Wood, Metal, Glass, etc. |
| `HitboxType` | string | Full, Half, etc. |
| `InteractionHitboxType` | string | Interaction hitbox |
| `MovementSettings` | object | Movement modifiers |

### Support Properties

```json
{
  "Support": {
    "Down": [{ "Type": "Full" }]
  },
  "Supporting": {
    "Up": [{ "Type": "Full" }],
    "North": [{ "Type": "Full" }]
  },
  "SupportDropType": "BREAK"
}
```

SupportDropType values: `BREAK`, `DROP`, `NONE`

### Behavior Properties

| Property | Type | Description |
|----------|------|-------------|
| `Flags.IsUsable` | bool | Can be interacted with |
| `Flags.IsStackable` | bool | Can be stacked on |
| `Interactions` | map | Interaction handlers |
| `Bench` | object | Crafting bench config |
| `DamageToEntities` | int | Contact damage |

## DrawType Enum

| Value | Description |
|-------|-------------|
| `Empty` | Invisible, no rendering |
| `Cube` | Standard cube block |
| `Model` | Custom 3D model |

## BlockMaterial Enum

| Value | Description |
|-------|-------------|
| `Empty` | No physical material |
| `Solid` | Generic solid |
| `Stone` | Stone-like |
| `Wood` | Wood-like |
| `Metal` | Metallic |
| `Glass` | Glass-like |
| `Organic` | Plants, grass |
| `Cloth` | Fabric |
| `Liquid` | Water, lava |

## Accessing Blocks in Code

```java
// Get BlockType by ID
BlockType stone = BlockType.getAssetMap().get("Rock_Stone");
BlockType custom = BlockType.getAssetMap().get("MyPlugin_CustomBlock");

// Get block from world
World world = Universe.get().getDefaultWorld();
BlockType type = world.getBlockType(x, y, z);
int blockId = world.getBlock(x, y, z);

// Special blocks
BlockType.EMPTY  // Air block
BlockType.UNKNOWN  // Unknown placeholder
```

## Setting Blocks

```java
// Simple set
world.setBlock(x, y, z, blockType);

// With settings
SetBlockSettings settings = new SetBlockSettings();
settings.setNotifyNeighbors(true);
settings.setUpdateLighting(true);
settings.setTriggerBlockUpdate(true);
world.setBlock(x, y, z, blockType, settings);

// Batch operations
world.setBlocks(positions, blockTypes, settings);
```

## Block States

Block states store per-block instance data (deprecated API):

```java
// Get state from chunk
WorldChunk chunk = world.getChunk(chunkX, chunkZ);
BlockState state = chunk.getState(localX, y, localZ);

// Ensure state exists
BlockState state = BlockState.ensureState(chunk, x, y, z);

// Cast to specific type
if (state instanceof ChestBlockState chestState) {
    chestState.toggle();
}
```

### Custom Block State

```java
public class ChestBlockState extends BlockState {
    public static final BuilderCodec<ChestBlockState> CODEC =
        BuilderCodec.builder(ChestBlockState.class, ChestBlockState::new)
            .inherit(BlockState.BASE_CODEC)
            .addField(
                new KeyedCodec<>("IsOpen", Codec.BOOLEAN),
                (state, open) -> state.isOpen = open,
                state -> state.isOpen
            )
            .build();

    private boolean isOpen = false;
    private ItemContainer inventory;

    @Override
    public boolean initialize(BlockType blockType) {
        this.inventory = new SimpleItemContainer((short) 27);
        return true;
    }

    public void toggle() {
        this.isOpen = !this.isOpen;
        markNeedsSave();
    }
}
```

### Registering Block States

```java
@Override
protected void setup() {
    getBlockStateRegistry().registerBlockState(
        ChestBlockState.class,
        "MyPlugin_ChestState",
        ChestBlockState.CODEC
    );
}
```

## Tick Procedures

Block tick behavior for growth, decay, etc.:

### JSON Configuration
```json
{
  "Id": "MyPlugin_GrowingPlant",
  "TickProcedure": {
    "Type": "BasicChanceBlockGrowthProcedure",
    "GrowChance": 0.1,
    "GrowToBlock": "MyPlugin_MaturePlant"
  }
}
```

### Built-in Procedures

| Type | Description |
|------|-------------|
| `BasicChanceBlockGrowthProcedure` | Chance-based growth |
| `SplitChanceBlockGrowthProcedure` | Different chances per stage |

### Custom Tick Procedure

```java
public class MyTickProcedure extends TickProcedure {
    @Override
    public BlockTickStrategy onTick(World world, WorldChunk chunk,
            int x, int y, int z, int blockId) {
        // Custom tick logic
        if (shouldGrow()) {
            world.setBlock(x, y, z, grownBlockType);
            return BlockTickStrategy.CHANGED;
        }
        return BlockTickStrategy.NONE;
    }
}
```

BlockTickStrategy values: `NONE`, `CHANGED`, `REMOVED`

## Block Events

```java
// Block break event
getEventRegistry().register(BreakBlockEvent.class, event -> {
    BlockType blockType = event.getBlockType();
    Vector3i position = event.getPosition();
    PlayerRef player = event.getPlayerRef();

    if (blockType.getKey().equals("Protected_Block")) {
        event.setCancelled(true);
        player.sendMessage(Message.raw("Cannot break this block!"));
    }
});

// Block place event
getEventRegistry().register(PlaceBlockEvent.class, event -> {
    BlockType blockType = event.getBlockType();
    Vector3i position = event.getPosition();
});

// Block use event
getEventRegistry().register(UseBlockEvent.class, event -> {
    BlockType blockType = event.getBlockType();
    // Handle interaction
});
```

## Connected Blocks

Blocks that connect to neighbors:

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

## Block Interactions

```json
{
  "Interactions": {
    "Primary": "MyPlugin_PrimaryInteraction",
    "Secondary": "MyPlugin_SecondaryInteraction"
  },
  "InteractionHint": "ui.interaction.open"
}
```

## Rotation

```json
{
  "RandomRotation": "XY",
  "VariantRotation": true,
  "FlipType": "None",
  "RotationYawPlacementOffset": 0
}
```

## Light Emission

```json
{
  "Light": {
    "Color": "#FFAA00",
    "Intensity": 15
  }
}
```

## Best Practices

1. **Use groups**: Organize related blocks with `Group` property
2. **Minimize states**: Only use block states when truly needed
3. **Optimize hitboxes**: Use appropriate hitbox complexity
4. **Consider support**: Define support requirements for physics
5. **Batch updates**: Group block changes when possible
6. **Check loaded**: Verify chunks are loaded before access
7. **Prefix IDs**: Use plugin name prefix for custom blocks

## Documentation Reference
- Block overview: `/docs/src/content/docs/api-reference/blocks/overview.mdx`
- Block types: `/docs/src/content/docs/api-reference/assets/blocks/block-types.mdx`
- Textures: `/docs/src/content/docs/api-reference/assets/blocks/textures.mdx`
- Animations: `/docs/src/content/docs/api-reference/assets/blocks/animations.mdx`
