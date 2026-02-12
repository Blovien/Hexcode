package com.riprod.hexcode.components;

import java.util.List;
import java.util.UUID;

import com.riprod.hexcode.core.execute.component.VariableMap;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

/**
 * Holds all of the information relating to the current hex execution
 * 
 * Will have all the various mutators and extensions that will dynamically change throughout the casting of the spell
 */
public class ExecutionContext {
    private VariableMap variableMap;
    private UUID currentNode;
    public ExecutionContext() {
        this.variableMap = new VariableMap();
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

    public ExecutionContext copy() {
        ExecutionContext copy = new ExecutionContext();
        copy.setVariableMap(this.variableMap.copy());
        copy.setCurrentNode(this.currentNode);
        return copy;
    }
}
