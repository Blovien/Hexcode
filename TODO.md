# Hexcode Implementation TODO

This document tracks all remaining work to complete the Hexcode mod implementation.

---

## Phase 1: Compilation Fixes

These must be fixed before any testing can occur.

### Import Corrections

- [ ] **HexcodeCommand.java** - Fix `Ref` and `Vector3d` imports
- [ ] **MathUtil.java** - Fix `Vector3d` import to `com.hypixel.hytale.math.vector.Vector3d`
- [ ] **HexStaffUtil.java** - Fix `Ref` import to `com.hypixel.hytale.component.Ref`
- [ ] **SpellBeamEntity.java** - Fix `Ref` and `Vector3d` imports
- [ ] **SpellProjectileEntity.java** - Fix `Ref` and `Vector3d` imports
- [ ] **ExecutionContext.java** - Fix `Ref` import
- [ ] **HexExecutor.java** - Fix `Ref` import
- [ ] **TargetSet.java** - Fix `Ref` import

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

- [ ] **Implement `OrbitalGlyphEntity.spawn()`** (`OrbitalGlyphEntity.java:170-181`)
  - Create entity in world at calculated orbital position
  - Attach model component from glyph visual
  - Attach dynamic light component
  - Store entity reference for later cleanup

- [ ] **Implement `OrbitalGlyphEntity.despawn()`**
  - Remove entity from world
  - Clean up references

- [ ] **Implement orbital spawning in GlyphMode.enter()**
  - For each glyph in loadout, spawn OrbitalGlyphEntity
  - Calculate initial orbital positions (evenly distributed)
  - Store spawned entities list

- [ ] **Implement orbital cleanup in GlyphMode.exit()**
  - Despawn all orbital glyph entities
  - Clear entity list

### Orbital Animation

- [ ] **Implement orbit update tick**
  - Update orbital angle based on time
  - Recalculate positions using `MathUtil.calculateOrbitalPosition()`
  - Move entities to new positions each tick

- [ ] **Register tick handler**
  - Add system or event listener to update orbital positions

---

## Phase 3: Interaction System - Hover Detection

Enable players to target glyphs by looking at them.

### Raycast Implementation

- [ ] **Create `RaycastUtil.java`** (or complete existing)
  - Implement `findHoveredGlyph(player, orbitalEntities)` method
  - Cast ray from player eye position in look direction
  - Check intersection with each orbital glyph bounding box
  - Return closest hit glyph or null

- [ ] **Add hover detection tick**
  - Each tick while in glyph mode, perform raycast
  - Update `GlyphMode.setHoveredGlyph()` with result
  - Call `GlyphRenderer.updateHoverHighlight()` for visual feedback

### Hover Visuals

- [ ] **Implement hover highlight in GlyphRenderer**
  - Scale up hovered glyph slightly (pulse effect)
  - Increase light intensity on hovered glyph
  - Add particle effect around hovered glyph

- [ ] **Grey out incompatible glyphs**
  - Based on current composition state, determine valid next placements
  - Dim/desaturate glyphs that cannot be placed
  - Update each tick as composition changes

---

## Phase 4: Interaction System - Drag and Drop

Enable players to drag glyphs and drop them in the crafting space.

### Mouse Position Tracking

- [ ] **Capture mouse/cursor position in EventHandlers**
  - Extract cursor position from PlayerMouseButtonEvent (or related event)
  - Convert screen position to world ray
  - Calculate 3D drag position along ray at crafting space distance

- [ ] **Implement `GlyphMode.updateDrag(position)`**
  - Store current drag position
  - Update dragged glyph entity position to follow cursor

### Drag Visuals

- [ ] **Move dragged glyph to follow cursor**
  - Remove from orbital ring visually
  - Position at drag location each frame
  - Add particle trail using `TrailEffect`

- [ ] **Implement drag start/end in EventHandlers**
  - On left-click with hovered glyph: start drag
  - On left-click release: end drag, process drop
  - On right-click during drag: cancel drag, return to orbit

### Drop Zone Detection

- [ ] **Fix `isInCraftingSpace()` in EventHandlers.java:308**
  - Currently always returns true
  - Should call `CraftingSpace.isInBounds(position)`
  - Return false if outside crafting space bounds

- [ ] **Implement drop target detection**
  - Determine what the glyph is being dropped on:
    - Empty crafting space → place as root
    - Existing glyph → wrap that glyph
    - Adjacent to glyph → position for linking
  - Use raycast or proximity check to find drop target

---

## Phase 5: Visual System - Crafting Space

Show the hex being composed in 3D space.

### CraftedGlyphEntity Implementation

- [ ] **Implement `CraftedGlyphEntity.spawn()`**
  - Create entity at crafting space position
  - Attach glyph model
  - Attach shell wrapper model if this glyph wraps others
  - Attach dynamic light

- [ ] **Implement `CraftedGlyphEntity.despawn()`**
  - Remove entity from world

- [ ] **Implement `CraftedGlyphEntity.updatePosition()`**
  - Recalculate position based on hex tree structure
  - Shells surround their children visually

### Crafting Space Layout

- [ ] **Implement hex tree to 3D position mapping**
  - Root node at center of crafting space
  - Children positioned inside parent shells
  - Siblings positioned side-by-side
  - Use `CraftingSpace.calculateNodePosition(node)`

- [ ] **Spawn crafted entities on composition change**
  - When glyph placed: spawn CraftedGlyphEntity
  - When glyph wrapped: update positions, add shell visual
  - When undo: despawn removed entity, update positions

### Link Visuals

- [ ] **Implement sibling link rendering**
  - Draw connection lines/particles between linked siblings
  - Use `LinkRenderer` or particle system
  - Update when siblings added/removed

---

## Phase 6: Composition Flow

Complete the hex building logic.

### Placement Validation

- [ ] **Validate modifier compatibility at drop time**
  - When dropping a modifier, check `HexValidator.isCompatible()`
  - If incompatible, reject placement with visual/audio feedback
  - Show error message to player

- [ ] **Validate role-based placement rules**
  - EFFECT can only be leaf (no children)
  - MODIFIER must wrap exactly one glyph
  - SELECT can wrap one or linked chain
  - Reject invalid placements

### Sibling Linking

- [ ] **Implement link action in EventHandlers**
  - Detect when two glyphs are adjacent and can be linked
  - Add keybind or interaction to link them
  - Call `CompositionState.addSibling()`

- [ ] **Visual indicator for linkable glyphs**
  - Highlight adjacent glyphs that can be linked
  - Show potential link line on hover

### Undo System

- [ ] **Add undo keybind**
  - Detect undo key press (e.g., 'Z' or middle-click)
  - Call `CompositionState.undo()`
  - Despawn removed crafted glyph entity
  - Update remaining positions

### Discard Hex

- [ ] **Add discard action**
  - Clear entire composition
  - Despawn all crafted glyph entities
  - Reset to empty state

---

## Phase 7: Mana System

Fix mana cost calculation and consumption.

### Mana Stat

- [ ] **Fix `getPlayerMana()` in EventHandlers.java:352**
  - Currently reads health stat
  - Should read actual mana stat (or create one)
  - Check what stat type Hytale provides for mana

- [ ] **Fix `consumePlayerMana()` in EventHandlers.java:358**
  - Currently modifies health
  - Should modify mana stat

### Cost Display

- [ ] **Show mana cost during composition**
  - Calculate cost of current hex
  - Display near crafting space or in UI
  - Update as composition changes

- [ ] **Show insufficient mana warning**
  - When cost exceeds available mana, show warning
  - Different warning for 75-100% range vs <75%

---

## Phase 8: Hex Execution - Instant

Complete instant spell execution.

### Effect Implementation

- [ ] **Implement actual effects in effect glyphs**
  - `FireGlyph.applyEffect()` - deal fire damage, apply burn
  - `IceGlyph.applyEffect()` - deal cold damage, apply slow
  - `LightningGlyph.applyEffect()` - deal shock damage, chain
  - `EarthGlyph.applyEffect()` - deal physical damage, knockback
  - `VoidGlyph.applyEffect()` - deal void damage, blindness
  - `LightGlyph.applyEffect()` - create light source
  - `ShieldGlyph.applyEffect()` - apply absorption buff
  - `BlinkGlyph.applyEffect()` - teleport target
  - `HealGlyph.applyEffect()` - restore health
  - `PushGlyph.applyEffect()` - apply knockback

### Select Implementation

- [ ] **Implement target selection in select glyphs**
  - `SelfGlyph.selectTargets()` - return caster
  - `TouchGlyph.selectTargets()` - raycast 3 blocks, return hit
  - `GazeGlyph.selectTargets()` - raycast to max range, return hit
  - `BurstGlyph.selectTargets()` - find entities in radius
  - `ConeGlyph.selectTargets()` - find entities in cone

---

## Phase 9: Hex Execution - Delayed

Complete delayed spell execution (BEAM, PROJECTILE).

### SpellBeamEntity

- [ ] **Implement `SpellBeamEntity.spawn()`** (`SpellBeamEntity.java:197`)
  - Create beam entity with visual
  - Set velocity and direction
  - Store pending hex node for execution on hit

- [ ] **Implement beam movement tick**
  - Move beam along direction
  - Check for collision with entities/blocks
  - Despawn when max range reached

- [ ] **Implement beam hit detection**
  - On collision, execute pending children via `HexExecutor`
  - Set hit entity/position as target
  - Despawn beam

### SpellProjectileEntity

- [ ] **Implement `SpellProjectileEntity.spawn()`**
  - Create projectile entity with visual
  - Set velocity with arc/gravity
  - Store pending hex node

- [ ] **Implement projectile physics**
  - Apply gravity and movement
  - Check for collision
  - Despawn on hit or timeout

- [ ] **Implement projectile hit detection**
  - Execute pending children on hit
  - Apply area effect if configured

### Delayed Execution Queue

- [ ] **Complete `DelayedExecutionManager`**
  - Track all pending delayed executions
  - When all delays resolve, continue with remaining siblings
  - Handle timeout for delays that never hit

---

## Phase 10: Audio & Visual Polish

### Sound Effects

- [ ] **Add sound for glyph mode enter**
- [ ] **Add sound for glyph mode exit**
- [ ] **Add sound for glyph hover**
- [ ] **Add sound for drag start**
- [ ] **Add sound for glyph placement**
- [ ] **Add sound for invalid placement**
- [ ] **Add sound for linking glyphs**
- [ ] **Add sound for undo**
- [ ] **Add sound for hex cast**
- [ ] **Add sound for cast failure (insufficient mana)**

### Particle Effects

- [ ] **Implement drag trail particles** (`TrailEffect`)
- [ ] **Implement shell glow particles**
- [ ] **Implement cast burst effect**
- [ ] **Implement effect-specific particles** (fire, ice, etc.)

### Visual Feedback

- [ ] **Add fizzle animation for invalid compositions**
- [ ] **Add success animation for valid placement**
- [ ] **Add pulse effect on cast**

---

## Phase 11: Stamina Integration

- [ ] **Implement stamina drain while in glyph mode**
  - Drain at configured rate per second
  - Check stamina each tick

- [ ] **Exit glyph mode when stamina depleted**
  - Force exit when stamina reaches 0
  - Show warning when stamina low

- [ ] **Slow movement while in glyph mode**
  - Apply movement speed multiplier from config

---

## Phase 12: Debug & Testing

### Debug Commands

- [ ] **Implement `/hexcode debug`** - Toggle debug visualization
- [ ] **Implement `/hexcode glyph <id>`** - Spawn glyph in crafting space
- [ ] **Implement `/hexcode loadout <glyphs>`** - Set loadout
- [ ] **Implement `/hexcode cast`** - Force cast current composition
- [ ] **Implement `/hexcode clear`** - Discard current composition
- [ ] **Implement `/hexcode mana <amount>`** - Set mana for testing
- [ ] **Implement `/hexcode stamina <amount>`** - Set stamina for testing
- [ ] **Implement `/hexcode tree`** - Print hex tree to console

### Testing

- [ ] **Test single effect hex** (e.g., `SELF[FIRE[]]`)
- [ ] **Test modified effect** (e.g., `SELF[POWER[FIRE[]]]`)
- [ ] **Test multi-effect hex** (e.g., `SELF[FIRE[], ICE[]]`)
- [ ] **Test nested selects** (e.g., `SELF[BEAM[BURST[FIRE[]]]]`)
- [ ] **Test delayed execution** (BEAM, PROJECTILE)
- [ ] **Test mana cost calculation**
- [ ] **Test composition undo**
- [ ] **Test invalid composition rejection**
- [ ] **Multiplayer synchronization testing**

---

## Phase 13: Multiplayer

- [ ] **Sync glyph mode state across clients**
- [ ] **Sync orbital glyph positions**
- [ ] **Sync crafting space visuals**
- [ ] **Sync spell projectiles/beams**
- [ ] **Sync effect application**

---

## Summary Progress

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Compilation Fixes | Not Started |
| 2 | Orbital Ring Visuals | Not Started |
| 3 | Hover Detection | Not Started |
| 4 | Drag and Drop | Not Started |
| 5 | Crafting Space Visuals | Not Started |
| 6 | Composition Flow | Partial |
| 7 | Mana System | Partial |
| 8 | Instant Execution | Partial |
| 9 | Delayed Execution | Not Started |
| 10 | Audio & Visual Polish | Not Started |
| 11 | Stamina Integration | Not Started |
| 12 | Debug & Testing | Not Started |
| 13 | Multiplayer | Not Started |

**Estimated completion for basic playability: Phases 1-6**
