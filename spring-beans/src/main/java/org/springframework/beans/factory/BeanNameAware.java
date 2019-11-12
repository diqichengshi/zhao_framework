package org.springframework.beans.factory;

public interface BeanNameAware extends Aware {

    /**
     * Set the name of the bean in the bean factory that created this bean.
     * <p>Invoked after population of normal bean properties but before an
     * init callback such as {@link InitializingBean#afterPropertiesSet()}
     * or a custom init-method.
     * @param name the name of the bean in the factory.
     * Note that this name is the actual bean name used in the factory, which may
     * differ from the originally specified name: in particular for inner bean
     * names, the actual bean name might have been made unique through appending
     * "#..." suffixes. Use the {@link BeanFactoryUtils#originalBeanName(String)}
     * method to extract the original bean name (without suffix), if desired.
     */
    void setBeanName(String name);

}
