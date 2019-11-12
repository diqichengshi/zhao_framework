package org.springframework.beans.factory.parsing;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanReference;

import java.util.ArrayList;
import java.util.List;

public class BeanComponentDefinition extends BeanDefinitionHolder implements ComponentDefinition {

    private BeanDefinition[] innerBeanDefinitions;

    private BeanReference[] beanReferences;

    public BeanComponentDefinition(BeanDefinition beanDefinition, String beanName) {
        super(beanDefinition, beanName);
        findInnerBeanDefinitionsAndBeanReferences(beanDefinition);
    }

    public BeanComponentDefinition(BeanDefinitionHolder holder) {
        super(holder);
        findInnerBeanDefinitionsAndBeanReferences(holder.getBeanDefinition());
    }

    private void findInnerBeanDefinitionsAndBeanReferences(BeanDefinition beanDefinition) {
        List<BeanDefinition> innerBeans = new ArrayList<BeanDefinition>();
        List<BeanReference> references = new ArrayList<BeanReference>();
        PropertyValues propertyValues = beanDefinition.getPropertyValues();
        for (int i = 0; i < propertyValues.getPropertyValues().length; i++) {
            PropertyValue propertyValue = propertyValues.getPropertyValues()[i];
            Object value = propertyValue.getValue();
            if (value instanceof BeanDefinitionHolder) {
                innerBeans.add(((BeanDefinitionHolder) value).getBeanDefinition());
            } else if (value instanceof BeanDefinition) {
                innerBeans.add((BeanDefinition) value);
            } else if (value instanceof BeanReference) {
                references.add((BeanReference) value);
            }
        }
        this.innerBeanDefinitions = innerBeans.toArray(new BeanDefinition[innerBeans.size()]);
        this.beanReferences = references.toArray(new BeanReference[references.size()]);
    }

    @Override
    public String getName() {
        return getBeanName();
    }

    @Override
    public String getDescription() {
        return getShortDescription();
    }

    @Override
    public BeanDefinition[] getBeanDefinitions() {
        return new BeanDefinition[] {getBeanDefinition()};
    }

    @Override
    public BeanDefinition[] getInnerBeanDefinitions() {
        return this.innerBeanDefinitions;
    }

    @Override
    public BeanReference[] getBeanReferences() {
        return this.beanReferences;
    }


    /**
     * This implementation returns this ComponentDefinition's description.
     * @see #getDescription()
     */
    @Override
    public String toString() {
        return getDescription();
    }

    /**
     * This implementations expects the other object to be of type BeanComponentDefinition
     * as well, in addition to the superclass's equality requirements.
     */
    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof BeanComponentDefinition && super.equals(other)));
    }

}
