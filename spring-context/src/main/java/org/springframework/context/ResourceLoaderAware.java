package org.springframework.context;

import org.springframework.beans.factory.Aware;
import org.springframework.core.io.ResourceLoader;

public interface ResourceLoaderAware extends Aware {

    /**
     * Set the ResourceLoader that this object runs in.
     * <p>This might be a ResourcePatternResolver, which can be checked
     * through {@code instanceof ResourcePatternResolver}. See also the
     * {@code ResourcePatternUtils.getResourcePatternResolver} method.
     * <p>Invoked after population of normal bean properties but before an init callback
     * like InitializingBean's {@code afterPropertiesSet} or a custom init-method.
     * Invoked before ApplicationContextAware's {@code setApplicationContext}.
     *
     * @param resourceLoader ResourceLoader object to be used by this object
     * @see org.springframework.core.io.support.ResourcePatternResolver
     * @see org.springframework.core.io.support.ResourcePatternUtils#getResourcePatternResolver
     */
    void setResourceLoader(ResourceLoader resourceLoader);

}
