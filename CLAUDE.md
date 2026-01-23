# Hexcode - Hexcasting Mod Implementation Plan

## Overview

Hexcode is a spell-crafting mod that allows players to enter **Glyph Mode** while wielding the **Hex Staff** (main hand) and **Hex Book** (offhand). In this mode, glyphs from the player's **loadout** orbit around them in 3D space as floating runes. Players compose spells by dragging glyphs into a central crafting space, building **Hexes** - tree-structured spell constructs where glyphs wrap around each other like shells.

The system uses a **modular, asset-driven architecture** where:
- **All glyphs are equal** - Whether defined by this plugin or external plugins, all glyphs use the same registration and execution path
- **Asset-driven configuration** - Glyph properties (visuals, costs, power, variability) are defined in asset files, not code
- **Context-based execution** - Glyphs receive and return a SpellContext object that carries execution state
- **Per-player glyph data** - Drawing accuracy and speed are stored per-player for persistence

The glyph roles are:
- **EFFECT glyphs** are the innermost leaves (actions like FIRE, HEAL)
- **MODIFIER glyphs** wrap around others as inner shells (amplify/alter behavior)
- **SELECT glyphs** wrap around others as outer shells (determine targeting/delivery)

---

## Core Concepts

### Hex vs Chain

The system distinguishes between two composition types:

**HEX (Nested - glyphs influence each other):**
```
Beam[Charge[Fire[]]]
- Beam wraps Charge wraps Fire
- Context flows: Beam → Charge → Fire
- Each glyph can modify the context for its children
```

**CHAIN (Sequential - glyphs execute independently):**
```
Fire[]:Ice[]:Heal[]
- Fire, Ice, Heal execute one after another
- Each receives the ORIGINAL context (not modified by siblings)
- Syntax uses ":" to separate chain elements
```

**COMBINED:**
```
Beam[Charge[Fire[]]]:Beam[Blink[]]
- Two hex chains: first is Beam[Charge[Fire[]]], second is Beam[Blink[]]
- Depth-first: complete first hex entirely, then start second hex
- Second Beam receives original context, NOT the context after Fire executed
```

### The Hex

A **Hex** is a tree-structured spell construct. Glyphs are composed by:
1. **Wrapping** - A glyph surrounds another as a shell (parent-child, context flows through)
2. **Chaining** - Glyphs connect side-by-side as chain siblings (execute sequentially, context isolated)

```
Example Spell: BEAM[POWER[FIRE[]]]:ICE[]

Structure:
Chain[0]: BEAM[POWER[FIRE[]]]  - nested hex
Chain[1]: ICE[]                - second chain element

Tree Structure:
BEAM (SELECT - outer shell)
└── POWER (MODIFIER - inner shell)
    └── FIRE (EFFECT - leaf)
: (chain separator)
ICE (EFFECT - standalone)

Visual (3D shells for nested hex):
┌─────────────────────────────────┐
│            BEAM                 │
│  ┌───────────────┐              │
│  │    POWER      │              │
│  │  ┌─────────┐  │              │
│  │  │  FIRE   │  │              │
│  │  └─────────┘  │              │
│  └───────────────┘              │
└─────────────────────────────────┘
```

### Glyph Roles

| Role | Description | Children | Position |
|------|-------------|----------|----------|
| **EFFECT** | Actions (FIRE, ICE, HEAL) | None - always leaf | Innermost |
| **MODIFIER** | Amplifies wrapped glyph | Exactly one | Inner shell |
| **SELECT** | Targeting/delivery | One or linked chain | Outer shell |

**Key Rules:**
- EFFECTs are always leaves - they cannot contain other glyphs
- MODIFIERs wrap exactly one glyph and only affect that direct child
- SELECTs wrap one glyph OR a linked chain of siblings
- A MODIFIER only modifies its direct child, not grandchildren
  - `POWER[BEAM[ICE[]]]` - POWER modifies BEAM, not ICE
- If no SELECT wraps a Hex, an implicit `SELF[]` is assumed

### Power Decay Rules

1. **Cast Decay**: Each subsequent cast of the same spell is weaker
   - Formula: `effectivePower = basePower * (1.0 / castNumber)`
   - Cast 1: 100%, Cast 2: 50%, Cast 3: 33%, etc.

2. **Glyph Repetition Decay**: If a glyph executes multiple times in one spell, subsequent executions are weaker
   - Formula: `effectivePower = basePower * (1.0 / executionCount)`
   - First Fire[]: 100%, Second Fire[]: 50%, etc.

### SpellContext

The SpellContext object is the central data structure passed between glyphs during execution:

```java
SpellContext {
    // Immutable base info
    UUID casterId;
    EntityRef caster;
    Vector3d castOrigin;
    Vector3d castDirection;
    int castNumber;           // How many times this spell has been cast (1, 2, 3...)

    // Mutable execution state
    List<EntityRef> targets;
    List<Vector3d> targetPositions;
    float powerMultiplier;    // Accumulated power (default 1.0)
    float rangeMultiplier;    // Accumulated range (default 1.0)
    float durationMultiplier; // Accumulated duration (default 1.0)

    // Execution history
    List<GlyphExecutionRecord> executedGlyphs;  // In order of execution
    Map<String, Integer> glyphExecutionCounts;  // How many times each glyph ID executed

    // Extensible metadata
    Map<String, Object> metadata;  // Glyphs can add custom data
}
```

**Key Behavior:**
- Chain elements get COPY of original context (isolated)
- Nested children get SAME context (flows through)
- Depth-first: complete entire hex before moving to next chain element

---

## Core Systems

### 1. Glyph Mode System

**Required Equipment:**
- **Hex Staff** in main hand (hotbar active slot)
- **Hex Book** in offhand (utility slot)
- Both items must be equipped simultaneously to use Hexcode

**Interaction Controls:**
- **Secondary (Right-click):** Toggle glyph mode on/off
- **Primary (Left-click) in glyph mode:** Select and drag glyphs to compose hexes
- **Primary (Left-click) outside glyph mode:** Cast the composed hex

**Entering Glyph Mode:**
- Right-click with Hex Staff in main hand AND Hex Book in offhand
- Glyphs from loadout appear as orbital ring around player

**While Active:**
- Stamina drains continuously (existing stamina system)
- Player can look around freely to see orbiting glyphs
- Normal movement and actions are restricted/modified (slow down the player)
- Glyphs from player's loadout appear as floating entities in orbital ring
- Use left-click to drag glyphs into the crafting space

**Exit Conditions:**
- Right-click again to exit glyph mode (hex is preserved for casting)
- Player removes staff or book
- Stamina depletes completely
- Player takes significant damage (optional interrupt)

**Casting:**
- After composing a hex in glyph mode, right-click to exit
- Left-click to cast the composed hex (consumes mana based on hex cost)
- The hex is cleared after casting

### 2. Glyph Types (MVP)

The focus is a robust modular glyph system. For the POC/MVP, actual balance of each glyph will not be considered - rather the ability to enable balance later through configuration.

Each glyph self-defines its properties including: visual appearance (shape, color, particles, idle animation), compatible modifiers, and behavior.

#### EFFECT Glyphs (Leaf Nodes)

| Glyph | Category | Description | Base Mana Cost |
|-------|----------|-------------|----------------|
| `FIRE` | Element | Deals fire damage, applies burn DOT | 15 |
| `ICE` | Element | Deals cold damage, applies slow | 15 |
| `LIGHTNING` | Element | Deals shock damage, chains to nearby | 15 |
| `EARTH` | Element | Deals physical damage, knockback | 15 |
| `VOID` | Element | Deals void damage, brief blindness | 15 |
| `LIGHT` | Utility | Creates light source at target | 10 |
| `SHIELD` | Utility | Applies damage absorption buff | 20 |
| `BLINK` | Utility | Teleports target short distance | 25 |
| `HEAL` | Utility | Restores health to target | 20 |
| `PUSH` | Utility | Applies knockback without damage | 10 |

#### MODIFIER Glyphs (Inner Shells)

| Glyph | Description | Effect on EFFECT | Effect on SELECT |
|-------|-------------|------------------|------------------|
| `POWER` | Amplify intensity | +50% damage/healing | Glyph-defined |
| `RANGE` | Extend distance | N/A (incompatible) | +50% range/radius |
| `DURATION` | Extend time | +50% DOT/buff duration | +50% travel time |
| `SPEED` | Increase velocity | N/A (incompatible) | +50% projectile speed |
| `SPLIT` | Multi-instance | N/A (incompatible) | Splits into 3 |

**Modifier Compatibility:**
- Each glyph defines which modifiers it accepts
- Incompatible combinations cause the spell to fizzle (fail)
- Examples:
  - `POWER[FIRE[]]` - Valid, fire deals more damage
  - `RANGE[FIRE[]]` - Invalid, FIRE doesn't have range
  - `RANGE[BEAM[...]]` - Valid, beam travels further
  - `RANGE[SELF[...]]` - Invalid, SELF has no range

#### SELECT Glyphs (Outer Shells)

| Glyph | Timing | Description | Target Behavior |
|-------|--------|-------------|-----------------|
| `SELF` | Instant | Self-targeting | Targets caster only |
| `TOUCH` | Instant | Melee range | Targets entity caster is looking at (3 blocks) |
| `GAZE` | Instant | Line of sight | Targets first entity in look direction |
| `BEAM` | Delayed | Raycast projectile | Fires beam, children execute on hit |
| `PROJECTILE` | Delayed | Thrown projectile | Launches projectile, children execute on hit |
| `BURST` | Instant | Area selection | Selects all entities in radius around current target |
| `CONE` | Instant | Cone selection | Selects entities in cone in front of caster |

**Delayed SELECT Behavior:**
- BEAM and PROJECTILE have travel time
- Their children execute when the projectile/beam hits
- All delayed SELECTs must resolve before siblings continue

### 3. Hex Composition Rules

**Building a Hex:**
1. Drag an EFFECT glyph to crafting space - starts the Hex
2. Drag a MODIFIER onto a glyph - wraps it as inner shell
3. Drag a SELECT to wrap glyph(s) - forms outer shell
4. Drag another EFFECT next to existing - positions for linking
5. Link adjacent glyphs together - forms sibling chain
6. SELECT automatically wraps the entire linked chain

**Composition Constraints:**
- EFFECTs cannot contain children
- MODIFIERs must wrap exactly one glyph
- Siblings must be explicitly linked before being wrapped
- No editing after placement - only undo (step by step) or discard entire Hex
- Duplicate glyphs allowed (FIRE[], FIRE[] is valid)
- No hard limit on depth/complexity - mana cost scales

**Wrapping Interaction:**
- When dragging a SELECT, hover highlights what will be wrapped
- Hovering at chain edge selects whole chain
- Hovering on individual glyph selects just that glyph

### 4. Hex Execution Model

**Execution Order:**
1. If no top-level SELECT, implicit `SELF[]` wraps everything
2. Traverse tree depth-first
3. SELECT establishes targets for all its children
4. Siblings share the same targets from their parent SELECT
5. Delayed SELECTs (BEAM, PROJECTILE) pause execution until hit
6. After ALL delayed SELECTs resolve, remaining siblings execute
7. Nested SELECTs use parent's origin, not sibling's result

**Target Context:**
- Each SELECT establishes targets for its children
- Siblings within a SELECT share the same target set
- If no target exists, falls back to most recent target (default: caster)
- Nested SELECTs originate from parent's position, not sibling results

**Example Execution (Nested Hex):**

```
Spell: BEAM[POWER[FIRE[]]]

1. Create base context for caster
2. BEAM.cast(context) - queues delayed execution, spawns beam
3.   └── [beam travels, hits entity X]
4.   └── On hit: context.addTarget(X)
5.   └── POWER.cast(context) - context.multiplyPower(1.5)
6.   └── FIRE.cast(context) - applies fire with power×1.5 to X
```

**Example Execution (Chain - Context Isolation):**

```
Spell: Beam[Charge[Fire[]]]:Beam[Blink[]]

1. Create base context
2. Execute Chain[0] with context COPY:
   - Beam hits Enemy, Charge boosts power, Fire damages Enemy
3. Execute Chain[1] with FRESH context COPY:
   - Beam fires (second beam has decay: 1/2 = 50%)
   - Beam misses, hits air block position
   - Blink teleports caster to that position
```

**Nested SELECT Example:**

```
Spell: SELF[BEAM[BURST[FIRE[], ICE[]]]]

1. SELF establishes origin = caster
2. BEAM fires from caster, hits entity X
3.   └── BURST selects around X (BEAM's hit point), finds Y, Z
4.         ├── FIRE[] executes on Y, Z
5.         └── ICE[] executes on Y, Z (same targets)
```

### 5. Visual System

**Orbital Ring (Loadout Display):**
- Single ring at chest height (~2 blocks radius)
- Shows all glyphs from player's equipped loadout
- Glyphs slowly orbit around player
- Incompatible glyphs appear greyed out based on current composition

**Glyph Appearance:**
- Each glyph is a floating 3D rune/symbol entity
- Each glyph defines its own: shape, color, particles, idle animation
- Suggested color scheme (glyph-overridable):
  - Elements: Match element (Fire=Orange, Ice=Cyan, etc.)
  - Utility: Green
  - Modifiers: Gold
  - Selects: White/Silver

**Selection & Dragging:**
- Crosshair indicates which glyph player is looking at
- Hovered glyph pulses/brightens
- Click initiates drag - glyph follows cursor
- Rune-glow particle trail follows dragged glyph

**Crafting Space:**
- Located ~2 blocks in front of player at eye level
- Shows composed Hex structure
- Shell glyphs visually surround their children
- Linked siblings show connection lines between them

**No Preview:**
- No ghost indicators or trajectory preview
- Players learn spell behavior through practice/mastery

### 6. Loadout System

Players don't have access to all glyphs at once. Like a deckbuilder (without randomness):
- Player pre-selects which glyphs to bring (loadout)
- Only loadout glyphs appear in orbital ring
- Loadout management happens outside of combat (inventory/UI)
- Future: Glyph discovery, unlocking, collection

### 7. Mana Cost System

**Formula:**
```
TotalCost = Σ (glyph.calculateManaCost(context))

Where each glyph's calculateManaCost():
- Reads baseManaCost from asset definition
- Applies context multipliers if relevant
- EFFECTs have base costs, SELECTs/MODIFIERs may have small costs
```

**Examples:**
```
BEAM[FIRE[]]
→ FIRE base=15, BEAM base=0
→ Cost = 15

BURST[FIRE[]] (hits 5 entities)
→ FIRE base=15, BURST base=0
→ Cost = 15 (targets don't increase cost, but do increase power decay)

BEAM[POWER[FIRE[]]]
→ FIRE base=15, POWER base=5, BEAM base=0
→ Cost = 20

Chain: FIRE[]:ICE[]
→ FIRE base=15, ICE base=15
→ Cost = 30
```

**Cast Validation:**
- If mana >= 100% cost: Cast succeeds, consume cost
- If mana >= 75% cost: Cast succeeds (weaker?), consume all mana
- If mana < 75% cost: Cast fails, no mana consumed

---

## Technical Architecture

### Package Structure

```
com.riprod.hexcode/
├── Hexcode.java                    // Main plugin class
├── config/
│   └── HexcodeConfig.java          // Configuration codec
├── asset/
│   ├── GlyphAssetDefinition.java   // Asset-defined glyph properties
│   └── GlyphAssetLoader.java       // Loads glyph assets from JSON files
├── data/
│   ├── PlayerGlyphData.java        // Per-player glyph instance data
│   ├── GlyphInstanceData.java      // Single glyph's accuracy/speed data
│   └── PlayerGlyphDataManager.java // Manages player data persistence
├── drawing/
│   └── DrawingTemplate.java        // PNG template for glyph drawing comparison
├── glyph/
│   ├── Glyph.java                  // Base glyph interface
│   ├── GlyphRole.java              // Enum: EFFECT, MODIFIER, SELECT
│   ├── GlyphRegistry.java          // All glyph definitions and lookup
│   ├── GlyphFactories.java         // Factory methods for built-in glyphs
│   ├── GlyphVisual.java            // Visual properties (color, shape, particles)
│   ├── GlyphShape.java             // Shape definitions
│   ├── effects/
│   │   ├── EffectGlyph.java        // Base class for EFFECT glyphs
│   │   ├── FireGlyph.java
│   │   ├── IceGlyph.java
│   │   ├── LightningGlyph.java
│   │   ├── EarthGlyph.java
│   │   ├── VoidGlyph.java
│   │   ├── LightGlyph.java
│   │   ├── ShieldGlyph.java
│   │   ├── BlinkGlyph.java
│   │   ├── HealGlyph.java
│   │   └── PushGlyph.java
│   ├── modifiers/
│   │   ├── ModifierGlyph.java      // Base class for MODIFIER glyphs
│   │   ├── PowerGlyph.java
│   │   ├── RangeGlyph.java
│   │   ├── DurationGlyph.java
│   │   ├── SpeedGlyph.java
│   │   └── SplitGlyph.java
│   └── selects/
│       ├── SelectGlyph.java        // Base class for SELECT glyphs
│       ├── SelfGlyph.java
│       ├── TouchGlyph.java
│       ├── GazeGlyph.java
│       ├── BeamGlyph.java
│       ├── ProjectileGlyph.java
│       ├── BurstGlyph.java
│       └── ConeGlyph.java
├── hex/
│   ├── Spell.java                  // Top-level spell with chain of hexes
│   ├── HexNode.java                // Node in hex tree (glyph + nested children)
│   ├── HexBuilder.java             // Builds/validates hex during composition
│   ├── HexValidator.java           // Validates compatibility rules
│   └── HexSerializer.java          // Save/load hex structures
├── execution/
│   ├── HexExecutor.java            // Executes a completed spell
│   ├── SpellContext.java           // Runtime context (targets, caster, multipliers)
│   ├── TargetSet.java              // Set of target entities/blocks/positions
│   ├── DelayedExecutionManager.java // Handles BEAM/PROJECTILE delays
│   └── GlyphExecutionRecord.java   // Records single glyph execution
├── mode/
│   ├── GlyphMode.java              // Player glyph mode state
│   ├── GlyphModeManager.java       // Manages active glyph sessions
│   ├── CraftingSpace.java          // 3D hex composition area
│   └── CompositionState.java       // Current composition progress
├── loadout/
│   ├── Loadout.java                // Player's equipped glyphs
│   ├── LoadoutManager.java         // Loadout CRUD operations
│   └── LoadoutStorage.java         // Persistence
├── entity/
│   ├── OrbitalGlyphEntity.java     // Floating glyph in ring
│   ├── OrbitalGlyphComponent.java  // Component for orbital glyph entities
│   ├── CraftedGlyphEntity.java     // Glyph in crafting space
│   ├── SpellProjectileEntity.java  // PROJECTILE delivery
│   └── SpellBeamEntity.java        // BEAM delivery
├── visual/
│   ├── GlyphRenderer.java          // Handles glyph visual updates
│   ├── GlyphParticleRenderer.java  // Particle effects for glyphs
│   ├── ShellRenderer.java          // Renders wrapper shells
│   ├── LinkRenderer.java           // Renders sibling connections
│   ├── TrailEffect.java            // Rune glow trail particles
│   └── LightingManager.java        // Dynamic light for glyphs
├── event/
│   ├── GlyphModeEnterEvent.java
│   ├── GlyphModeExitEvent.java
│   ├── GlyphRegistrationEvent.java // Fired for external plugin glyph registration
│   ├── GlyphDragEvent.java
│   ├── GlyphPlaceEvent.java
│   ├── GlyphLinkEvent.java
│   ├── GlyphWrapEvent.java
│   ├── HexCastEvent.java
│   └── EventHandlers.java
├── command/
│   └── HexcodeCommand.java         // Debug/admin commands
└── util/
    ├── MathUtil.java               // Vector/position calculations
    ├── RaycastUtil.java            // Look-at detection
    └── TreeUtil.java               // Tree traversal helpers
```

### Core Interfaces

```java
/**
 * Base interface for all glyphs (asset-driven)
 */
public interface Glyph {
    // Identity
    String getId();                    // "hexcode:fire"
    String getDisplayName();           // "Fire"
    GlyphRole getRole();               // EFFECT, MODIFIER, or SELECT

    // Asset-driven properties (loaded from asset file)
    GlyphAssetDefinition getAssetDefinition();

    // Registration (called once when glyph is registered)
    RegisterObject onRegister(GlyphRegistry registry);

    // Execution (called each time glyph executes)
    SpellContext cast(SpellContext context);

    // Per-execution data (set by drawing system)
    float getAccuracy();        // 0.0-1.0, set by drawing system
    float getDrawSpeed();       // Seconds taken to draw
    void setExecutionData(float accuracy, float drawSpeed);

    // Mana calculation
    float calculateManaCost(SpellContext context);
}

/**
 * Glyph asset definition (loaded from JSON)
 */
public class GlyphAssetDefinition {
    String id;                    // e.g., "hexcode:fire"
    String displayName;           // e.g., "Fire"
    String role;                  // "EFFECT", "MODIFIER", or "SELECT"

    // Visual assets
    String modelPath;             // Path to Blockbench .blockymodel file
    String drawingTemplatePath;   // Path to PNG for drawing comparison

    // Base stats (all default to 1.0 if not specified)
    float basePower;              // Multiplier for effect strength
    float baseManaCost;           // Mana cost before modifiers
    float baseVariability;        // Drawing tolerance 0.0-1.0

    // Role-specific properties
    Map<String, Object> properties;  // Custom properties per glyph type
}

/**
 * Top-level spell structure with chain of hexes
 */
public class Spell {
    private final List<HexNode> chain;  // Top-level chain of hexes

    public List<HexNode> getChain();
    public boolean isChained();  // Returns true if chain.size() > 1
    public int getChainLength();
}

/**
 * A node in the Hex tree (nested structure)
 */
public class HexNode {
    private final Glyph glyph;
    private final List<HexNode> children;  // Nested children (hex relationship)

    // Factory methods
    public static HexNode leaf(Glyph glyph);
    public static HexNode wrap(Glyph glyph, HexNode child);
    public static HexNode wrap(Glyph glyph, List<HexNode> children);

    // Traversal
    public boolean isLeaf();
    public List<HexNode> getChildren();
    public void forEachDepthFirst(Consumer<HexNode> action);
}

/**
 * Executes a spell with proper context handling
 */
public class HexExecutor {
    public void execute(Spell spell, EntityRef caster, int castNumber) {
        Vector3d origin = getCasterPosition(caster);
        Vector3d direction = getCasterLookDirection(caster);

        // Create base context
        SpellContext baseContext = SpellContext.create(caster, origin, direction, castNumber);

        // Execute each hex in chain with COPY of base context
        for (HexNode hexNode : spell.getChain()) {
            SpellContext chainContext = baseContext.copy();  // Fresh copy for each chain element
            executeHexNode(hexNode, chainContext);
        }
    }

    private SpellContext executeHexNode(HexNode node, SpellContext context) {
        Glyph glyph = node.getGlyph();

        // Execute this glyph (modifies context)
        context = glyph.cast(context);

        // Execute children depth-first (context flows down)
        for (HexNode child : node.getChildren()) {
            context = executeHexNode(child, context);
        }

        return context;
    }
}

/**
 * Runtime spell context (passed between glyphs)
 */
public class SpellContext {
    // Immutable base info
    private final UUID casterId;
    private final EntityRef caster;
    private final Vector3d castOrigin;
    private final Vector3d castDirection;
    private final int castNumber;

    // Mutable execution state
    private List<EntityRef> targets;
    private List<Vector3d> targetPositions;
    private float powerMultiplier = 1.0f;
    private float rangeMultiplier = 1.0f;
    private float durationMultiplier = 1.0f;

    // Execution history
    private List<GlyphExecutionRecord> executedGlyphs;
    private Map<String, Integer> glyphExecutionCounts;

    // Extensible metadata
    private Map<String, Object> metadata;

    // Factory methods
    static SpellContext create(EntityRef caster, Vector3d origin, Vector3d direction, int castNumber);
    SpellContext copy();  // Deep copy for chain isolation

    // Power decay calculation
    float calculateDecayedPower(Glyph glyph);
}
```

### ECS Components

```java
// Component for tracking glyph mode state on players
public class GlyphModeComponent {
    boolean active;
    long modeEnteredAt;
    float staminaDrainRate;
    CompositionState composition;  // Current hex being built
}

// Component for orbital glyph entities
public class OrbitalGlyphComponent {
    String glyphId;
    float orbitAngle;
    float orbitSpeed;
    boolean isHovered;
    boolean isDragging;
    EntityRef ownerPlayer;
}

// Component for crafted glyph entities in composition space
public class CraftedGlyphComponent {
    HexNode node;              // Reference to hex tree node
    Vector3f localPosition;    // Position in crafting space
    boolean isShell;           // Is this a wrapper shell visual?
    List<EntityRef> linkedTo;  // Visual links to siblings
}

// Component for spell projectiles
public class SpellProjectileComponent {
    HexNode pendingNode;       // Subtree to execute on hit
    SpellContext context;      // Execution state (with context copy for chain isolation)
    EntityRef caster;
    float speed;
    float maxDistance;
}
```

### Glyph Registration

The GlyphRegistry is the single source of truth for all glyphs. It supports both built-in and external plugin glyphs:

```java
// During Hexcode initialization
public void onEnable() {
    // 1. Load all glyph asset definitions
    Map<String, GlyphAssetDefinition> assets = GlyphAssetLoader.getInstance().loadAllGlyphAssets();

    // 2. Fire GlyphRegistrationEvent (allows external plugins to register)
    GlyphRegistrationEvent event = new GlyphRegistrationEvent(registry, assetLoader);
    EventSystem.fire(event);

    // 3. Register built-in glyphs using factories + assets
    for (GlyphAssetDefinition asset : assets.values()) {
        GlyphFactory factory = GlyphFactories.getFactory(asset.getId());
        if (factory != null) {
            registry.registerGlyphFromAsset(asset, factory);
        }
    }

    // 4. Freeze registry (no more registrations)
    registry.freeze();

    // 5. Log summary
    logger.info("Registered {} glyphs ({} effects, {} modifiers, {} selects)",
        registry.getAllGlyphs().size(), ...);
}
```

**External plugins register via the event:**
```java
@Subscribe
public void onGlyphRegistration(GlyphRegistrationEvent event) {
    GlyphAssetDefinition asset = event.loadAsset("myplugin:custom_glyph");
    event.registerGlyph(new CustomGlyph(asset));
}
```

### Event Flow

```
[Glyph Mode Entry]
    → Check offhand for Hex Staff
    → Create GlyphModeComponent on player
    → Spawn OrbitalGlyphEntity for each known glyph from player's loadout
    → Initialize empty CompositionState

[PlayerMouseButtonEvent - Drag Start]
    → Raycast to find hovered OrbitalGlyphEntity
    → Set isDragging = true
    → Begin drag visual (glyph follows cursor with trail)

[PlayerMouseButtonEvent - Drop]
    → Determine drop target:
        → Empty space: Place as new root/sibling
        → On existing glyph: Attempt wrap
        → Adjacent to glyph: Position for linking
    → Validate placement (role compatibility)
    → Update HexBuilder state
    → Spawn CraftedGlyphEntity with shell visuals

[Link Action]
    → Check adjacent glyphs can be linked
    → Create sibling relationship in HexNode
    → Update visual connection lines

[Cast Action]
    → Validate Hex is complete
    → Calculate mana cost
    → If sufficient: Execute Hex, consume mana, exit mode
    → If insufficient: Fizzle feedback

[Glyph Mode Exit]
    → Despawn all OrbitalGlyphEntity
    → Despawn all CraftedGlyphEntity
    → Remove GlyphModeComponent
```

### Key API Usage

**Detecting Staff in Offhand:**
```java
Inventory inventory = player.getInventory();
ItemStack offhand = inventory.getUtility().getItemAt(0);
if (offhand != null && isHexStaff(offhand.getItem())) {
    // Staff equipped - can enter glyph mode
}
```

**Spawning Orbital Glyph:**
```java
Vector3f position = calculateOrbitalPosition(player, angle, radius);
EntityRef glyphEntity = entityStore.createEntity();
store.addComponent(glyphEntity, ModelComponent.TYPE, glyph.getVisual().getModel());
store.addComponent(glyphEntity, DynamicLight.TYPE, createLight(glyph.getVisual()));
store.addComponent(glyphEntity, OrbitalGlyphComponent.TYPE, new OrbitalGlyphComponent(glyph, player));
```

**Look-at Detection:**
```java
Vector3f eyePos = player.getEyePosition();
Vector3f lookDir = player.getLookDirection();
for (EntityRef glyph : orbitalGlyphs) {
    BoundingBox bb = store.getComponent(glyph, BoundingBox.TYPE);
    if (rayIntersects(eyePos, lookDir, bb)) {
        return glyph;
    }
}
```

**Delayed Execution (BEAM/PROJECTILE):**
```java
// In BeamGlyph.cast() - queues for delayed execution
@Override
public SpellContext cast(SpellContext context) {
    context.recordGlyphExecution(this);

    // Queue for delayed execution
    UUID executionId = DelayedExecutionManager.getInstance()
        .queueDelayedExecution(getCurrentNode(), context,
                                context.getCastOrigin(),
                                context.getCastDirection());

    // Mark context as having pending delay
    context.setMetadata("pendingDelayId", executionId);

    return context;
}

// In DelayedExecutionManager.onProjectileHit()
public void onHit(UUID executionId, EntityRef hitEntity, Vector3f hitPos) {
    DelayedExecutionState state = pendingExecutions.get(executionId);
    SpellContext context = state.getContext();

    // Add hit entity as target
    context.addTarget(hitEntity);

    // Execute all children on hit
    HexExecutor executor = HexExecutor.getInstance();
    for (HexNode child : state.getNode().getChildren()) {
        executor.executeHexNode(child, context);
    }

    // Remove from pending
    pendingExecutions.remove(executionId);
}
```

---

## Asset System

### Glyph Asset Files

All glyphs are defined through asset files. This enables external plugins to add glyphs using the same system.

**Asset File Location**: `Assets/Server/Hexcode/Glyphs/{glyphId}.json`

**Example Asset File** (`Assets/Server/Hexcode/Glyphs/fire.json`):
```json
{
  "id": "hexcode:fire",
  "displayName": "Fire",
  "role": "EFFECT",
  "modelPath": "Hexcode/Models/Glyphs/fire.blockymodel",
  "drawingTemplatePath": "Hexcode/Drawings/fire.png",
  "basePower": 0.25,
  "baseManaCost": 15,
  "baseVariability": 0.6,
  "properties": {
    "damageType": "fire",
    "baseDamage": 10.0,
    "burnDuration": 3.0,
    "burnEffectId": "hexcode:burn"
  },
  "compatibleModifiers": ["hexcode:power", "hexcode:duration"]
}
```

### Drawing Templates

Drawing templates are black & white PNGs used to compare player-drawn glyphs for accuracy.

**Location**: `Assets/Common/Hexcode/Drawings/{glyphId}.png`

**PNG Format Requirements**:
- Black & white PNG (white = shape, black/transparent = background)
- Recommended size: 128x128 pixels
- Shape should be centered and fill ~80% of canvas

### Per-Player Glyph Data

Player glyph data (accuracy, speed, known glyphs) is stored per-player:
- **Location**: `{universe}/players/{uuid}/hexcode.json`
- Players can only create spells from **known glyphs**
- Accuracy and draw speed affect spell power

---

## Asset Requirements

### Items
| Asset | Path | Description |
|-------|------|-------------|
| Hex Staff | `Server/Item/Items/Hex_Staff.json` | Staff item definition |
| Hex Staff Icon | `Common/Icons/ItemsGenerated/Hex_Staff.png` | Inventory icon |
| Hex Staff Model | `Common/Models/Items/Hex_Staff.json` | 3D model |

### Glyph Assets (per glyph)
| Asset | Path | Description |
|-------|------|-------------|
| Glyph Definition | `Server/Hexcode/Glyphs/{id}.json` | Asset-defined glyph properties |
| Glyph Model | `Common/Hexcode/Models/Glyphs/{id}.blockymodel` | 3D model |
| Drawing Template | `Common/Hexcode/Drawings/{id}.png` | PNG for drawing comparison |

### Glyph Visuals
| Asset | Path | Description |
|-------|------|-------------|
| Glyph Frame Model | `Common/Models/Glyphs/Glyph_Frame.json` | Base floating rune frame |
| Shell Model | `Common/Models/Glyphs/Shell.json` | Wrapper shell visual |
| Effect Textures | `Common/Textures/Glyphs/Effect_*.png` | Per-effect rune textures |
| Modifier Textures | `Common/Textures/Glyphs/Modifier_*.png` | Per-modifier rune textures |
| Select Textures | `Common/Textures/Glyphs/Select_*.png` | Per-select rune textures |

### Effects
| Asset | Path | Description |
|-------|------|-------------|
| Drag Trail | `Common/Particles/Glyph_Drag_Trail.json` | Trail when dragging glyph |
| Link Line | `Common/Particles/Glyph_Link.json` | Connection between siblings |
| Shell Glow | `Common/Particles/Shell_Glow.json` | Wrapper shell ambient |
| Cast Burst | `Common/Particles/Hex_Cast.json` | Cast confirmation effect |

### Localization
| Key | English |
|-----|---------|
| `item.hex_staff.name` | Hex Staff |
| `glyph.effect.fire` | Fire |
| `glyph.effect.ice` | Ice |
| `glyph.modifier.power` | Power |
| `glyph.select.beam` | Beam |
| `hexcode.mode.enter` | Entered Glyph Mode |
| `hexcode.cast.fail.mana` | Not enough mana |
| `hexcode.cast.fail.incompatible` | Incompatible glyph combination |

---

## Configuration

```json
{
  "glyphMode": {
    "staminaDrainPerSecond": 5.0,
    "movementSpeedMultiplier": 0.5,
    "orbitalRadius": 2.5,
    "orbitSpeed": 0.3,
    "craftingSpaceDistance": 2.0
  },
  "composition": {
    "maxHexDepth": 10,
    "maxSiblingsPerSelect": 8,
    "undoStackSize": 20
  },
  "casting": {
    "minManaPercentage": 0.75,
    "baseCooldown": 0.5
  },
  "visuals": {
    "glyphLightRadius": 6,
    "glyphLightIntensity": 10,
    "dragTrailParticles": 15,
    "dragTrailDuration": 0.3
  },
  "execution": {
    "beamSpeed": 50.0,
    "projectileSpeed": 20.0,
    "burstDefaultRadius": 5.0
  }
}
```

---

## Open Questions / Future Considerations

1. **Glyph Drawing System**: Future implementation where players draw glyphs with mouse gestures, accuracy determines quality percentage (per-player data structure ready)
2. **Glyph Unlocking**: Per-player glyph data system supports known glyphs - discovery/learning mechanics TBD
3. **Loadout UI**: Interface for managing which glyphs to bring into combat
4. **Staff Tiers**: Multiple staff types with different mana efficiency or composition limits
5. **PvP Balance**: How do spells interact with other players?
6. **Spell Interruption**: Can other players interrupt casting/composition?
7. **Hex Saving**: Should players be able to save frequently used hex configurations?
8. **Visual Customization**: Player-chosen glyph colors/styles?
9. **Combo Effects**: Should certain glyph combinations create special effects? (Fire + Ice = Steam?)
10. **External Plugin Glyphs**: GlyphRegistrationEvent allows external plugins to register custom glyphs

---

## Reference Documentation

Key documentation files in `/docs/` relevant to this implementation:

- `extracted/entities.md` - Entity creation, components, ECS
- `extracted/effects.md` - Status effects system (for DOTs, buffs)
- `extracted/events.md` - Event system registration
- `extracted/dynamic-lighting.md` - Dynamic light system
- `extracted/inventory.md` - Inventory/equipment access
- `extracted/interaction.md` - Player interaction handling
- `extracted/physics.md` - Projectile physics
- `extracted/networking.md` - Multiplayer sync considerations

