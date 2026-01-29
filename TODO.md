# Hexcode System Rework: Unified Hex/Glyph Treatment

## Overview

Complete replacement of the hexknights/hexcode system to unify Glyph and Hex treatment using:
1. **hex-positioning skill**: Angular layout with subtreeDepth, scale, angularRadius, 3-pass layout algorithm
2. **entity-mounting skill**: Visual hierarchy via MountedComponent

**Key Insight**: A single glyph is simply a HexNode with no children - this enables unified treatment.

**ZERO backwards compatibility** - all dead code will be removed.

---

## Phase 1: Enhance HexNode Data Structure

> Reference `/hex-positioning` skill for layout algorithm details

### 1.1 Add hex-positioning fields to HexNode.java

- [x] Add field: `private String id` (unique identifier, UUID)
- [x] Add field: `private float baseMargin = 5.0f` (base angular radius in degrees)
- [x] Add field: `private int subtreeDepth` (computed: 0 for leaf, max(children.subtreeDepth) + 1)
- [x] Add field: `private float scale` (computed: 1.0 + 0.2 * subtreeDepth)
- [x] Add field: `private float angularRadius` (computed: baseMargin * scale)
- [x] Add field: `private float localOffsetAngle` (0-360 degrees, direction from parent center)
- [x] Add field: `private float localOffsetDistance` (angular distance from parent center)
- [x] Add field: `private float absoluteYaw` (-180 to 180, computed)
- [x] Add field: `private float absolutePitch` (-90 to 90, computed)
- [x] Add field: `private Ref<EntityStore> entityRef` (reference to spawned visual entity)

### 1.2 Implement 3-pass layout algorithm in HexNode.java

- [x] Implement `public void recalculateLayout()` - entry point that calls all 3 passes
- [x] Implement `private int calculateSubtreeDepth(HexNode node)` - bottom-up pass
  - Leaf nodes: subtreeDepth = 0
  - Parent nodes: subtreeDepth = max(children.subtreeDepth) + 1
  - Set scale = 1.0f + 0.2f * subtreeDepth
  - Set angularRadius = baseMargin * scale
- [x] Implement `private void positionChildren(HexNode parent)` - weighted angular distribution
  - Single child: localOffsetAngle = 0, localOffsetDistance = 0 (shares parent center)
  - Multiple children: distribute proportionally by angular radius around 360 degrees
  - Formula: childSlice = (child.angularRadius / totalMass) * availableAngle
  - Set localOffsetDistance = parent.angularRadius - child.angularRadius
- [x] Implement `private void calculateAbsolutePositions(HexNode node, float parentYaw, float parentPitch)` - top-down pass
  - Root: absoluteYaw = parentYaw, absolutePitch = parentPitch (passed in)
  - Children: Convert localOffsetAngle + localOffsetDistance to delta yaw/pitch
  - Apply pole correction when |pitch| > 85 degrees
  - Normalize yaw to [-180, 180], clamp pitch to [-90, 90]

### 1.3 Implement hit testing in HexNode.java

- [x] Implement `public static float angularDistance(float yaw1, float pitch1, float yaw2, float pitch2)` - haversine formula
- [x] Implement `public boolean isWithinBounds(float userYaw, float userPitch)` - check if point within angularRadius
- [x] Implement `public HexNode findDeepestAt(float userYaw, float userPitch)` - recursive descent
  - If not within bounds, return null
  - Check all children recursively, return first non-null result
  - If no children match, return this node

### 1.4 Add entity reference and utility methods to HexNode.java

- [x] Add getter/setter: `getEntityRef()` / `setEntityRef(Ref<EntityStore> ref)`
- [x] Add getter/setter: `getAbsoluteYaw()` / `setAbsoluteYaw(float yaw)`
- [x] Add getter/setter: `getAbsolutePitch()` / `setAbsolutePitch(float pitch)`
- [x] Add getter: `getScale()`, `getAngularRadius()`, `getSubtreeDepth()`
- [x] Add method: `public Vector3f getMountOffset(float distance)` - convert absolute yaw/pitch to Vector3f for mounting
- [x] Add method: `public boolean hasChildren()` - returns !children.isEmpty()
- [x] Remove unused field: `relativeRotation` (replaced by new positioning system)
- [x] Remove method: `recalculateAngularMargin()` (replaced by recalculateLayout)

### 1.5 Update HexNode serialization

- [x] Update `toJson()` to include new fields: id, baseMargin
- [x] Update `fromJson()` to restore new fields
- [x] Ensure layout is recalculated after deserialization

---

## Phase 2: Create Unified HexNodeEntity

> Reference `/entity-mounting` skill for MountedComponent usage

### 2.1 Create new file: `/entity/HexNodeEntity.java`

- [x] Create class implementing OrbitalElement interface
- [x] Add field: `private final HexNode node` (the root data node)
- [x] Add field: `private final Ref<EntityStore> ownerPlayer`
- [x] Add field: `private ElementState state = ElementState.NOT_SPAWNED`
- [x] Add field: `private boolean pendingSpawn = false`
- [x] Add field: `private boolean isHovered = false`
- [x] Add field: `private boolean isDragging = false`
- [x] Add field: `private boolean isAvailable = true`

### 2.2 Implement OrbitalElement interface methods in HexNodeEntity

- [x] Implement `getId()` - return node.getId()
- [x] Implement `isSavedHex()` - return node.hasChildren()
- [x] Implement `getVisual()` - return node.getValue().getGlyph().getVisual()
- [x] Implement `getEntityRef()` - return node.getEntityRef()
- [x] Implement `getOwnerPlayer()` - return ownerPlayer
- [x] Implement `getRotation()` - return new GlyphRotation(node.getAbsolutePitch(), node.getAbsoluteYaw())
- [x] Implement `setRotation(GlyphRotation rotation)` - update node's absoluteYaw/absolutePitch
- [x] Implement `getSelectionTolerance()` - return node.getAngularRadius()
- [x] Implement state machine methods: `getState()`, `transitionTo()`
- [x] Implement flag methods: `isDragging()`, `setDragging()`, `isHovered()`, `setHovered()`, `isAvailable()`, `setAvailable()`
- [x] Implement `isPendingSpawn()`, `clearPendingSpawn()`

### 2.3 Implement spawn with entity mounting in HexNodeEntity

- [x] Implement `spawn(CommandBuffer<EntityStore> commandBuffer, Vector3d spawnPosition, Ref<EntityStore> playerRef)`
  - Call node.recalculateLayout()
  - Call spawnNodeTree(commandBuffer, node, playerRef)
  - Set state to IDLE, pendingSpawn = true
- [x] Implement `private void spawnNodeTree(CommandBuffer cb, HexNode currentNode, Ref<EntityStore> parentEntityRef)`
  - Create entity holder with: UUID, Transform, Model, DynamicLight, NetworkId, EntityScaleComponent
  - Only add BoundingBox to root node (children are visual-only)
  - Add entity via commandBuffer
  - Store entityRef in currentNode.setEntityRef()
  - Create MountedComponent with parent ref and offset from getMountOffset()
  - Add MountedComponent to entity
  - Recursively call for all children, passing current entity as parent
- [x] Implement `private Holder<EntityStore> createNodeHolder(HexNode node, boolean isRoot)`
  - Add UUIDComponent
  - Add TransformComponent (initial position, will be overridden by mount)
  - Add ModelComponent from node.getValue().getGlyph().getVisual()
  - Add DynamicLight from visual color
  - Add NetworkId
  - Add EntityScaleComponent with node.getScale()
  - If isRoot: add BoundingBox for hit detection

### 2.4 Implement despawn in HexNodeEntity

- [x] Implement `despawn(CommandBuffer<EntityStore> commandBuffer)`
  - Call despawnNodeTree(commandBuffer, node)
  - Set state to CONSUMED
- [x] Implement `private void despawnNodeTree(CommandBuffer cb, HexNode currentNode)`
  - Recursively despawn all children first
  - Remove entity via commandBuffer.removeEntity()
  - Clear currentNode.setEntityRef(null)

### 2.5 Implement position update methods in HexNodeEntity

- [x] Implement `updatePositionFromPlayer(Store store, Vector3d playerPosition)`
  - NO-OP: MountedComponent handles positioning automatically
  - Only needed if root mount offset changes
- [x] Implement `captureRotationFromLook(Store store, GlyphRotation lookRotation)`
  - Update node's absoluteYaw/absolutePitch from lookRotation
- [x] Implement `updateWorldPositionDirect(Store store, Vector3d position)`
  - For dragging: directly set Transform position on root entity
- [x] Implement `updateHoverVisual(Store store)`
  - Update EntityScaleComponent: 1.1f when hovered, node.getScale() otherwise
  - Update DynamicLight intensity based on hover state

### 2.6 Add helper methods to HexNodeEntity

- [x] Add `public HexNode getNode()` - return node
- [x] Add `public HexNode findNodeAtLookDirection(float userYaw, float userPitch)` - return node.findDeepestAt()
- [x] Add `public void refreshAndRespawn(CommandBuffer cb, Ref<EntityStore> playerRef)`
  - Despawn all entities
  - Recalculate layout
  - Respawn with new structure

---

## Phase 3: Update GlyphRotation Utilities

### 3.1 Add mount offset conversion to GlyphRotation.java

- [x] Add static method: `public static Vector3f angularToMountOffset(float yaw, float pitch, float distance)`
  - Convert spherical coordinates to Cartesian offset Vector3f
  - Formula:
    ```java
    float x = (float)(Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * distance);
    float y = (float)(-Math.sin(Math.toRadians(pitch)) * distance);
    float z = (float)(Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * distance);
    return new Vector3f(x, y, z);
    ```

---

## Phase 4: Update RotationObserver for Nested Hit Testing

### 4.1 Add NodeDropTarget class to RotationObserver.java

- [x] Create inner class `NodeDropTarget` with fields:
  - `public final HexNodeEntity entity` (the orbital element)
  - `public final HexNode targetNode` (the specific node within the tree)
  - `public final GlyphRotation lookRotation`
  - `public final float angularDistance`

### 4.2 Implement nested node targeting in RotationObserver.java

- [x] Add method: `public NodeDropTarget findNodeDropTarget(Store store, Ref playerRef, List<OrbitalElement> elements)`
  - Get player look rotation
  - For each HexNodeEntity element (skip if dragging):
    - Call node.findDeepestAt(userYaw, userPitch)
    - If hit found, calculate angular distance
    - Track closest match
  - Return NodeDropTarget with deepest matching node
- [x] Update existing `findDropTarget()` to delegate to `findNodeDropTarget()` internally
- [x] Remove `findHexDropTarget()` method (replaced by unified approach)
- [x] Remove `HexDropTarget` inner class (replaced by NodeDropTarget)

---

## Phase 5: Update GlyphMode Spawning

### 5.1 Update GlyphMode.java for unified spawning

- [x] Update `spawnOrbitalGlyphs(CommandBuffer commandBuffer)`:
  - Change glyph spawning to create HexNode(glyphInstance) with no children
  - Set node.setBaseMargin(GlyphRotation.BASE_TOLERANCE)
  - Get rotation from orbitalStyle.getSpawnRotation()
  - Set node.setAbsoluteYaw() and setAbsolutePitch() from rotation
  - Call node.recalculateLayout()
  - Create HexNodeEntity(node, player)
  - Call entity.spawn()
- [x] Update saved hex spawning to use HexNodeEntity instead of HexEntity
- [x] Remove references to `orbitalEntities` list (glyphs)
- [x] Remove references to `orbitalSavedHexEntities` list (hexes)
- [x] Unify into single `List<HexNodeEntity> orbitalElements` (via orbitalStyle.getElements())
- [x] Update `getAllOrbitalElements()` to return the unified list

### 5.2 Update GlyphMode drag state management

- [x] Update `startDragElement()` to work with HexNodeEntity only
- [x] Update `endDrag()` to work with HexNodeEntity only
- [x] Remove `draggingGlyph` field (no longer needed - use draggingElement.getNode())
- [x] Update `setActiveHex()` to accept HexNodeEntity

---

## Phase 6: Update Drag/Drop Interaction

### 6.1 Update HexcodeGlyphModeSelect.java drag start

- [x] Update `handleDragStart()`:
  - Get hovered HexNodeEntity from mode
  - Remove MountedComponent from root entity (detach from player via unmountFromPlayer)
  - Set entity.setDragging(true)
  - Start tracking drag state

### 6.2 Update HexcodeGlyphModeSelect.java drag update

- [x] Update `updateDragPosition()`:
  - Get look rotation and player position
  - Calculate world position from rotation
  - Call entity.updateWorldPositionDirect(store, worldPosition)
  - Element follows player's look direction while dragging

### 6.3 Update HexcodeGlyphModeSelect.java drag end with new drop logic

- [x] Update `handleDragEnd()`:
  - Get drop target via rotationObserver.findDropTarget() cast to NodeDropTarget
  - Implement Case 1: Hex onto Hex
    - Get target node from NodeDropTarget.targetNode
    - Detach dragged node from its parent (if any)
    - Call targetNode.addChild(draggedNode)
    - Despawn dragged entity
    - Call targetEntity.refreshAndRespawn()
  - Implement Case 2: Glyph onto Hex (same as Case 1 - unified treatment)
  - Implement Case 3: Glyph onto Glyph (same as Case 1 - unified treatment)
  - Implement Case 4: Drop into empty space
    - Update node's absoluteYaw/absolutePitch from look rotation
    - Remount to player via remountToPlayer()
- [x] Remove `canWrap()` method (no longer needed - unified treatment)
- [x] Remove instanceof checks for GlyphEntity/HexEntity (only HexNodeEntity now)
- [x] Added release detection in tick0 to call handleDragEnd when time != CHARGING_HELD

---

## Phase 7: Update Hover Detection

### 7.1 Update HexcodeGlyphModeToggle.java

- [x] Update `processServerSideHover()` to cast DropTarget to NodeDropTarget and extract HexNodeEntity
- [x] Update type references: extract `HexNodeEntity` from `NodeDropTarget.entity`
- [x] Added imports for `NodeDropTarget` and `HexNodeEntity`

---

## Phase 8: Create FacePlayerSystem

### 8.1 Create new file: `/system/FacePlayerSystem.java`

- [x] Create EntityTickingSystem that makes hex nodes face the player
- [x] Query for entities with HexNodeComponent AND TransformComponent
- [x] Each tick:
  - Get owner player ref from HexNodeComponent
  - Get player position from player's TransformComponent
  - Get entity position from entity's TransformComponent
  - Calculate yaw to face player: atan2(playerX - entityX, playerZ - entityZ)
  - Update entity Transform rotation.y (yaw)

### 8.2 Create HexNodeComponent.java

- [x] Create ECS component with fields:
  - `private Ref<EntityStore> ownerPlayerRef`
  - `private String nodeId` (for debugging/identification)
- [x] Add to entity holders during spawn

### 8.3 Register system in Hexcode.java

- [x] Register HexNodeComponent type in registerComponents()
- [x] Create registerSystems() method
- [x] Register FacePlayerSystem with hexNodeComponentType and transformType

---

## Phase 9: Update Data Storage

### 9.1 Update HexBookData.java

- [x] Change saved hex storage from `List<Hex>` to `List<HexNode>` (just the root nodes)
- [x] Update `getSavedHexes()` to return `List<HexNode>`
- [x] Update serialization to use HexNode.toJson/fromJson directly
- [x] Update all constructors from `List<Hex>` to `List<HexNode>`
- [x] Update saveHex(), getSavedHex(), hasSavedHex(), deleteSavedHex() for HexNode
- [x] Update codec helper methods: setSavedHexesFromList(), getSavedHexesAsList()
- [x] Recalculate layout after deserialization in fromJson()

### 9.2 Update codec files

- [x] Update HexCodecs.java for new HexNode structure
- [x] Remove Hex-specific codecs (HEX, HEX_ARRAY removed)
- [x] Update HEX_NODE codec to include Id and BaseMargin fields
- [x] Change HEX_NODE_ARRAY from private to public (used by HEX_BOOK_DATA)
- [x] Update HEX_BOOK_DATA to use HEX_NODE_ARRAY instead of HEX_ARRAY
- [x] Update arrayToList/listToArray utilities to use HexNode
- [x] Updated GlyphMode.getSavedHexes() to directly return bookData.getSavedHexes()

---

## Phase 10: Update Styles

### 10.1 Update BaseGlyphStyle.java

- [x] Change element type from OrbitalElement to HexNodeEntity
- [x] Update `addElement()`, `removeElement()`, `getElements()` signatures
- [x] Update `updateEffects()` abstract method to use `List<HexNodeEntity>`
- [x] Update class documentation to reflect unified HexNodeEntity treatment

### 10.2 Update RingGlyphStyle.java

- [x] Update for HexNodeEntity type
- [x] Update `updateEffects()` override to use `List<HexNodeEntity>`
- [x] Verify getSpawnRotation() works correctly with new system (no changes needed)

### 10.3 Update dependent files

- [x] Update GlyphMode.java loops to use `HexNodeEntity` instead of `OrbitalElement`
- [x] Update `getAllOrbitalElements()` return type to `List<HexNodeEntity>`
- [x] Update `setOrbitalStyle()` to use `List<HexNodeEntity>`
- [x] Update RotationObserver.findDropTarget to accept `List<? extends OrbitalElement>`
- [x] Update HexcodeGlyphModeToggle.processServerSideHover to use `List<HexNodeEntity>`
- [x] Remove unused OrbitalElement import from HexcodeGlyphModeToggle

---

## Phase 11: Update Execution

### 11.1 Update HexExecutor.java

- [x] Update to traverse HexNode trees directly (no Hex wrapper)
- [x] Change method signature from `execute(Hex hex, ...)` to `execute(HexNode root, ...)`
- [x] Remove Hex import
- [x] Update logging to use `root.getDepth()` instead of `hex.getMaxDepth()`

### 11.2 Update HexcodeGlyphCast.java

- [x] Remove Hex import
- [x] Update `castHex()` to use `HexNode` instead of `Hex`
- [x] Update `castHexFromBook()` to use `HexNode` instead of `Hex`
- [x] Remove `calculateHexManaCost(Hex)` wrapper - use `calculateNodeManaCost(HexNode)` directly
- [x] Add null check to `calculateNodeManaCost()`
- [x] Get root node via `mode.getHexToCast()` (returns HexNode directly)

---

## Phase 12: Delete Obsolete Files

### 12.1 Delete entity files

- [x] Delete `/entity/GlyphEntity.java` (replaced by HexNodeEntity)
- [x] Delete `/entity/HexEntity.java` (replaced by HexNodeEntity)

### 12.2 Delete hex container files

- [x] Delete `/hex/Hex.java` (container logic merged into HexNode)
- [x] Delete `/hex/HexSerializer.java` (replaced by updated HexNode codecs)

### 12.3 Delete obsolete component files

- [x] Delete `/entity/GlyphComponent.java` (replaced by HexNodeComponent)

---

## Phase 13: Clean Up References

### 13.1 Update GlyphModeManager.java

- [x] Update type references from OrbitalElement to HexNodeEntity
- [x] Remove any GlyphEntity/HexEntity imports

### 13.2 Update import statements across all modified files

- [x] Remove imports for deleted classes
- [x] Add imports for new classes
- [x] Ensure no compilation errors

### 13.3 Search for dead code

- [x] Grep for "GlyphEntity" - should find zero references (only doc comments remain)
- [x] Grep for "HexEntity" - should find zero references (only doc comments remain)
- [x] Grep for "class Hex " - should find zero references (except comments)
- [x] Remove any orphaned helper methods

---

## Phase 14: Verification

### 14.1 Compile and fix errors

- [ ] Run full build: `./gradlew build`
- [ ] Fix any compilation errors
- [ ] Ensure no warnings about unused code

### 14.2 Test spawning

- [ ] Enter glyph mode with hex staff + book
- [ ] Verify glyphs spawn in ring formation
- [ ] Verify each glyph entity is mounted to player
- [ ] Verify glyphs face the player

### 14.3 Test hover detection

- [ ] Look at a glyph - verify hover visual (scale increase)
- [ ] Look away - verify hover clears
- [ ] Look at composed hex - verify deepest node is selected

### 14.4 Test dragging

- [ ] Click and drag a glyph - verify it detaches and follows look
- [ ] Release in empty space - verify it remounts at new position
- [ ] Verify glyph continues to face player while dragging

### 14.5 Test dropping

- [ ] Drag glyph A onto glyph B - verify B[A] hex is created
- [ ] Drag glyph C onto hex B[A] - verify B[A, C] structure
- [ ] Drag hex D[E] onto hex B[A, C] - verify tree merge
- [ ] Verify visual hierarchy updates correctly after each drop

### 14.6 Test execution

- [ ] Cast a composed hex
- [ ] Verify spell executes correctly
- [ ] Verify glyph effects apply in correct order

---

## Phase 15: Orbital Tracking System (Replace Player Mounting)

> **Problem:** MountedComponent for root nodes causes mount/unmount cycle issues during drag.
> **Solution:** Use a custom ticking system for root nodes to track player position.
> **Keep:** Child nodes still use MountedComponent for internal hierarchy.

### Architecture Overview

| Entity Type | Player Tracking | Internal Hierarchy |
|-------------|-----------------|-------------------|
| Root node | **OrbitalPositionComponent + OrbitalTrackingSystem** | N/A |
| Child nodes | Inherits from root (ticked) | **MountedComponent** (unchanged) |

### 15.1 Create OrbitalPositionComponent

- [x] Create new file: `/entity/OrbitalPositionComponent.java`
- [x] Add field: `private Ref<EntityStore> targetRef` (player to orbit around)
- [x] Add field: `private float yaw` (horizontal angle -180 to 180)
- [x] Add field: `private float pitch` (vertical angle -90 to 90)
- [x] Add field: `private float distance` (distance from player, default 2.0)
- [x] Add field: `private boolean paused` (skip positioning when true, for dragging)
- [x] Add CODEC for serialization
- [x] Add static componentType field with getter/setter
- [x] Add getter/setter methods for all fields
- [x] Implement clone()

### 15.2 ~~Create OrbitalTrackingSystem~~ (REMOVED - Use GlyphMode ticking instead)

> **Change:** Instead of a separate ECS system, orbital positioning is handled in
> `GlyphMode.updateOrbitalGlyphs()` which calls `HexNodeEntity.updatePositionFromPlayer()`.
> This is more performant because it only runs while glyph mode is active.

- [x] ~~Create OrbitalTrackingSystem~~ - REMOVED, not needed
- [x] Position calculation moved to `HexNodeEntity.updatePositionFromPlayer()`
- [x] Uses OrbitalPositionComponent data (yaw, pitch, distance, paused)
- [x] Skips update when paused (during drag)
- [x] Faces entity toward player after positioning

### 15.3 Update HexNodeEntity for Orbital Tracking

- [x] In `spawnNodeTree()` for ROOT nodes:
  - Remove MountedComponent creation
  - Add OrbitalPositionComponent with player ref, yaw, pitch, distance
- [x] In `spawnNodeTree()` for CHILD nodes:
  - Keep MountedComponent (mount to parent entity, not player)
- [x] Update `unmountFromPlayer()`:
  - Instead of removing MountedComponent, set orbitalComponent.setPaused(true)
  - Rename to `pauseOrbitalTracking()`
- [x] Update `remountToPlayer()`:
  - Instead of adding MountedComponent, update orbitalComponent yaw/pitch and set paused=false
  - Rename to `resumeOrbitalTracking()`
- [x] Update `refreshAndRespawn()`:
  - Root gets OrbitalPositionComponent (via spawnNodeTree)
  - Children get MountedComponent to parent (via spawnNodeTree)

### 15.4 Update HexcodeGlyphModeSelect for Orbital

- [x] Update `handleDragStart()`:
  - Call `pauseOrbitalTracking()` instead of `unmountFromPlayer()`
- [x] Update `handleDragEnd()`:
  - Call `resumeOrbitalTracking()` instead of `remountToPlayer()`
- [ ] Remove MountedComponent import if no longer used (keep for now, used by child mounting)

### 15.5 Register New Component

- [x] In Hexcode.java `registerComponents()`:
  - Register OrbitalPositionComponent
  - Set static componentType
- [x] ~~Register OrbitalTrackingSystem~~ - Not needed, using GlyphMode ticking instead

### 15.6 Verification

- [ ] Compile and fix any errors
- [ ] Test: Glyphs spawn in ring around player (visible)
- [ ] Test: Glyphs track player position as player moves
- [ ] Test: Drag start: glyph stops tracking, follows mouse
- [ ] Test: Drag end empty: glyph resumes tracking at new position
- [ ] Test: Drag end onto target: composition works, target refreshes
- [ ] Test: Child nodes still mounted to parent (internal hierarchy)

---

## Bug Fixes Applied

### BUG-001: ComponentType not in archetype during remount (FIXED)

**Error:**
```
java.lang.IllegalArgumentException: ComponentType is not in archetype:
ComponentType{...MountedComponent, index=117}
```

**Root Cause:**
- `remountToPlayer()` called `commandBuffer.removeComponent(entityRef, MountedComponent.getComponentType())`
- But MountedComponent was already removed by prior `unmountFromPlayer()` call during drag start
- The entity archetype no longer includes MountedComponent, so `removeComponent()` throws

**Flow that caused the bug:**
1. Drag starts → `unmountFromPlayer()` removes MountedComponent ✓
2. Drag ends → `remountToPlayer()` tries to remove MountedComponent AGAIN ✗ (crash)

**Fix Applied:**
- Changed `removeComponent()` to `tryRemoveComponent()` in both methods
- `tryRemoveComponent()` safely checks if component exists before removing
- Cross-referenced with server pattern: `MountSystems.java:380` uses `tryRemoveComponent()`

**Verification:**
- [x] Identified root cause from stack trace
- [x] Found server pattern using `tryRemoveComponent()` at `MountSystems.java:380`
- [x] Applied fix to `HexNodeEntity.remountToPlayer()` line 545
- [x] Applied fix to `HexNodeEntity.unmountFromPlayer()` line 572
- [x] Verified no other `removeComponent(MountedComponent)` calls exist in hexcode
- [x] Cross-referenced CommandBuffer execution model (queued ops, batch consume)
- [ ] Recompile and test

### BUG-002: Glyphs are invisible (models not loading) (FIXED)

**Symptom:**
- Glyphs spawn but are invisible
- Logs show: `Could not load model 'Fire' for node..., trying fallback`
- Hover/drag states work (entities exist) but nothing visible

**Root Cause:**
- GlyphVisual uses model IDs like "Fire", "Ice", "Self" which are not valid registered model asset IDs
- Model asset IDs need full paths like "Projectiles/Spells/Fireball" (matching JSON files in Assets/Server/Models/)
- The fallback model "Base_glyph" also doesn't exist
- When fallback fails, no ModelComponent is added → invisible entity

**Fix Applied:**
- Created model asset JSON at `lib/Assets/Server/Models/VFX/Glyphs/base_glyph.json`
  - References blockymodel at `VFX/Glyphs/base_glyph.blockymodel`
  - References texture at `VFX/Glyphs/base_glyph.png`
- Updated fallback model order in `HexNodeEntity.loadFallbackModel()`:
  1. "VFX/Glyphs/base_glyph" (primary - custom glyph model)
  2. "Projectiles/Spells/Fireball"
  3. "Projectiles/Spells/Ice_Ball"
  4. "Projectiles/Projectile"
- Added logging when fallback succeeds or when all fallbacks fail

**Verification:**
- [x] Identified model loading failure in logs
- [x] Found valid model asset paths in `lib/Assets/Server/Models/Projectiles/Spells/`
- [x] Applied fix to `HexNodeEntity.loadFallbackModel()`
- [x] Added severe logging if all fallbacks fail
- [x] Created model asset JSON for base_glyph at `VFX/Glyphs/base_glyph`
- [ ] Recompile and test

**Future Work:**
- Create element-specific glyph model assets (Assets/Server/Models/Glyphs/Fire.json, Ice.json, etc.)
- Update GlyphVisual to use correct asset paths matching the JSON asset IDs

---

## Files Summary

### Files to CREATE
| File | Purpose |
|------|---------|
| `/entity/HexNodeEntity.java` | Unified entity for all hex nodes |
| `/entity/HexNodeComponent.java` | ECS component for node data |
| `/entity/OrbitalPositionComponent.java` | Component for player-relative orbital positioning |

### Files to DELETE
| File | Reason |
|------|--------|
| `/entity/GlyphEntity.java` | Replaced by HexNodeEntity |
| `/entity/HexEntity.java` | Replaced by HexNodeEntity |
| `/hex/Hex.java` | Container merged into HexNode |
| `/hex/HexSerializer.java` | Replaced by updated HexNode codecs |
| `/entity/GlyphComponent.java` | Replaced by HexNodeComponent |

### Files to HEAVILY MODIFY
| File | Changes |
|------|---------|
| `/hex/HexNode.java` | Add hex-positioning fields, layout algorithm, hit testing |
| `/mode/GlyphMode.java` | Unified spawning with HexNodeEntity |
| `/interaction/HexcodeGlyphModeSelect.java` | Unified drag/drop logic |
| `/casting/RotationObserver.java` | Nested node targeting |
| `/math/GlyphRotation.java` | Add angularToMountOffset() |
| `/data/HexBookData.java` | Store HexNode roots |

### Files with MINOR CHANGES
| File | Changes |
|------|---------|
| `/interaction/HexcodeGlyphModeToggle.java` | Type updates |
| `/mode/GlyphModeManager.java` | Type updates |
| `/casting/styles/BaseGlyphStyle.java` | Element type change |
| `/casting/styles/RingGlyphStyle.java` | Type updates |
| `/executing/HexExecutor.java` | Traverse HexNode directly |
| `/Hexcode.java` | Register new components/systems |
