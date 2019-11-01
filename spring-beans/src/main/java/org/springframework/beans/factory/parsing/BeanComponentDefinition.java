package org.springframework.beans.factory.parsing;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanReference;

import java.util.ArrayList;
import java.util.List;

public class BeanComponentDefinition extends BeanDefinitionHolder  implements ComponentDefinition {

    private BeanDefinition[] innerBeanDefinitions;

    private BeanReference[] beanReferences;

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
            }
            else if (value instanceof BeanDefinition) {
                innerBeans.add((BeanDefinition) value);
            }
            else if (value instanceof BeanReference) {
                references.add((BeanReference) value);
            }
        }
        this.innerBeanDefinitions = innerBeans.toArray(new BeanDefinition[innerBeans.size()]);
        this.beanReferences = references.toArray(new BeanReference[references.size()]);
    }

}
