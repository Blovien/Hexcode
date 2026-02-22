package com.riprod.hexcode.components;

import java.util.List;
import java.util.UUID;

import com.riprod.hexcode.core.execution.component.VariableMap;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

public class ExecutionContext {
    private VariableMap variableMap;
    private UUID currentNode;
    private int depth;

    public ExecutionContext() {
        this.variableMap = new VariableMap();
        this.depth = 0;
    }

    public VariableMap getVariableMap() {
        return variableMap;
    }

    public List<SpellVar> getVariable(int index) {
        return variableMap.get(index);
    }
    public void setVariable(int index, List<SpellVar> variables) {
        variableMap.set(index, variables);
    }

    public void setVariableMap(VariableMap variableMap) {
        this.variableMap = variableMap;
    }

    public UUID getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(UUID currentNode) {
        this.currentNode = currentNode;
    }

    public int incrementDepth() {
        return ++depth;
    }

    public ExecutionContext copy() {
        ExecutionContext copy = new ExecutionContext();
        copy.setVariableMap(this.variableMap.copy());
        copy.setCurrentNode(this.currentNode);
        copy.depth = this.depth;
        return copy;
    }
}
