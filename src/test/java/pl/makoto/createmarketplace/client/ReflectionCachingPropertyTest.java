package pl.makoto.createmarketplace.client;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for reflection caching idempotency.
 *
 * Property 8: Reflection caching is idempotent.
 *
 * For any number of sequential calls to initReflection(), the Class.forName() and
 * getConstructor()/getMethod() lookups are performed at most once. The initialized flag
 * prevents re-execution.
 *
 * Validates: Requirements 10.1, 10.2, 10.3
 */
class ReflectionCachingPropertyTest {

    // Use a well-known JDK class for testing so lookups succeed without external dependencies
    private static final String TEST_CLASS_NAME = "java.lang.String";
    private static final String TEST_METHOD_NAME = "valueOf";
    private static final Class<?>[] CONSTRUCTOR_PARAMS = new Class<?>[]{ String.class };
    private static final Class<?>[] METHOD_PARAMS = new Class<?>[]{ Object.class };

    private ReflectionCacheLogic cache;

    @BeforeProperty
    void setUp() {
        cache = new ReflectionCacheLogic(TEST_CLASS_NAME, CONSTRUCTOR_PARAMS, TEST_METHOD_NAME, METHOD_PARAMS);
    }

    /**
     * Property 8a: For any number of sequential calls to initReflection(),
     * the lookup logic is executed at most once.
     *
     * Validates: Requirements 10.1, 10.2, 10.3
     */
    @Property
    void reflectionLookupsPerformedAtMostOnce(@ForAll("callCounts") int numberOfCalls) {
        for (int i = 0; i < numberOfCalls; i++) {
            cache.initReflection();
        }

        // The lookup logic should have been executed exactly once (at most once)
        assertEquals(1, cache.getLookupCount(),
                "Reflection lookups must be performed exactly once regardless of call count");
    }

    /**
     * Property 8b: For any number of sequential calls, the initialized flag
     * remains true after the first call and prevents re-execution.
     *
     * Validates: Requirements 10.1, 10.2, 10.3
     */
    @Property
    void initializedFlagPreventsReExecution(@ForAll("callCounts") int numberOfCalls) {
        for (int i = 0; i < numberOfCalls; i++) {
            cache.initReflection();

            // After every call, initialized must be true
            assertTrue(cache.isInitialized(),
                    "initialized flag must be true after any call to initReflection()");
        }

        // Lookup count must still be exactly 1
        assertEquals(1, cache.getLookupCount(),
                "The initialized flag must prevent re-execution of lookups");
    }

    /**
     * Property 8c: For any number of sequential calls, the cached objects remain
     * the same reference — they are not re-created on subsequent calls.
     *
     * Validates: Requirements 10.1, 10.2, 10.3
     */
    @Property
    void cachedObjectsRemainConsistentAcrossCalls(@ForAll("callCounts") int numberOfCalls) {
        // First call to establish cached objects
        cache.initReflection();

        Class<?> firstClass = cache.getCachedClass();
        Constructor<?> firstConstructor = cache.getCachedConstructor();
        Method firstMethod = cache.getCachedMethod();

        assertNotNull(firstClass, "Cached class should not be null after successful init");
        assertNotNull(firstConstructor, "Cached constructor should not be null after successful init");
        assertNotNull(firstMethod, "Cached method should not be null after successful init");

        // Subsequent calls should not change the cached references
        for (int i = 1; i < numberOfCalls; i++) {
            cache.initReflection();

            assertSame(firstClass, cache.getCachedClass(),
                    "Cached Class object must be the same reference on call " + (i + 1));
            assertSame(firstConstructor, cache.getCachedConstructor(),
                    "Cached Constructor object must be the same reference on call " + (i + 1));
            assertSame(firstMethod, cache.getCachedMethod(),
                    "Cached Method object must be the same reference on call " + (i + 1));
        }
    }

    /**
     * Property 8d: When the target class does not exist, the available flag is false
     * and remains consistently false across multiple calls — lookups are still only attempted once.
     *
     * Validates: Requirements 10.1, 10.2, 10.3
     */
    @Property
    void failedLookupRemainsConsistentAcrossCalls(@ForAll("callCounts") int numberOfCalls) {
        // Use a non-existent class name to simulate Xaero not being present
        ReflectionCacheLogic failingCache = new ReflectionCacheLogic(
                "com.nonexistent.FakeClass",
                new Class<?>[]{},
                "fakeMethod",
                new Class<?>[]{}
        );

        for (int i = 0; i < numberOfCalls; i++) {
            failingCache.initReflection();
        }

        assertTrue(failingCache.isInitialized(),
                "initialized must be true even when lookup fails");
        assertFalse(failingCache.isAvailable(),
                "available must be false when class is not found");
        assertEquals(1, failingCache.getLookupCount(),
                "Lookup must be attempted exactly once even on failure");
        assertNull(failingCache.getCachedClass(),
                "Cached class must be null when lookup failed");
        assertNull(failingCache.getCachedConstructor(),
                "Cached constructor must be null when lookup failed");
        assertNull(failingCache.getCachedMethod(),
                "Cached method must be null when lookup failed");
    }

    // --- Generators ---

    @Provide
    Arbitrary<Integer> callCounts() {
        // Generate a number of calls between 1 and 100
        return Arbitraries.integers().between(1, 100);
    }
}
