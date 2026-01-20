package com.riprod.hexcode.mode;

import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.hex.HexNode;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks the current hex composition state during glyph mode.
 *
 * Supports:
 * - Building the hex tree structure
 * - Undo functionality (step by step)
 * - Validation of composition rules
 */
public class CompositionState {
    private Hex hex;
    private final Deque<CompositionAction> undoStack;
    private final int maxUndoSize;

    public CompositionState() {
        this(20); // Default undo stack size
    }

    public CompositionState(int maxUndoSize) {
        this.hex = new Hex();
        this.undoStack = new ArrayDeque<>();
        this.maxUndoSize = maxUndoSize;
    }

    /**
     * @return The current hex being composed
     */
    public Hex getHex() {
        return hex;
    }

    /**
     * @return true if there's nothing in the composition
     */
    public boolean isEmpty() {
        return hex.isEmpty();
    }

    /**
     * @return true if there are actions that can be undone
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Add a glyph to the composition.
     * If composition is empty, places as root.
     * Otherwise, attempts to add based on glyph role and current state.
     *
     * @param glyph The glyph to add
     * @return true if successful
     */
    public boolean addGlyph(Glyph glyph) {
        if (isEmpty()) {
            return placeAsRoot(glyph);
        }
        // For non-empty composition, wrap the root with this glyph
        HexNode root = hex.getRoot();
        return wrapNode(glyph, root);
    }

    /**
     * Place a glyph as the root of the hex.
     *
     * @param glyph The glyph to place
     * @return true if successful
     */
    public boolean placeAsRoot(Glyph glyph) {
        if (!hex.isEmpty()) {
            return false;
        }
        HexNode node = new HexNode(glyph);
        hex.setRoot(node);
        pushAction(new CompositionAction(CompositionActionType.PLACE_ROOT, node, null));
        return true;
    }

    /**
     * Wrap an existing node with a new glyph (modifier or select).
     *
     * @param wrapper The glyph that will wrap
     * @param target The node to wrap
     * @return true if successful
     */
    public boolean wrapNode(Glyph wrapper, HexNode target) {
        if (target == null || wrapper == null) {
            return false;
        }

        HexNode wrapperNode = new HexNode(wrapper);
        HexNode parent = target.getParent();

        if (parent == null) {
            // Target is root, wrapper becomes new root
            if (wrapperNode.addChild(target)) {
                hex.setRoot(wrapperNode);
                pushAction(new CompositionAction(CompositionActionType.WRAP, wrapperNode, target));
                return true;
            }
        } else {
            // Replace target with wrapper in parent's children
            if (parent.removeChild(target) && wrapperNode.addChild(target) && parent.addChild(wrapperNode)) {
                pushAction(new CompositionAction(CompositionActionType.WRAP, wrapperNode, target));
                return true;
            }
        }
        return false;
    }

    /**
     * Add a sibling to an existing node (for linking).
     *
     * @param glyph The glyph to add as sibling
     * @param existingNode The node to be a sibling of
     * @return true if successful
     */
    public boolean addSibling(Glyph glyph, HexNode existingNode) {
        if (existingNode == null || glyph == null) {
            return false;
        }

        HexNode parent = existingNode.getParent();
        if (parent == null) {
            return false; // Can't add sibling to root without a parent
        }

        HexNode newNode = new HexNode(glyph);
        if (parent.addChild(newNode)) {
            pushAction(new CompositionAction(CompositionActionType.ADD_SIBLING, newNode, parent));
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
                hex.setRoot(null);
                break;
            case WRAP:
                unwrap(action.node, action.target);
                break;
            case ADD_SIBLING:
                if (action.target != null) {
                    ((HexNode) action.target).removeChild(action.node);
                }
                break;
        }
        return true;
    }

    private void unwrap(HexNode wrapper, HexNode wrapped) {
        HexNode parent = wrapper.getParent();
        if (parent == null) {
            // Wrapper was root
            hex.setRoot(wrapped);
        } else {
            parent.removeChild(wrapper);
            parent.addChild(wrapped);
        }
    }

    /**
     * Clear the entire composition.
     */
    public void clear() {
        hex = new Hex();
        undoStack.clear();
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

    private enum CompositionActionType {
        PLACE_ROOT,
        WRAP,
        ADD_SIBLING
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
