package pl.makoto.createmarketplace.client;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for debounced save coalescing.
 *
 * Property 7: Debounced save coalesces rapid toggles.
 *
 * For any sequence of N favorite toggles occurring within a 5-second window,
 * the FavoritesManager SHALL perform at most one disk write, and that write
 * SHALL occur no sooner than 5 seconds after the last toggle in the sequence.
 *
 * Validates: Requirements 9.1, 9.2
 */
class DebounceSaveCoalescingPropertyTest {

    /**
     * Property 7a: For any sequence of N rapid scheduleSave() calls within the debounce window,
     * at most one save execution occurs after the delay elapses.
     *
     * This verifies that rapid toggles are coalesced: no matter how many times scheduleSave()
     * is called in quick succession, only one disk write happens.
     *
     * Validates: Requirements 9.1, 9.2
     */
    @Property(tries = 20)
    void rapidTogglesCoalesceIntoAtMostOneSave(
            @ForAll @IntRange(min = 2, max = 50) int numberOfToggles
    ) throws InterruptedException {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Test-DebounceSave");
            t.setDaemon(true);
            return t;
        });

        try {
            AtomicInteger saveCount = new AtomicInteger(0);
            // Use a short delay (200ms) for testing — the property holds regardless of delay value
            DebounceSaveScheduler scheduler = new DebounceSaveScheduler(
                    executor, saveCount::incrementAndGet, 200, TimeUnit.MILLISECONDS
            );

            // Simulate N rapid toggles — all within the debounce window
            for (int i = 0; i < numberOfToggles; i++) {
                scheduler.scheduleSave();
                // Small delay between toggles (5ms) — all well within the 200ms debounce window
                Thread.sleep(5);
            }

            // At this point, no save should have executed yet (we're within the debounce window)
            assertEquals(0, saveCount.get(),
                    "No save should execute while toggles are still occurring within the debounce window");

            // Wait for the debounce delay to elapse after the last toggle
            Thread.sleep(350);

            // Exactly one save should have occurred
            assertEquals(1, saveCount.get(),
                    "Exactly one save should occur after the debounce delay elapses following the last toggle");
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    /**
     * Property 7b: Each scheduleSave() call cancels the previous pending save,
     * ensuring only one save is scheduled at any time.
     *
     * After a sequence of N scheduleSave() calls, there is exactly one pending save
     * (not N pending saves).
     *
     * Validates: Requirements 9.1, 9.2
     */
    @Property(tries = 20)
    void eachScheduleSaveCancelsPreviousPending(
            @ForAll @IntRange(min = 2, max = 100) int numberOfToggles
    ) throws InterruptedException {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Test-DebounceSave");
            t.setDaemon(true);
            return t;
        });

        try {
            AtomicInteger saveCount = new AtomicInteger(0);
            DebounceSaveScheduler scheduler = new DebounceSaveScheduler(
                    executor, saveCount::incrementAndGet, 300, TimeUnit.MILLISECONDS
            );

            // Fire N rapid scheduleSave() calls with no delay between them
            for (int i = 0; i < numberOfToggles; i++) {
                scheduler.scheduleSave();
            }

            // After all calls, there should be exactly one pending save
            assertTrue(scheduler.hasPendingSave(),
                    "After rapid scheduleSave() calls, there should be a pending save");

            // No save should have executed yet
            assertEquals(0, saveCount.get(),
                    "No save should have executed immediately after scheduling");

            // Wait for the delay to pass
            Thread.sleep(500);

            // Only one save should have executed (not N saves)
            assertEquals(1, saveCount.get(),
                    "Only one save should execute regardless of how many times scheduleSave() was called");
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    /**
     * Property 7c: flush() executes the save immediately and cancels any pending scheduled save.
     * After flush(), no additional save occurs from the previously scheduled one.
     *
     * Validates: Requirements 9.1, 9.2
     */
    @Property(tries = 20)
    void flushExecutesImmediatelyAndCancelsPending(
            @ForAll @IntRange(min = 1, max = 50) int numberOfToggles
    ) throws InterruptedException {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Test-DebounceSave");
            t.setDaemon(true);
            return t;
        });

        try {
            AtomicInteger saveCount = new AtomicInteger(0);
            DebounceSaveScheduler scheduler = new DebounceSaveScheduler(
                    executor, saveCount::incrementAndGet, 300, TimeUnit.MILLISECONDS
            );

            // Fire N rapid scheduleSave() calls
            for (int i = 0; i < numberOfToggles; i++) {
                scheduler.scheduleSave();
            }

            // No save yet
            assertEquals(0, saveCount.get(),
                    "No save should have executed before flush");

            // Flush immediately
            scheduler.flush();

            // Exactly one save should have executed (the flush)
            assertEquals(1, saveCount.get(),
                    "flush() should execute exactly one save immediately");

            // No pending save should remain
            assertFalse(scheduler.hasPendingSave(),
                    "After flush(), no pending save should remain");

            // Wait to ensure no additional save fires from the cancelled schedule
            Thread.sleep(500);

            // Still only one save (the flush), no additional from the cancelled pending
            assertEquals(1, saveCount.get(),
                    "No additional save should fire after flush() cancelled the pending one");
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }
}
