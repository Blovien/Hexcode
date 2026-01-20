# Hexcode Implementation TODO

This document tracks all remaining work to complete the Hexcode mod implementation.

---

## Phase 1: Compilation Fixes

These must be fixed before any testing can occur.

### Import Corrections

- [x] **HexcodeCommand.java** - Fix `Ref` and `Vector3d` imports
- [x] **MathUtil.java** - Fix `Vector3d` import to `com.hypixel.hytale.math.vector.Vector3d`
- [x] **HexStaffUtil.java** - Fix `Ref` import to `com.hypixel.hytale.component.Ref`
- [x] **SpellBeamEntity.java** - Fix `Ref` and `Vector3d` imports
- [x] **SpellProjectileEntity.java** - Fix `Ref` and `Vector3d` imports
- [x] **ExecutionContext.java** - Fix `Ref` import
- [x] **HexExecutor.java** - Fix `Ref` import
- [x] **TargetSet.java** - Fix `Ref` import

**Pattern to fix:**
```java
// Wrong:
import com.hypixel.hytale.component.store.Ref;
import org.joml.Vector3d;

// Correct:
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
```

---

## Phase 2: Visual System - Orbital Ring

Enable players to see glyphs orbiting around them in Glyph Mode.

### OrbitalGlyphEntity Implementation

- [x] **Implement `OrbitalGlyphEntity.spawn()`** (`OrbitalGlyphEntity.java:170-181`)
  - Create entity in world at calculated orbital position
  - Attach model component from glyph visual
  - Attach dynamic light component
  - Store entity reference for later cleanup

- [x] **Implement `OrbitalGlyphEntity.despawn()`**
  - Remove entity from world
  - Clean up references

- [x] **Implement orbital spawning in GlyphMode.enter()**
  - For each glyph in loadout, spawn OrbitalGlyphEntity
  - Calculate initial orbital positions (evenly distributed)
  - Store spawned entities list

- [x] **Implement orbital cleanup in GlyphMode.exit()**
  - Despawn all orbital glyph entities
  - Clear entity list

### Orbital Animation

- [x] **Implement orbit update tick**
  - Update orbital angle based on time
  - Recalculate positions using `MathUtil.calculateOrbitalPosition()`
  - Move entities to new positions each tick

- [x] **Register tick handler**
  - Add system or event listener to update orbital positions

---

## Phase 3: Interaction System - Hover Detection

Enable players to target glyphs by looking at them.

### Raycast Implementation

- [x] **Create `RaycastUtil.java`** (or complete existing)
  - Implement `findHoveredGlyph(player, orbitalEntities)` method
  - Cast ray from player eye position in look direction
  - Check intersection with each orbital glyph bounding box
  - Return closest hit glyph or null

- [x] **Add hover detection tick**
  - Each tick while in glyph mode, perform raycast
  - Update `GlyphMode.setHoveredGlyph()` with result
  - Call `GlyphRenderer.updateHoverHighlight()` for visual feedback

### Hover Visuals

- [x] **Implement hover highlight in GlyphRenderer**
  - Scale up hovered glyph slightly (pulse effect)
  - Increase light intensity on hovered glyph
  - Add particle effect around hovered glyph

- [x] **Grey out incompatible glyphs**
  - Based on current composition state, determine valid next placements
  - Dim/desaturate glyphs that cannot be placed
  - Update each tick as composition changes

---

## Phase 4: Interaction System - Drag and Drop

Enable players to drag glyphs and drop them in the crafting space.

### Mouse Position Tracking

- [x] **Capture mouse/cursor position in EventHandlers**
  - Extract cursor position from PlayerMouseButtonEvent (or related event)
  - Convert screen position to world ray
  - Calculate 3D drag position along ray at crafting space distance

- [x] **Implement `GlyphMode.updateDrag(position)`**
  - Store current drag position
  - Update dragged glyph entity position to follow cursor

### Drag Visuals

- [x] **Move dragged glyph to follow cursor**
  - Remove from orbital ring visually
  - Position at drag location each frame
  - Add particle trail using `TrailEffect`

- [x] **Implement drag start/end in EventHandlers**
  - On left-click with hovered glyph: start drag
  - On left-click release: end drag, process drop
  - On right-click during drag: cancel drag, return to orbit

### Drop Zone Detection

- [x] **Fix `isInCraftingSpace()` in EventHandlers.java:308**
  - Currently always returns true
  - Should call `CraftingSpace.isInBounds(position)`
  - Return false if outside crafting space bounds

- [x] **Implement drop target detection**
  - Determine what the glyph is being dropped on:
    - Empty crafting space → place as root
    - Existing glyph → wrap that glyph
    - Adjacent to glyph → position for linking
  - Use raycast or proximity check to find drop target

---

## Phase 5: Visual System - Crafting Space

Show the hex being composed in 3D space.

### CraftedGlyphEntity Implementation

- [x] **Implement `CraftedGlyphEntity.spawn()`**
  - Create entity at crafting space position
  - Attach glyph model
  - Attach shell wrapper model if this glyph wraps others
  - Attach dynamic light

- [x] **Implement `CraftedGlyphEntity.despawn()`**
  - Remove entity from world

- [x] **Implement `CraftedGlyphEntity.updatePosition()`**
  - Recalculate position based on hex tree structure
  - Shells surround their children visually

### Crafting Space Layout

- [x] **Implement hex tree to 3D position mapping**
  - Root node at center of crafting space
  - Children positioned inside parent shells
  - Siblings positioned side-by-side
  - Use `CraftingSpace.calculateNodePosition(node)`

- [x] **Spawn crafted entities on composition change**
  - When glyph placed: spawn CraftedGlyphEntity
  - When glyph wrapped: update positions, add shell visual
  - When undo: despawn removed entity, update positions

### Link Visuals

- [x] **Implement sibling link rendering**
  - Draw connection lines/particles between linked siblings
  - Use `LinkRenderer` or particle system
  - Update when siblings added/removed

---

## Phase 6: Composition Flow

Complete the hex building logic.

### Placement Validation

- [x] **Validate modifier compatibility at drop time**
  - When dropping a modifier, check `HexValidator.isCompatible()`
  - If incompatible, reject placement with visual/audio feedback
  - Show error message to player

- [x] **Validate role-based placement rules**
  - EFFECT can only be leaf (no children)
  - MODIFIER must wrap exactly one glyph
  - SELECT can wrap one or linked chain
  - Reject invalid placements

### Sibling Linking

- [x] **Implement link action in EventHandlers**
  - Detect when two glyphs are adjacent and can be linked
  - Add keybind or interaction to link them
  - Call `CompositionState.addSibling()`

- [x] **Visual indicator for linkable glyphs**
  - Highlight adjacent glyphs that can be linked
  - Show potential link line on hover

### Undo System

- [x] **Add undo keybind**
  - Detect undo key press (e.g., 'Z' or middle-click)
  - Call `CompositionState.undo()`
  - Despawn removed crafted glyph entity
  - Update remaining positions

### Discard Hex

- [x] **Add discard action**
  - Clear entire composition
  - Despawn all crafted glyph entities
  - Reset to empty state

---

## Phase 7: Mana System

Fix mana cost calculation and consumption.

### Mana Stat

- [x] **Fix `getPlayerMana()` in EventHandlers.java:352**
  - ~~Currently reads health stat~~
  - Now reads actual mana stat using `DefaultEntityStatTypes.getMana()`

- [x] **Fix `consumePlayerMana()` in EventHandlers.java:358**
  - ~~Currently modifies health~~
  - Now modifies mana stat correctly

### Cost Display

- [x] **Show mana cost during composition**
  - Calculate cost of current hex
  - Display near crafting space or in UI
  - Update as composition changes

- [x] **Show insufficient mana warning**
  - When cost exceeds available mana, show warning
  - Different warning for 75-100% range vs <75%

---

## Phase 8: Hex Execution - Instant

Complete instant spell execution.

### Effect Implementation

- [x] **Implement actual effects in effect glyphs**
  - [x] `FireGlyph.applyEffect()` - deal fire damage, apply burn
  - [x] `IceGlyph.applyEffect()` - deal cold damage, apply slow
  - [x] `LightningGlyph.applyEffect()` - deal shock damage, chain
  - [x] `EarthGlyph.applyEffect()` - deal physical damage, knockback
  - [x] `VoidGlyph.applyEffect()` - deal void damage, blindness
  - [x] `LightGlyph.applyEffect()` - create light source
  - [x] `ShieldGlyph.applyEffect()` - apply absorption buff
  - [x] `BlinkGlyph.applyEffect()` - teleport target
  - [x] `HealGlyph.applyEffect()` - restore health
  - [x] `PushGlyph.applyEffect()` - apply knockback

### Select Implementation

- [x] **Implement target selection in select glyphs**
  - [x] `SelfGlyph.selectTargets()` - return caster
  - [x] `TouchGlyph.selectTargets()` - raycast 3 blocks, return hit
  - [x] `GazeGlyph.selectTargets()` - raycast to max range, return hit
  - [x] `BurstGlyph.selectTargets()` - find entities in radius
  - [x] `ConeGlyph.selectTargets()` - find entities in cone

---

## Phase 9: Hex Execution - Delayed

Complete delayed spell execution (BEAM, PROJECTILE).

### SpellBeamEntity

- [x] **Implement `SpellBeamEntity.spawn()`** (`SpellBeamEntity.java:197`)
  - Create beam entity with visual
  - Set velocity and direction
  - Store pending hex node for execution on hit

- [x] **Implement beam movement tick**
  - Move beam along direction
  - Check for collision with entities/blocks
  - Despawn when max range reached

- [x] **Implement beam hit detection**
  - On collision, execute pending children via `HexExecutor`
  - Set hit entity/position as target
  - Despawn beam

### SpellProjectileEntity

- [x] **Implement `SpellProjectileEntity.spawn()`**
  - Create projectile entity with visual
  - Set velocity with arc/gravity
  - Store pending hex node

- [x] **Implement projectile physics**
  - Apply gravity and movement
  - Check for collision
  - Despawn on hit or timeout

- [x] **Implement projectile hit detection**
  - Execute pending children on hit
  - Apply area effect if configured

### Delayed Execution Queue

- [x] **Complete `DelayedExecutionManager`**
  - Track all pending delayed executions
  - When all delays resolve, continue with remaining siblings
  - Handle timeout for delays that never hit

---

## Phase 10: Audio & Visual Polish

### Sound Effects

- [x] **Add sound for glyph mode enter**
- [x] **Add sound for glyph mode exit**
- [x] **Add sound for glyph hover**
- [x] **Add sound for drag start**
- [x] **Add sound for glyph placement**
- [x] **Add sound for invalid placement**
- [x] **Add sound for linking glyphs**
- [x] **Add sound for undo**
- [x] **Add sound for hex cast**
- [x] **Add sound for cast failure (insufficient mana)**

### Particle Effects

- [x] **Implement drag trail particles** (`TrailEffect`)
- [x] **Implement shell glow particles**
- [x] **Implement cast burst effect**
- [x] **Implement effect-specific particles** (fire, ice, etc.)

### Visual Feedback

- [x] **Add fizzle animation for invalid compositions**
- [x] **Add success animation for valid placement**
- [x] **Add pulse effect on cast**

---

## Phase 11: Stamina Integration

- [x] **Implement stamina drain while in glyph mode**
  - Drain at configured rate per second
  - Check stamina each tick

- [x] **Exit glyph mode when stamina depleted**
  - Force exit when stamina reaches 0
  - Show warning when stamina low

- [x] **Slow movement while in glyph mode**
  - Apply movement speed multiplier from config

---

## Phase 12: Debug & Testing

### Debug Commands

- [x] **Implement `/hexcode debug`** - Toggle debug visualization
- [x] **Implement `/hexcode glyph <id>`** - Spawn glyph in crafting space
- [x] **Implement `/hexcode loadout <glyphs>`** - Set loadout
- [x] **Implement `/hexcode cast`** - Force cast current composition
- [x] **Implement `/hexcode clear`** - Discard current composition
- [x] **Implement `/hexcode mana <amount>`** - Set mana for testing
- [x] **Implement `/hexcode stamina <amount>`** - Set stamina for testing
- [x] **Implement `/hexcode tree`** - Print hex tree to console

### Testing

- [x] **Test single effect hex** (e.g., `SELF[FIRE[]]`)
- [x] **Test modified effect** (e.g., `SELF[POWER[FIRE[]]]`)
- [x] **Test multi-effect hex** (e.g., `SELF[FIRE[], ICE[]]`)
- [x] **Test nested selects** (e.g., `SELF[BEAM[BURST[FIRE[]]]]`)
- [x] **Test delayed execution** (BEAM, PROJECTILE)
- [x] **Test mana cost calculation**
- [x] **Test composition undo**
- [x] **Test invalid composition rejection**
- [x] **Multiplayer synchronization testing**

---

## Phase 13: Multiplayer

- [x] **Sync glyph mode state across clients**
- [x] **Sync orbital glyph positions**
- [x] **Sync crafting space visuals**
- [x] **Sync spell projectiles/beams**
- [x] **Sync effect application**

---

## Summary Progress

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Compilation Fixes | Complete |
| 2 | Orbital Ring Visuals | Complete |
| 3 | Hover Detection | Complete |
| 4 | Drag and Drop | Complete |
| 5 | Crafting Space Visuals | Complete |
| 6 | Composition Flow | Complete |
| 7 | Mana System | Complete |
| 8 | Instant Execution | Complete |
| 9 | Delayed Execution | Complete |
| 10 | Audio & Visual Polish | Complete |
| 11 | Stamina Integration | Complete |
| 12 | Debug & Testing | Complete |
| 13 | Multiplayer | Complete |

**ALL PHASES COMPLETE.**
