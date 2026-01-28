# Hexcode Glyph Mode Refactor: Rotation-Based Positioning System

## Overview

Migrate the glyph/casting mode from Cartesian offset-based positioning to rotation-based positioning. Glyphs will be stored as pitch/yaw angles from the player's eyes and rendered 2 blocks out along that direction.

**Key Changes:**
- Position storage: `Vector3d playerOffset` -> `GlyphRotation (pitch, yaw)`
- Selection: Ray-sphere intersection -> Angular distance comparison (5-degree tolerance)
- Drop detection: Ray-sphere hit -> Angular distance to closest glyph
- Composition: Drop target becomes parent; parent grows from 5deg to 7deg margin
- No pitch limits: Full spherical positioning allowed

---

## Phase 1: Core Data Model

### 1.1 Create GlyphRotation Class
**File:** `src/main/java/com/riprod/hexcode/math/GlyphRotation.java` (NEW)

- [ ] Create immutable value class storing `pitchRadians` and `yawRadians`
- [ ] Add constants:
  - `DEFAULT_DISTANCE = 2.0` blocks
  - `DEFAULT_SELECTION_TOLERANCE = 5.0` degrees
  - `COMPOSED_TOLERANCE = 7.0` degrees
- [ ] Implement constructors:
  - `GlyphRotation(double pitchRadians, double yawRadians)`
  - `static fromDegrees(double pitch, double yaw)`
  - `static fromDirection(Vector3d direction)`
- [ ] Implement direction conversion:
  ```java
  public Vector3d toDirection() {
      double cosPitch = Math.cos(pitchRadians);
      double x = -Math.sin(yawRadians) * cosPitch;
      double y = -Math.sin(pitchRadians);
      double z = Math.cos(yawRadians) * cosPitch;
      return new Vector3d(x, y, z);
  }
  ```
- [ ] Implement world position calculation:
  ```java
  public Vector3d toWorldPosition(Vector3d eyePosition, double distance)
  public Vector3d toWorldPosition(Vector3d eyePosition) // Uses DEFAULT_DISTANCE
  ```
- [ ] Implement angular distance (great-circle):
  ```java
  public double angularDistanceTo(GlyphRotation other) {
      Vector3d a = this.toDirection();
      Vector3d b = other.toDirection();
      double dot = Math.max(-1.0, Math.min(1.0, a.dot(b)));
      return Math.acos(dot);  // radians
  }
  ```
- [ ] Implement tolerance check:
  ```java
  public boolean isWithinTolerance(GlyphRotation other, double toleranceDegrees)
  ```
- [ ] Implement lerp for smooth transitions

### 1.2 Create RotationMath Utility Class
**File:** `src/main/java/com/riprod/hexcode/util/RotationMath.java` (NEW)

- [ ] `EYE_HEIGHT = 1.62` constant
- [ ] `getEyePosition(Vector3d feetPosition)` - adds eye height
- [ ] `getEyePosition(TransformComponent transform)` - extracts position and adds eye height
- [ ] `getLookRotation(HeadRotation headRotation)` - converts look direction to GlyphRotation
- [ ] `getPlayerLookRotation(Store<EntityStore>, Ref<EntityStore>)` - convenience method
- [ ] `angularDistanceDegrees(GlyphRotation a, GlyphRotation b)` - returns degrees

### 1.3 Update OrbitalElement Interface
**File:** `src/main/java/com/riprod/hexcode/casting/styles/OrbitalElement.java`

- [ ] Add new methods (with default implementations for migration):
  ```java
  GlyphRotation getGlyphRotation();
  void setGlyphRotation(GlyphRotation rotation);
  void updatePositionFromRotation(Store<EntityStore> store, Vector3d eyePosition);
  void captureRotationFromLook(double lookPitch, double lookYaw);
  default double getSelectionToleranceDegrees() { return 5.0; }
  default boolean isLookedAt(GlyphRotation lookRotation) { ... }
  ```
- [ ] Mark deprecated (DO NOT REMOVE YET):
  ```java
  @Deprecated Vector3d getPlayerOffset();
  @Deprecated void setPlayerOffset(Vector3d offset);
  @Deprecated void updatePositionFromPlayer(Store<EntityStore>, Vector3d);
  @Deprecated void captureOffsetFromPlayer(Store<EntityStore>, Vector3d);
  ```

### 1.4 Update GlyphEntity
**File:** `src/main/java/com/riprod/hexcode/entity/GlyphEntity.java`

- [ ] Replace `private Vector3d playerOffset` with `private GlyphRotation glyphRotation`
- [ ] Update constructor to accept `GlyphRotation initialRotation`
- [ ] Add deprecated constructor wrapper for migration
- [ ] Implement `getGlyphRotation()` and `setGlyphRotation()`
- [ ] Implement `updatePositionFromRotation()`:
  - Skip if dragging
  - Calculate world position from rotation
  - Update TransformComponent
  - Call `updateFacingTowardsPlayer()`
- [ ] Implement `captureRotationFromLook()`
- [ ] Update `spawn()` method to use rotation

### 1.5 Update HexEntity
**File:** `src/main/java/com/riprod/hexcode/entity/HexEntity.java`

- [ ] Replace `private Vector3d playerOffset` with `private GlyphRotation glyphRotation`
- [ ] Replace `private float[] tierRadii` with `private float[] tierAngularMargins`
- [ ] Implement rotation methods (same as GlyphEntity)
- [ ] Implement `getSelectionToleranceDegrees()`:
  ```java
  return BASE_TOLERANCE + (tierCount - 1) * TOLERANCE_INCREMENT;
  // 5 + (tiers-1)*2 degrees
  ```
- [ ] Implement `findClosestTier(GlyphRotation lookRotation)`:
  - Calculate angular distance to hex rotation
  - Return tier index based on distance bands
- [ ] Update `calculateTierAngularMargins()`:
  - Tier 0 (root with children): 7 degrees
  - Inner tiers: 5, 4, 3.2... (decay factor 0.8)
- [ ] Remove `calculateTierRadii()` after migration complete

### 1.6 Update GlyphComponent (ECS)
**File:** `src/main/java/com/riprod/hexcode/entity/GlyphComponent.java`

- [ ] Replace offset fields:
  ```java
  // OLD: offsetX, offsetY, offsetZ
  // NEW:
  private double pitchRadians;
  private double yawRadians;
  ```
- [ ] Update CODEC for new field names
- [ ] Add helper methods:
  ```java
  public GlyphRotation getRotation()
  public void setRotation(GlyphRotation rotation)
  ```

---

## Phase 2: Selection and Drag System

### 2.1 Create RotationObserver (Replaces IntersectionObserver)
**File:** `src/main/java/com/riprod/hexcode/casting/RotationObserver.java` (NEW)

- [ ] Create `RotationTarget` result class:
  ```java
  public final OrbitalElement target;
  public final double angularDistanceDegrees;
  ```
- [ ] Create `HexRotationTarget extends RotationTarget`:
  ```java
  public final int tierLevel;
  public final HexNode targetNode;
  ```
- [ ] Implement `findTargetByRotation(Store, Ref, List<OrbitalElement>)`:
  - Get player look rotation from HeadRotation component
  - Iterate elements, calculate angular distance
  - Return closest within tolerance (element.getSelectionToleranceDegrees())
  - Skip elements that are dragging
- [ ] Implement hex tier detection:
  - If target is HexEntity, call `findClosestTier()`
  - Return `HexRotationTarget` with tier info

### 2.2 Update Drag Mechanics in GlyphMode
**File:** `src/main/java/com/riprod/hexcode/mode/GlyphMode.java`

- [ ] Remove `private Vector3d dragPosition`
- [ ] Add tracking for drag rotation (or use element's rotation directly)
- [ ] Update `startDragElement()`:
  - Element's rotation will be updated each tick to follow look
- [ ] Update `updateDrag()`:
  - Set element's rotation to player's current look rotation
  - Call `updatePositionFromRotation()` for visual update
- [ ] Update `endDrag()`:
  - Call `captureRotationFromLook()` on element
  - Transition state back to IDLE

### 2.3 Update HexcodeGlyphModeToggle
**File:** `src/main/java/com/riprod/hexcode/interaction/HexcodeGlyphModeToggle.java`

- [ ] Replace `IntersectionObserver` usage with `RotationObserver`
- [ ] Update `processServerSideHover()`:
  - Use `rotationObserver.findTargetByRotation()`
  - Update hover state based on angular proximity
- [ ] Update position tick to use `updatePositionFromRotation()` with eye position

### 2.4 Update HexcodeGlyphModeSelect
**File:** `src/main/java/com/riprod/hexcode/interaction/HexcodeGlyphModeSelect.java`

- [ ] Update `handleDragStart()`:
  - Find target using RotationObserver instead of raycasting
- [ ] Update `updateDragPosition()`:
  - Element rotation follows player look direction
  - Visual position calculated from rotation
- [ ] Update `handleDragEnd()`:
  - Find drop target using angular distance comparison
  - Use `findClosestByRotation()` among all non-dragging elements
  - 5-degree tolerance for composition trigger

---

## Phase 3: Composition System

### 3.1 Update HexNode for Angular Margins
**File:** `src/main/java/com/riprod/hexcode/hex/HexNode.java`

- [ ] Add constants:
  ```java
  public static final float BASE_ANGULAR_MARGIN = 5.0f;
  public static final float COMPOSED_ANGULAR_MARGIN = 7.0f;
  ```
- [ ] Add fields:
  ```java
  private float angularMargin = BASE_ANGULAR_MARGIN;
  private boolean hasBeenComposed = false;
  ```
- [ ] Add methods:
  ```java
  public float getAngularMargin()
  public void setHasBeenComposed(boolean composed)
  public boolean hasBeenComposed()
  ```
- [ ] Update serialization (if applicable) for new fields

### 3.2 Update CompositionState
**File:** `src/main/java/com/riprod/hexcode/mode/CompositionState.java`

- [ ] Remove/deprecate `wrapNode()` operation (old system)
- [ ] Update `addAsChild()` (formerly addSibling concept):
  - Dragged glyph becomes child of target node
  - Set `target.setHasBeenComposed(true)` after adding child
- [ ] Update `CompositionAction` to store:
  ```java
  final float previousAngularMargin;
  final boolean previousComposedState;
  ```
- [ ] Update `undo()`:
  - Restore angular margin state when removing children
  - If target has no children after undo, restore to BASE_ANGULAR_MARGIN

### 3.3 Update Drop Handling Logic
**File:** `src/main/java/com/riprod/hexcode/interaction/HexcodeGlyphModeSelect.java`

- [ ] Add self-drop guard:
  ```java
  if (dropTargetElement == draggedElement) {
      // Return element to original position, abort composition
      return;
  }
  ```
- [ ] Update "drop glyph on glyph" case:
  - Target becomes parent, dragged becomes child
  - Create HexEntity with target as root, dragged as first child
  - Mark root as composed (7-degree margin)
- [ ] Update "drop glyph on hex tier" case:
  - Get target node at tier
  - Add dragged as child of target node (creates sibling if tier already has children)
  - Mark target node as composed
- [ ] Add "drop hex on hex" case:
  - Deep copy dragged hex's tree
  - Add as child to target tier node
  - Despawn dragged hex entity
  - Refresh target hex visuals

### 3.4 Update Hex Visual Traversal
**File:** `src/main/java/com/riprod/hexcode/hex/Hex.java`

- [ ] Add `getGlyphStylesWithRotation(float parentRotation)`:
  - Pass rotation context down through traversal
  - Children arranged in circle relative to parent's rotation
- [ ] Update `traverse()` to use `angularMargin` from nodes for sizing hints

---

## Phase 4: Style System

### 4.1 Update BaseGlyphStyle
**File:** `src/main/java/com/riprod/hexcode/casting/styles/BaseGlyphStyle.java`

- [ ] Add new abstract method:
  ```java
  public abstract GlyphRotation getSpawnRotation(int index, int total, GlyphRotation playerLookRotation);
  ```
- [ ] Add deprecated wrapper for old method (migration):
  ```java
  @Deprecated
  public Vector3d getSpawnPosition(int index, int total, Vector3d playerPosition) {
      GlyphRotation rot = getSpawnRotation(index, total, new GlyphRotation(0, 0));
      Vector3d eyePos = new Vector3d(playerPosition.x, playerPosition.y + 1.62, playerPosition.z);
      return rot.toWorldPosition(eyePos);
  }
  ```

### 4.2 Update RingGlyphStyle
**File:** `src/main/java/com/riprod/hexcode/casting/styles/RingGlyphStyle.java`

- [ ] Implement `getSpawnRotation()`:
  ```java
  public GlyphRotation getSpawnRotation(int index, int total, GlyphRotation playerLookRotation) {
      if (total == 0) return playerLookRotation;

      // Ring at slight downward pitch (-9 degrees = chest level)
      double ringPitch = Math.toRadians(-9.0);

      // Distribute yaw evenly, starting from player's look direction
      double baseYaw = playerLookRotation.getYawRadians();
      double yawStep = (2.0 * Math.PI) / total;
      double elementYaw = normalizeAngle(baseYaw + (yawStep * index));

      return new GlyphRotation(ringPitch, elementYaw);
  }
  ```

### 4.3 Update GlyphMode Spawning
**File:** `src/main/java/com/riprod/hexcode/mode/GlyphMode.java`

- [ ] Update `spawnOrbitalGlyphs()`:
  - Get player eye position and look rotation
  - Call `orbitalStyle.getSpawnRotation()` for each glyph
  - Create GlyphEntity/HexEntity with rotation
  - Calculate world position from rotation for spawn
- [ ] Update `updateOrbitalGlyphs()`:
  - Get eye position from TransformComponent
  - Call `element.updatePositionFromRotation(store, eyePosition)` for each element

---

## Phase 5: Cleanup and Deprecation Removal

### 5.1 Remove Deprecated Code
- [ ] Remove `playerOffset` field from GlyphEntity
- [ ] Remove `playerOffset` field from HexEntity
- [ ] Remove offset fields from GlyphComponent
- [ ] Remove deprecated methods from OrbitalElement interface
- [ ] Remove `IntersectionObserver` class (replaced by RotationObserver)
- [ ] Remove/deprecate raycast methods in `RaycastUtil` (keep if used elsewhere)
- [ ] Remove deprecated constructor wrappers

### 5.2 Update Serialization
- [ ] Ensure GlyphComponent codec uses new pitch/yaw fields
- [ ] Update any persistent storage to use rotation format
- [ ] Add migration path if existing data needs conversion

### 5.3 Remove Role References (Already Done in Execution)
- [ ] Audit code for any remaining EFFECT/MODIFIER/SELECT enforcement
- [ ] Remove role-based composition restrictions
- [ ] Update any comments referencing old role system

---

## Phase 6: Testing and Verification

### 6.1 Unit Tests
- [ ] GlyphRotation:
  - Direction conversion (forward, up, down, cardinal directions)
  - Angular distance calculation (same direction = 0, opposite = 180)
  - World position calculation at various angles
  - Tolerance checking edge cases
- [ ] RotationObserver:
  - Single element selection
  - Multiple elements, closest wins
  - Hex tier detection
  - Edge case: no elements in tolerance

### 6.2 Integration Tests
- [ ] Spawn glyphs, verify ring distribution
- [ ] Select glyph by looking, verify correct element
- [ ] Drag glyph, verify rotation follows look
- [ ] Drop glyph on glyph, verify composition (parent-child)
- [ ] Drop on hex tier, verify tier detection
- [ ] Undo composition, verify margin restoration
- [ ] Exit casting mode, verify hex queued

### 6.3 Edge Cases to Verify
- [ ] Looking straight up (pitch = -90): glyph positions correctly
- [ ] Looking straight down (pitch = +90): glyph positions correctly
- [ ] Multiple glyphs at same rotation: closest wins (or first in list)
- [ ] Self-drop: properly rejected
- [ ] Hex-on-hex composition: trees merge correctly
- [ ] Empty hex book: no crash on spawn

---

## File Summary

### New Files
| File | Purpose |
|------|---------|
| `math/GlyphRotation.java` | Core rotation value class with conversions |
| `util/RotationMath.java` | Utility functions for rotation operations |
| `casting/RotationObserver.java` | Rotation-based selection (replaces IntersectionObserver) |

### Modified Files
| File | Changes |
|------|---------|
| `casting/styles/OrbitalElement.java` | Add rotation interface methods |
| `entity/GlyphEntity.java` | Replace offset with rotation, implement new methods |
| `entity/HexEntity.java` | Replace offset with rotation, tier angular margins |
| `entity/GlyphComponent.java` | Replace offset fields with pitch/yaw |
| `hex/HexNode.java` | Add angular margin tracking |
| `mode/CompositionState.java` | Update for "drop into target" pattern |
| `mode/GlyphMode.java` | Rotation-based spawn and update |
| `casting/styles/BaseGlyphStyle.java` | New spawn rotation method |
| `casting/styles/RingGlyphStyle.java` | Implement rotation-based ring spawn |
| `interaction/HexcodeGlyphModeToggle.java` | Use RotationObserver |
| `interaction/HexcodeGlyphModeSelect.java` | Rotation-based drag/drop |
| `hex/Hex.java` | Rotation-aware visual traversal |

### Deprecated/Removed Files
| File | Status |
|------|--------|
| `casting/IntersectionObserver.java` | Remove after migration |
| `util/RaycastUtil.java` | Deprecate glyph-related methods |

---

## Notes

- **No backwards compatibility required** - clean break from old system
- **Glyph roles already removed** - execution uses Context modification pattern
- **Eye height**: 1.62 blocks above player feet position
- **Glyph distance**: 2.0 blocks from eye position
- **Base tolerance**: 5 degrees for single glyphs
- **Composed tolerance**: 7 degrees for hex parents
- **Queued casting**: Last selected hex queued for manual cast on exit
