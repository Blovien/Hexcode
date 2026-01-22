# Hexcode - Hexcasting Mod Implementation Plan

## Overview

Hexcode is a spell-crafting mod that allows players to enter **Glyph Mode** while wielding the **Hex Staff** (main hand) and **Hex Book** (offhand). In this mode, glyphs from the player's **loadout** orbit around them in 3D space as floating runes. Players compose spells by dragging glyphs into a central crafting space, building **Hexes** - tree-structured spell constructs where glyphs wrap around each other like shells.

The system uses a **composite/component architecture** where:
- **EFFECT glyphs** are the innermost leaves (actions like FIRE, HEAL)
- **MODIFIER glyphs** wrap around others as inner shells (amplify/alter behavior)
- **SELECT glyphs** wrap around others as outer shells (determine targeting/delivery)

---

## Core Concepts

### The Hex

A **Hex** is a tree-structured spell construct. Glyphs are composed by:
1. **Wrapping** - A glyph surrounds another as a shell (parent-child)
2. **Linking** - Glyphs connect side-by-side as siblings (execute sequentially)

```
Example Hex: BEAM[POWER[FIRE[]], ICE[]]

Tree Structure:
BEAM (SELECT - outer shell)
├── POWER (MODIFIER - inner shell)
│   └── FIRE (EFFECT - leaf)
└── ICE (EFFECT - leaf, sibling to POWER[FIRE[]])

Visual (3D shells):
┌─────────────────────────────────┐
│            BEAM                 │
│  ┌───────────────┐   ┌───────┐  │
│  │    POWER      │───│  ICE  │  │
│  │  ┌─────────┐  │   └───────┘  │
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

**Example Execution:**

```
Hex: SELF[BEAM[POWER[FIRE[]], ICE[]], BURST[HEAL[]]]

1. SELF establishes origin = caster position
2. BEAM (sibling 1, delayed) fires from caster
3.   └── [waits for hit on entity X]
4.   └── On hit: POWER[FIRE[]] executes → powered fire on X
5.   └── On hit: ICE[] executes → ice on X (same target)
6. [All delays resolved]
7. BURST (sibling 2) selects from SELF origin (caster), finds entities Y, Z
8.   └── HEAL[] executes on Y, Z
```

**Nested SELECT Example:**

```
Hex: SELF[BEAM[BURST[FIRE[], ICE[]]]]

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
TotalCost = Σ (effect_base_cost × target_multiplier × modifier_multiplier)

Where:
- Only EFFECT glyphs have base_cost
- target_multiplier = number of targets hit by parent SELECT
- modifier_multiplier = product of all modifiers wrapping the effect
```

**Examples:**
```
BEAM[FIRE[]]
→ FIRE base=15, BEAM hits 1 target, no modifiers
→ Cost = 15 × 1 × 1 = 15

BURST[FIRE[]] (hits 5 entities)
→ FIRE base=15, BURST hits 5 targets
→ Cost = 15 × 5 × 1 = 75

BEAM[POWER[FIRE[]]]
→ FIRE base=15, BEAM hits 1, POWER multiplier=1.5
→ Cost = 15 × 1 × 1.5 = 22.5
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
├── glyph/
│   ├── Glyph.java                  // Base glyph interface
│   ├── GlyphRole.java              // Enum: EFFECT, MODIFIER, SELECT
│   ├── GlyphRegistry.java          // All glyph definitions and lookup
│   ├── GlyphVisual.java            // Visual properties (color, shape, particles)
│   ├── ModifierCompatibility.java  // Compatibility definitions
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
│   ├── Hex.java                    // Complete spell tree structure
│   ├── HexNode.java                // Node in hex tree (glyph + children)
│   ├── HexBuilder.java             // Builds/validates hex during composition
│   ├── HexValidator.java           // Validates compatibility rules
│   └── HexSerializer.java          // Save/load hex structures
├── execution/
│   ├── HexExecutor.java            // Executes a completed hex
│   ├── ExecutionContext.java       // Runtime context (targets, caster, etc.)
│   ├── TargetSet.java              // Set of target entities/blocks/positions
│   ├── DelayedExecution.java       // Handles BEAM/PROJECTILE delays
│   └── EffectApplicator.java       // Applies effects to targets
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
│   ├── CraftedGlyphEntity.java     // Glyph in crafting space
│   ├── SpellProjectileEntity.java  // PROJECTILE delivery
│   └── SpellBeamEntity.java        // BEAM delivery
├── visual/
│   ├── GlyphRenderer.java          // Handles glyph visual updates
│   ├── ShellRenderer.java          // Renders wrapper shells
│   ├── LinkRenderer.java           // Renders sibling connections
│   ├── TrailEffect.java            // Rune glow trail particles
│   └── LightingManager.java        // Dynamic light for glyphs
├── event/
│   ├── GlyphModeEnterEvent.java
│   ├── GlyphModeExitEvent.java
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
 * Base interface for all glyphs
 */
public interface Glyph {
    // Identity
    String getId();                    // "hexcode:fire"
    String getDisplayName();           // "Fire"
    GlyphRole getRole();               // EFFECT, MODIFIER, or SELECT

    // Visual properties
    GlyphVisual getVisual();           // Color, shape, particles, animation

    // For MODIFIER glyphs
    default Set<String> getCompatibleGlyphs() { return Set.of(); }
    default float getModifierMultiplier() { return 1.0f; }

    // For SELECT glyphs
    default boolean isDelayed() { return false; }
    default TargetSet selectTargets(ExecutionContext ctx) { return TargetSet.empty(); }

    // For EFFECT glyphs
    default int getBaseCost() { return 0; }
    default void applyEffect(ExecutionContext ctx, TargetSet targets) {}
}

/**
 * A node in the Hex tree
 */
public class HexNode {
    private final Glyph glyph;
    private final List<HexNode> children;  // Empty for EFFECT, one for MODIFIER, many for SELECT
    private HexNode parent;

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public boolean isLinkedSibling() {
        return parent != null && parent.children.size() > 1;
    }
}

/**
 * Complete Hex structure
 */
public class Hex {
    private HexNode root;

    public void execute(EntityRef caster) {
        ExecutionContext ctx = new ExecutionContext(caster);
        executeNode(root, ctx);
    }

    private void executeNode(HexNode node, ExecutionContext ctx) {
        Glyph glyph = node.getGlyph();

        switch (glyph.getRole()) {
            case SELECT:
                SelectGlyph select = (SelectGlyph) glyph;
                if (select.isDelayed()) {
                    // Queue for delayed execution
                    ctx.queueDelayed(node, select);
                } else {
                    TargetSet targets = select.selectTargets(ctx);
                    ctx.pushTargets(targets);
                    for (HexNode child : node.getChildren()) {
                        executeNode(child, ctx);
                    }
                }
                break;

            case MODIFIER:
                // Modifier affects the child - apply multiplier to context
                ctx.pushModifier(glyph.getId(), glyph.getModifierMultiplier());
                executeNode(node.getChildren().get(0), ctx);
                ctx.popModifier(glyph.getId());
                break;

            case EFFECT:
                EffectGlyph effect = (EffectGlyph) glyph;
                TargetSet targets = ctx.getCurrentTargets();
                effect.applyEffect(ctx, targets);
                break;
        }
    }
}

/**
 * Runtime execution context
 */
public class ExecutionContext {
    private final EntityRef caster;
    private final Deque<TargetSet> targetStack;
    private final Map<String, Float> activeModifiers;
    private final List<DelayedExecution> delayedQueue;

    public TargetSet getCurrentTargets() {
        return targetStack.isEmpty() ? TargetSet.of(caster) : targetStack.peek();
    }

    public float getModifier(String id) {
        return activeModifiers.getOrDefault(id, 1.0f);
    }

    public float getTotalModifierMultiplier() {
        return activeModifiers.values().stream()
            .reduce(1.0f, (a, b) -> a * b);
    }
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
    ExecutionContext context;  // Execution state
    EntityRef caster;
    float speed;
    float maxDistance;
}
```

### Event Flow

```
[Glyph Mode Entry]
    → Check offhand for Hex Staff
    → Create GlyphModeComponent on player
    → Spawn OrbitalGlyphEntity for each loadout glyph
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
// In BeamGlyph
public void onHit(ExecutionContext ctx, EntityRef hitEntity, Vector3f hitPos) {
    ctx.pushTargets(TargetSet.of(hitEntity));

    // Execute all children on hit
    for (HexNode child : this.node.getChildren()) {
        ctx.getExecutor().executeNode(child, ctx);
    }

    // Signal delay resolved
    ctx.resolveDelay(this);
}
```

---

## Implementation Phases

### Phase 1: Foundation
- [x] Create package structure
- [x] Define Glyph interface and GlyphRole enum
- [x] Implement GlyphRegistry with MVP glyph definitions
- [x] Create Hex and HexNode tree structures
- [x] Create Hex Staff item definition (JSON + texture)
- [x] Implement offhand detection for staff

### Phase 2: Glyph Mode Core
- [x] Create GlyphModeManager singleton
- [x] Implement GlyphMode state class per player
- [x] Add event listener for glyph mode toggle
- [x] Implement stamina drain while in mode
- [x] Handle mode exit conditions
- [x] Implement basic loadout (hardcoded for MVP)

### Phase 3: Orbital Ring Display
- [x] Create OrbitalGlyphEntity spawning
- [x] Implement orbital positioning math
- [x] Add dynamic lighting to glyph entities
- [x] Create glyph model assets (or placeholder shapes)
- [x] Implement orbit animation
- [x] Add hover highlight visual

### Phase 4: Hex Composition
- [x] Implement CraftingSpace positioning
- [x] Create HexBuilder for composition state
- [x] Handle drag start/end events
- [x] Implement glyph placement logic
- [x] Add shell wrapper visuals
- [x] Implement sibling linking
- [x] Add undo functionality
- [x] Validate composition rules

### Phase 5: Hex Execution - Instant
- [x] Create ExecutionContext
- [x] Implement tree traversal execution
- [x] Implement TargetSet management
- [x] Create instant SELECT execution (SELF, TOUCH, BURST)
- [x] Apply MODIFIER multipliers to context
- [x] Execute EFFECT glyphs on targets

### Phase 6: Hex Execution - Delayed
- [x] Create SpellProjectileEntity
- [x] Create SpellBeamEntity
- [x] Implement delayed execution queue
- [x] Handle projectile/beam hit detection
- [x] Execute pending children on hit
- [x] Resolve sibling continuation after delays

### Phase 7: Mana & Casting
- [x] Implement mana cost calculation
- [x] Add cost preview during composition
- [x] Validate mana on cast
- [x] Handle partial mana (75%+ rule)
- [x] Consume mana and execute

### Phase 8: Polish & Testing
- [x] Add sound effects for all interactions
- [x] Particle trails for dragging
- [x] Visual feedback for invalid actions
- [x] Debug commands
- [x] Performance optimization
- [x] Multiplayer synchronization testing

---

## Asset Requirements

### Items
| Asset | Path | Description |
|-------|------|-------------|
| Hex Staff | `Server/Item/Items/Hex_Staff.json` | Staff item definition |
| Hex Staff Icon | `Common/Icons/ItemsGenerated/Hex_Staff.png` | Inventory icon |
| Hex Staff Model | `Common/Models/Items/Hex_Staff.json` | 3D model |

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

1. **Glyph Drawing System**: Future implementation where players draw glyphs with mouse gestures, accuracy determines quality percentage
2. **Glyph Unlocking**: Future progression system - discover glyphs in world, learn from scrolls/NPCs
3. **Loadout UI**: Interface for managing which glyphs to bring into combat
4. **Staff Tiers**: Multiple staff types with different mana efficiency or composition limits
5. **PvP Balance**: How do spells interact with other players?
6. **Spell Interruption**: Can other players interrupt casting/composition?
7. **Hex Saving**: Should players be able to save frequently used hex configurations?
8. **Visual Customization**: Player-chosen glyph colors/styles?
9. **Combo Effects**: Should certain glyph combinations create special effects? (Fire + Ice = Steam?)

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

