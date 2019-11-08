package org.springframework.aop.aspectj.annotation;

import org.springframework.util.Assert;

public class LazySingletonAspectInstanceFactoryDecorator implements MetadataAwareAspectInstanceFactory {

    private final MetadataAwareAspectInstanceFactory maaif;

    private volatile Object materialized;


    /**
     * Create a new lazily initializing decorator for the given AspectInstanceFactory.
     * @param maaif the MetadataAwareAspectInstanceFactory to decorate
     */
    public LazySingletonAspectInstanceFactoryDecorator(MetadataAwareAspectInstanceFactory maaif) {
        Assert.notNull(maaif, "AspectInstanceFactory must not be null");
        this.maaif = maaif;
    }


    @Override
    public synchronized Object getAspectInstance() {
        if (this.materialized == null) {
            synchronized (this) {
                if (this.materialized == null) {
                    this.materialized = this.maaif.getAspectInstance();
                }
            }
        }
        return this.materialized;
    }

    public boolean isMaterialized() {
        return (this.materialized != null);
    }

    @Override
    public ClassLoader getAspectClassLoader() {
        return this.maaif.getAspectClassLoader();
    }

    @Override
    public AspectMetadata getAspectMetadata() {
        return this.maaif.getAspectMetadata();
    }

    @Override
    public int getOrder() {
        return this.maaif.getOrder();
    }


    @Override
    public String toString() {
        return "LazySingletonAspectInstanceFactoryDecorator: decorating " + this.maaif;
    }

}
