package org.springframework.beans.factory.parsing;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;

public interface ComponentDefinition extends BeanMetadataElement {
    /**
     * Get the user-visible name of this {@code ComponentDefinition}.
     * <p>This should link back directly to the corresponding configuration data
     * for this component in a given context.
     */
    String getName();

    /**
     * Return a friendly description of the described component.
     * <p>Implementations are encouraged to return the same value from
     * {@code toString()}.
     */
    String getDescription();

    /**
     * Return the {@link BeanDefinition BeanDefinitions} that were registered
     * to form this {@code ComponentDefinition}.
     * <p>It should be noted that a {@code ComponentDefinition} may well be related with
     * other {@link BeanDefinition BeanDefinitions} via {@link BeanReference references},
     * however these are <strong>not</strong> included as they may be not available immediately.
     * Important {@link BeanReference BeanReferences} are available from {@link #getBeanReferences()}.
     * @return the array of BeanDefinitions, or an empty array if none
     */
    BeanDefinition[] getBeanDefinitions();

    /**
     * Return the {@link BeanDefinition BeanDefinitions} that represent all relevant
     * inner beans within this component.
     * <p>Other inner beans may exist within the associated {@link BeanDefinition BeanDefinitions},
     * however these are not considered to be needed for validation or for user visualization.
     * @return the array of BeanDefinitions, or an empty array if none
     */
    BeanDefinition[] getInnerBeanDefinitions();

    /**
     * Return the set of {@link BeanReference BeanReferences} that are considered
     * to be important to this {@code ComponentDefinition}.
     * <p>Other {@link BeanReference BeanReferences} may exist within the associated
     * {@link BeanDefinition BeanDefinitions}, however these are not considered
     * to be needed for validation or for user visualization.
     * @return the array of BeanReferences, or an empty array if none
     */
    BeanReference[] getBeanReferences();
}
