# Hexcode Plugin Documentation

## Overview

Hexcode is a spell-crafting plugin for Hytale that allows players to compose complex hexes (spells) by interacting with glyphs in a casting mode. Players enter casting mode while holding a hex staff and hex book, where glyphs float around them and can be combined into spell trees.

## Core Concepts

### Glyph Mode (Casting Mode)

When a player holds a hex staff and hex book, they can enter **Glyph Mode** by holding right-click. In this mode:

- Glyphs from the hex book spawn in a ring around the player
- Glyphs are positioned using **rotation-based positioning**: each glyph is stored as a pitch/yaw angle from the player's eyes, rendered 2 blocks out
- The player can select glyphs by looking at them (5-degree angular tolerance)
- Selected glyphs can be dragged by holding left-click
- Dropping a glyph onto another glyph composes them into a Hex

### Rotation-Based Positioning

Glyphs use a spherical coordinate system relative to the player's eye position:

- **Pitch**: Vertical angle (-90 to +90 degrees). Negative = looking up, positive = looking down
- **Yaw**: Horizontal angle (-180 to +180 degrees). 0 = forward (positive Z), 90 = right
- **Distance**: Fixed at 2.0 blocks from eye position

World position is calculated as:
```
eyePosition + direction(pitch, yaw) * 2.0
```

### Selection and Interaction

- **Selection**: A glyph is selected when the player's look direction is within 5 degrees of the glyph's rotation
- **Dragging**: While holding left-click on a selected glyph, the glyph's rotation follows the player's look direction
- **Drop Detection**: When releasing, if another glyph is within 5 degrees of the dragged glyph's rotation, composition occurs

### Hex Composition

Hexes are tree structures where glyphs are nested:

```
BEAM[POWER[FIRE[], ICE[]]]
     ^     ^      ^
     |     |      └── Siblings (children of POWER)
     |     └── Child of BEAM
     └── Root
```

**Composition Rules:**
- When glyph A is dropped onto glyph B, B becomes the parent and A becomes B's child
- Multiple drops onto the same glyph create siblings
- After composition, the parent's selection tolerance grows from 5 degrees to 7 degrees
- Tier-based selection allows targeting specific depths within a hex

### Angular Margins

- **Base Glyph**: 5-degree selection tolerance
- **Composed Hex (parent)**: 7-degree selection tolerance
- **Inner Tiers**: Progressively smaller tolerances (5, 4, 3.2... degrees)

Precise aiming selects inner tiers; approximate aiming selects outer tiers.

## Glyph System

### GlyphInstance

Each glyph in a player's book is a `GlyphInstance` containing:
- `glyph`: Reference to the registered Glyph type
- `accuracy`: 0.0-1.0 from drawing quality
- `drawSpeed`: Time taken to draw the glyph
- `timesUsed`: Usage counter

### Hex Execution

When a hex is cast, execution traverses the tree depth-first:
- Each glyph modifies a `SpellContext` object
- Single child: Same context flows through (nesting)
- Multiple children: Each gets an isolated copy of context (chaining)
- Glyphs add/modify targets, multipliers, and effects on the context

## Entity System

### GlyphEntity

Represents a single glyph floating in glyph mode:
- Stores rotation (pitch/yaw)
- Spawns as entity with model, dynamic light, and bounding box
- Always faces the player visually

### HexEntity

Represents a composed hex in glyph mode:
- Contains the full hex tree
- Spawns root entity with child entities for each glyph
- Tier-based angular margins for nested selection

### OrbitalElement Interface

Common interface for GlyphEntity and HexEntity:
- Position management (rotation-based)
- State machine: NOT_SPAWNED -> IDLE <-> HOVERING -> DRAGGING -> CONSUMED
- Selection tolerance

## Spawn Styles

Glyphs can be spawned in different patterns:

### RingGlyphStyle
- Distributes glyphs evenly around the player in a horizontal ring
- Ring pitch: ~-9 degrees (slightly below eye level)
- Yaw distribution: Evenly spaced, starting from player's look direction

## Key Classes

| Class | Purpose |
|-------|---------|
| `GlyphRotation` | Value class for pitch/yaw rotation storage |
| `GlyphEntity` | Single glyph entity in glyph mode |
| `HexEntity` | Composed hex entity in glyph mode |
| `GlyphMode` | Manages glyph mode session per player |
| `GlyphModeManager` | Singleton managing all active sessions |
| `CompositionState` | Tracks hex being composed with undo stack |
| `RotationObserver` | Finds glyph at player's look rotation |
| `RingGlyphStyle` | Spawns glyphs in ring formation |
| `Hex` | Tree structure representing a spell |
| `HexNode` | Node in hex tree with value and children |
| `SpellContext` | Execution context passed through hex tree |

## Hex Book System

- Players draw glyphs to add them to their hex book
- Each book has a unique UUID stored in item metadata
- Composed hexes can be saved to the book
- Queued hex: Last selected hex is queued for casting on glyph mode exit

## Casting Flow

1. Player enters glyph mode (hold right-click with staff + book)
2. Glyphs spawn at rotation positions in ring around player
3. Player looks at glyph to hover (visual feedback)
4. Player holds left-click to drag
5. Player releases over another glyph to compose
6. Composed hex appears at drop location
7. Player exits glyph mode (release right-click)
8. Queued hex can be cast with separate action

## Constants

| Constant | Value | Purpose |
|----------|-------|---------|
| Eye Height | 1.62 blocks | Player eye position above feet |
| Glyph Distance | 2.0 blocks | Distance from eye to glyph |
| Base Tolerance | 5.0 degrees | Selection tolerance for single glyphs |
| Composed Tolerance | 7.0 degrees | Selection tolerance for hex parents |
| Tolerance Decay | 0.8x | Inner tier tolerance reduction |
