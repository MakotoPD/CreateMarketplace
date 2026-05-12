package pl.makoto.createmarketplace.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Extracted testable logic for the reflection caching pattern used by XaeroCompat.
 * <p>
 * This class encapsulates the idempotent initialization pattern:
 * - An {@code initialized} flag prevents re-execution of lookups
 * - An {@code available} flag indicates whether lookups succeeded
 * - Class.forName(), getConstructor(), and getMethod() are performed at most once
 * <p>
 * The XaeroCompat class delegates to this pattern for its static reflection caching.
 */
public class ReflectionCacheLogic {

    private boolean initialized = false;
    private boolean available = false;
    private Class<?> cachedClass;
    private Constructor<?> cachedConstructor;
    private Method cachedMethod;
    private final AtomicInteger lookupCount = new AtomicInteger(0);

    private final String className;
    private final String methodName;
    private final Class<?>[] constructorParamTypes;
    private final Class<?>[] methodParamTypes;

    /**
     * Creates a ReflectionCacheLogic instance targeting specific class/method lookups.
     *
     * @param className           fully qualified class name to look up
     * @param constructorParamTypes parameter types for the constructor lookup
     * @param methodName          method name to look up
     * @param methodParamTypes    parameter types for the method lookup
     */
    public ReflectionCacheLogic(String className, Class<?>[] constructorParamTypes,
                                String methodName, Class<?>[] methodParamTypes) {
        this.className = className;
        this.constructorParamTypes = constructorParamTypes;
        this.methodName = methodName;
        this.methodParamTypes = methodParamTypes;
    }

    /**
     * Performs the reflection lookups at most once. Subsequent calls return immediately
     * due to the {@code initialized} guard.
     * <p>
     * This mirrors the pattern in XaeroCompat.initReflection().
     */
    public synchronized void initReflection() {
        if (initialized) return;
        initialized = true;
        lookupCount.incrementAndGet();
        try {
            cachedClass = Class.forName(className);
            cachedConstructor = cachedClass.getConstructor(constructorParamTypes);
            cachedMethod = cachedClass.getMethod(methodName, methodParamTypes);
            available = true;
        } catch (Exception e) {
            available = false;
        }
    }

    /**
     * Returns whether initialization has been performed.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Returns whether the reflection lookups succeeded.
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Returns the cached Class object, or null if not yet initialized or lookup failed.
     */
    public Class<?> getCachedClass() {
        return cachedClass;
    }

    /**
     * Returns the cached Constructor object, or null if not yet initialized or lookup failed.
     */
    public Constructor<?> getCachedConstructor() {
        return cachedConstructor;
    }

    /**
     * Returns the cached Method object, or null if not yet initialized or lookup failed.
     */
    public Method getCachedMethod() {
        return cachedMethod;
    }

    /**
     * Returns the number of times the actual lookup logic was executed.
     * This should always be 0 or 1 due to the idempotency guard.
     */
    public int getLookupCount() {
        return lookupCount.get();
    }

    /**
     * Resets the state for testing purposes.
     */
    public void reset() {
        initialized = false;
        available = false;
        cachedClass = null;
        cachedConstructor = null;
        cachedMethod = null;
        lookupCount.set(0);
    }
}
