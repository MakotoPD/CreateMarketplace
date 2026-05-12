package pl.makoto.createmarketplace.client;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Extracted debounce scheduling logic from FavoritesManager for testability.
 *
 * This class encapsulates the debounce mechanism: each call to {@link #scheduleSave()}
 * cancels any pending save and schedules a new one after the configured delay.
 * {@link #flush()} cancels any pending save and executes the save action immediately.
 */
public class DebounceSaveScheduler {

    private final ScheduledExecutorService executor;
    private final Runnable saveAction;
    private final long delay;
    private final TimeUnit timeUnit;

    private ScheduledFuture<?> pendingSave = null;
    private int saveExecutionCount = 0;

    /**
     * Creates a new DebounceSaveScheduler with seconds as the time unit.
     *
     * @param executor     the scheduled executor to use for scheduling saves
     * @param saveAction   the action to execute when a save is triggered
     * @param delaySeconds the debounce delay in seconds
     */
    public DebounceSaveScheduler(ScheduledExecutorService executor, Runnable saveAction, long delaySeconds) {
        this(executor, saveAction, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * Creates a new DebounceSaveScheduler with a configurable time unit.
     *
     * @param executor   the scheduled executor to use for scheduling saves
     * @param saveAction the action to execute when a save is triggered
     * @param delay      the debounce delay
     * @param timeUnit   the time unit for the delay
     */
    public DebounceSaveScheduler(ScheduledExecutorService executor, Runnable saveAction, long delay, TimeUnit timeUnit) {
        this.executor = executor;
        this.saveAction = saveAction;
        this.delay = delay;
        this.timeUnit = timeUnit;
    }

    /**
     * Schedules a save operation. Cancels any previously pending save and schedules a new one
     * after the configured delay. This ensures that rapid calls are coalesced into a single save.
     */
    public synchronized void scheduleSave() {
        if (pendingSave != null && !pendingSave.isDone()) {
            pendingSave.cancel(false);
        }
        pendingSave = executor.schedule(this::executeSave, delay, timeUnit);
    }

    /**
     * Flushes any pending save immediately. Cancels the pending scheduled save and executes
     * the save action synchronously.
     */
    public synchronized void flush() {
        if (pendingSave != null && !pendingSave.isDone()) {
            pendingSave.cancel(false);
        }
        executeSave();
    }

    private void executeSave() {
        saveExecutionCount++;
        saveAction.run();
    }

    /**
     * Returns the number of times the save action has been executed.
     * Useful for testing to verify coalescing behavior.
     */
    public int getSaveExecutionCount() {
        return saveExecutionCount;
    }

    /**
     * Returns whether there is a pending (not yet executed) save scheduled.
     */
    public synchronized boolean hasPendingSave() {
        return pendingSave != null && !pendingSave.isDone();
    }
}
