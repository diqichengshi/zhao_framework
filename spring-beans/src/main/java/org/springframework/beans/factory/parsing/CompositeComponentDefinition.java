package org.springframework.beans.factory.parsing;

import org.springframework.util.Assert;

import java.util.LinkedList;
import java.util.List;

public class CompositeComponentDefinition implements ComponentDefinition {

    private final String name;

    private final Object source;

    private final List<ComponentDefinition> nestedComponents = new LinkedList<ComponentDefinition>();

    public CompositeComponentDefinition(String name, Object source) {
        Assert.notNull(name, "Name must not be null");
        this.name = name;
        this.source = source;
    }

    public void addNestedComponent(ComponentDefinition component) {
        Assert.notNull(component, "ComponentDefinition must not be null");
        this.nestedComponents.add(component);
    }

}
