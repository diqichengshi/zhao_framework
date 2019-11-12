package org.springframework.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.env.EnvironmentCapable;

public interface ApplicationContext extends EnvironmentCapable,ApplicationEventPublisher, ListableBeanFactory {
    /**
     * Return the unique id of this application context.
     * @return the unique id of the context, or {@code null} if none
     */
    String getId();

}
