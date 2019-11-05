package org.springframework.core;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.HashSet;
import java.util.Set;

public abstract class DecoratingClassLoader extends ClassLoader {

    /**
     * Java 7+ {@code ClassLoader.registerAsParallelCapable()} available?
     *
     * @since 4.1.2
     */
    protected static final boolean parallelCapableClassLoaderAvailable =
            ClassUtils.hasMethod(ClassLoader.class, "registerAsParallelCapable");

    static {
        if (parallelCapableClassLoaderAvailable) {
            ClassLoader.registerAsParallelCapable();
        }
    }


    private final Set<String> excludedPackages = new HashSet<String>();

    private final Set<String> excludedClasses = new HashSet<String>();

    private final Object exclusionMonitor = new Object();


    /**
     * Create a new DecoratingClassLoader with no parent ClassLoader.
     */
    public DecoratingClassLoader() {
    }

    /**
     * Create a new DecoratingClassLoader using the given parent ClassLoader
     * for delegation.
     */
    public DecoratingClassLoader(ClassLoader parent) {
        super(parent);
    }


    /**
     * Add a package name to exclude from decoration (e.g. overriding).
     * <p>Any class whose fully-qualified name starts with the name registered
     * here will be handled by the parent ClassLoader in the usual fashion.
     *
     * @param packageName the package name to exclude
     */
    public void excludePackage(String packageName) {
        Assert.notNull(packageName, "Package name must not be null");
        synchronized (this.exclusionMonitor) {
            this.excludedPackages.add(packageName);
        }
    }

    /**
     * Add a class name to exclude from decoration (e.g. overriding).
     * <p>Any class name registered here will be handled by the parent
     * ClassLoader in the usual fashion.
     *
     * @param className the class name to exclude
     */
    public void excludeClass(String className) {
        Assert.notNull(className, "Class name must not be null");
        synchronized (this.exclusionMonitor) {
            this.excludedClasses.add(className);
        }
    }

    /**
     * Determine whether the specified class is excluded from decoration
     * by this class loader.
     * <p>The default implementation checks against excluded packages and classes.
     *
     * @param className the class name to check
     * @return whether the specified class is eligible
     * @see #excludePackage
     * @see #excludeClass
     */
    protected boolean isExcluded(String className) {
        synchronized (this.exclusionMonitor) {
            if (this.excludedClasses.contains(className)) {
                return true;
            }
            for (String packageName : this.excludedPackages) {
                if (className.startsWith(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

}

