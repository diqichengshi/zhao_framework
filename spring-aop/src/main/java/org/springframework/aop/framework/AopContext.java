package org.springframework.aop.framework;

import org.springframework.core.NamedThreadLocal;

public abstract class AopContext {
    /**
     * ThreadLocal holder for AOP proxy associated with this thread.
     * Will contain {@code null} unless the "exposeProxy" property on
     * the controlling proxy configuration has been set to "true".
     * @see ProxyConfig#setExposeProxy
     */
    private static final ThreadLocal<Object> currentProxy = new NamedThreadLocal<Object>("Current AOP proxy");


    /**
     * Try to return the current AOP proxy. This method is usable only if the
     * calling method has been invoked via AOP, and the AOP framework has been set
     * to expose proxies. Otherwise, this method will throw an IllegalStateException.
     * @return Object the current AOP proxy (never returns {@code null})
     * @throws IllegalStateException if the proxy cannot be found, because the
     * method was invoked outside an AOP invocation context, or because the
     * AOP framework has not been configured to expose the proxy
     */
    public static Object currentProxy() throws IllegalStateException {
        Object proxy = currentProxy.get();
        if (proxy == null) {
            throw new IllegalStateException(
                    "Cannot find current proxy: Set 'exposeProxy' property on Advised to 'true' to make it available.");
        }
        return proxy;
    }

    /**
     * Make the given proxy available via the {@code currentProxy()} method.
     * <p>Note that the caller should be careful to keep the old value as appropriate.
     * @param proxy the proxy to expose (or {@code null} to reset it)
     * @return the old proxy, which may be {@code null} if none was bound
     * @see #currentProxy()
     */
    static Object setCurrentProxy(Object proxy) {
        Object old = currentProxy.get();
        if (proxy != null) {
            currentProxy.set(proxy);
        }
        else {
            currentProxy.remove();
        }
        return old;
    }
}
