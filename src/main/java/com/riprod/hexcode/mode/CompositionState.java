package com.riprod.hexcode.mode;

import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.hex.HexNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks the current hex composition state during glyph mode.
 *
 * <p>With the unified HexNode treatment (where a single glyph is simply a
 * HexNode with no children), this class directly manages the root HexNode
 * instead of a separate Hex wrapper.
 *
 * Supports:
 * - Building the hex tree structure
 * - Undo functionality (step by step)
 * - Validation of composition rules
 */
public class CompositionState {
    private HexNode rootNode;
    private final Deque<CompositionAction> undoStack;
    private final int maxUndoSize;

    public CompositionState() {
        this(20); // Default undo stack size
    }

    public CompositionState(int maxUndoSize) {
        this.rootNode = null;
        this.undoStack = new ArrayDeque<>();
        this.maxUndoSize = maxUndoSize;
    }

    /**
     * @return The root HexNode being composed, or null if empty
     */
    @Nullable
    public HexNode getRoot() {
        return rootNode;
    }

    /**
     * @return true if there's nothing in the composition
     */
    public boolean isEmpty() {
        return rootNode == null;
    }

    /**
     * @return true if there are actions that can be undone
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Place a glyph as the root of an empty composition.
     *
     * @param glyph The glyph to place as root
     * @return true if successful, false if composition is not empty
     */
    public boolean placeRoot(GlyphInstance glyph) {
        if (glyph == null) {
            return false;
        }

        if (!isEmpty()) {
            // Composition already has a root - use wrapNode or addSibling instead
            return false;
        }

        HexNode newRoot = new HexNode(glyph);
        this.rootNode = newRoot;
        pushAction(new CompositionAction(CompositionActionType.PLACE_ROOT, newRoot, null));
        return true;
    }

    /**
     * Add a glyph to the composition.
     * If composition is empty, places as root.
     * Otherwise, wraps the root with this glyph.
     *
     * @param glyph The glyph to add
     * @return true if successful
     */
    public boolean addGlyph(GlyphInstance glyph) {
        if (isEmpty()) {
            return placeRoot(glyph);
        }
        // For non-empty composition, wrap the root with this glyph
        return wrapNode(glyph, rootNode);
    }

    /**
     * Wrap an existing node with a new glyph.
     * The new glyph becomes the parent of the target node.
     *
     * For example: If tree is A[B[C]] and we wrap B with X:
     * Result: A[X[B[C]]]
     *
     * @param glyph The glyph that will wrap the target
     * @param target The node to wrap
     * @return true if successful
     */
    public boolean wrapNode(GlyphInstance glyph, HexNode target) {
        if (glyph == null || target == null) {
            return false;
        }

        HexNode wrapperNode = new HexNode(glyph);
        HexNode parent = target.getParent();

        if (parent == null) {
            // Target is root, wrapper becomes new root
            if (wrapperNode.addChild(target)) {
                this.rootNode = wrapperNode;
                pushAction(new CompositionAction(CompositionActionType.WRAP, wrapperNode, target));
                return true;
            }
        } else {
            // Insert wrapper between parent and target
            if (parent.replaceChild(target, wrapperNode) && wrapperNode.addChild(target)) {
                pushAction(new CompositionAction(CompositionActionType.WRAP, wrapperNode, target));
                return true;
            }
        }
        return false;
    }

    /**
     * Add a glyph as a child of an existing node.
     * Used when adding an EFFECT to a MODIFIER or SELECT.
     *
     * @param glyph The glyph to add as child
     * @param parentNode The node that will become the parent
     * @return true if successful
     */
    public boolean addChild(GlyphInstance glyph, HexNode parentNode) {
        if (glyph == null || parentNode == null) {
            return false;
        }

        HexNode newNode = new HexNode(glyph);
        if (parentNode.addChild(newNode)) {
            pushAction(new CompositionAction(CompositionActionType.ADD_SIBLING, newNode, parentNode));
            return true;
        }
        return false;
    }

    /**
     * Add a glyph as a sibling to an existing node (chaining).
     * The new glyph becomes a sibling under the same parent.
     *
     * For example: If tree is A[B, C] and we add sibling D to B:
     * Result: A[B, C, D]
     *
     * @param glyph The glyph to add as sibling
     * @param existingNode The node to be a sibling of
     * @return true if successful
     */
    public boolean addSibling(GlyphInstance glyph, HexNode existingNode) {
        if (existingNode == null || glyph == null) {
            return false;
        }

        HexNode parent = existingNode.getParent();
        if (parent == null) {
            // Can't add sibling to root without a parent
            // Instead, we need to wrap the root first or handle this case specially
            return false;
        }

        HexNode newNode = new HexNode(glyph);
        if (parent.addChild(newNode)) {
            pushAction(new CompositionAction(CompositionActionType.ADD_SIBLING, newNode, parent));
            return true;
        }
        return false;
    }

    /**
     * @deprecated Use wrapNode() instead. This method has incorrect parameter naming.
     */
    @Deprecated
    public boolean addLeaf(GlyphInstance glyph, HexNode target) {
        return wrapNode(glyph, target);
    }

    /**
     * Insert a glyph as a child of a specific target node.
     *
     * <p>Used for tier-based drop detection where the player aims at a specific
     * tier/shell of a hex and the glyph is inserted as a child of that node.
     *
     * <p>This is similar to addChild() but uses INSERT_AT_NODE action type
     * for proper undo semantics with tier-based insertion context.
     *
     * @param glyph The glyph to insert
     * @param targetNode The node that will become the parent
     * @return true if successful
     */
    public boolean insertAtNode(GlyphInstance glyph, HexNode targetNode) {
        if (glyph == null || targetNode == null) {
            return false;
        }

        HexNode newNode = new HexNode(glyph);
        if (targetNode.addChild(newNode)) {
            pushAction(new CompositionAction(CompositionActionType.INSERT_AT_NODE, newNode, targetNode));
            return true;
        }
        return false;
    }

    /**
     * Undo the last action.
     *
     * @return true if an action was undone
     */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }

        CompositionAction action = undoStack.pop();
        switch (action.type) {
            case PLACE_ROOT:
            case PLACE_SAVED_HEX:
                this.rootNode = null;
                break;
            case WRAP:
                unwrap(action.node, action.target);
                break;
            case ADD_SIBLING:
            case INSERT_AT_NODE:
                if (action.target != null) {
                    action.target.removeChild(action.node);
                }
                break;
            case WRAP_WITH_SAVED_HEX:
                // Restore the previous root (which was wrapped)
                this.rootNode = action.target;
                break;
        }
        return true;
    }

    private void unwrap(HexNode wrapper, HexNode wrapped) {
        HexNode parent = wrapper.getParent();
        if (parent == null) {
            // Wrapper was root
            this.rootNode = wrapped;
        } else {
            parent.removeChild(wrapper);
            parent.addChild(wrapped);
        }
    }

    /**
     * Clear the entire composition.
     */
    public void clear() {
        this.rootNode = null;
        undoStack.clear();
    }

    /**
     * Load a HexNode tree into the composition state.
     *
     * <p>Clears any existing composition and loads the provided node tree.
     * The undo stack is cleared since this is a fresh load operation.
     *
     * <p>This is used when entering glyph mode to restore a previously
     * queued hex from the book.
     *
     * @param existingRoot The HexNode root to load into the composition
     */
    public void loadFromHexNode(@Nullable HexNode existingRoot) {
        clear();

        if (existingRoot == null) {
            return;
        }

        // Deep copy the hex tree to avoid modifying the original
        HexNode copiedRoot = deepCopyNode(existingRoot);
        this.rootNode = copiedRoot;
        // Don't add to undo stack - this is a load, not a user action
    }

    private void pushAction(CompositionAction action) {
        if (undoStack.size() >= maxUndoSize) {
            // Remove oldest action if at capacity
            ((ArrayDeque<CompositionAction>) undoStack).removeLast();
        }
        undoStack.push(action);
    }

    /**
     * @return Number of actions in undo stack
     */
    public int getUndoStackSize() {
        return undoStack.size();
    }

    // ==================== SAVED HEX SUPPORT ====================

    /**
     * Add a saved hex (HexNode tree) to the composition.
     * The saved hex expands to its full tree structure.
     *
     * @param savedHexRoot The root of the saved hex tree to add
     * @return true if added successfully
     */
    public boolean addSavedHex(@Nullable HexNode savedHexRoot) {

        if (savedHexRoot == null) {
            return false;
        }

        if (isEmpty()) {
            // First element - copy the saved hex's root as our root
            // Create a deep copy to avoid modifying the original
            HexNode copiedRoot = deepCopyNode(savedHexRoot);
            this.rootNode = copiedRoot;
            pushAction(new CompositionAction(CompositionActionType.PLACE_SAVED_HEX, copiedRoot, null));
            return true;
        }

        // If not empty, the saved hex wraps the current composition
        // This is like adding a SELECT glyph that has pre-defined children
        HexNode copiedRoot = deepCopyNode(savedHexRoot);
        HexNode currentRoot = this.rootNode;

        // Find the leaf of the copied saved hex tree
        HexNode leaf = findFirstLeaf(copiedRoot);
        if (leaf != null) {
            // Add current composition as a child of the saved hex's leaf
            leaf.addChild(currentRoot);
            this.rootNode = copiedRoot;
            pushAction(new CompositionAction(CompositionActionType.WRAP_WITH_SAVED_HEX, copiedRoot, currentRoot));
            return true;
        }

        // If the saved hex doesn't have a suitable insertion point,
        // we can't combine them
        return false;
    }

    /**
     * Deep copy a HexNode and all its children.
     */
    private HexNode deepCopyNode(HexNode node) {
        if (node == null) {
            return null;
        }

        HexNode copy = new HexNode(node.getValue());
        for (HexNode child : node.getChildren()) {
            HexNode childCopy = deepCopyNode(child);
            copy.addChild(childCopy);
        }
        return copy;
    }

    /**
     * Find the first leaf node (node with no children) in a tree.
     */
    private HexNode findFirstLeaf(HexNode node) {
        if (node == null) {
            return null;
        }
        if (node.getChildren().isEmpty()) {
            return node;
        }
        return findFirstLeaf(node.getChildren().get(0));
    }

    private enum CompositionActionType {
        PLACE_ROOT,
        WRAP,
        ADD_SIBLING,
        INSERT_AT_NODE,
        PLACE_SAVED_HEX,
        WRAP_WITH_SAVED_HEX
    }

    private static class CompositionAction {
        final CompositionActionType type;
        final HexNode node;
        final HexNode target; // HexNode for WRAP, parent for ADD_SIBLING

        CompositionAction(CompositionActionType type, HexNode node, HexNode target) {
            this.type = type;
            this.node = node;
            this.target = target;
        }
    }
}
