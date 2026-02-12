package io.casehub.engine.internal.model;

import io.casehub.context.StateContext;

import java.util.UUID;

/**
 * Represents the current runtime state of a CaseHub instance execution.
 *
 * This object combines:
 * - The persistent CaseHubInstanceRun entity (metadata, status, timestamps)
 * - The in-memory StateContext (shared observable state)
 *
 * Purpose:
 * - Provides a convenient way to pass the complete execution state in events
 * - Avoids repeated lookups of StateContext and CaseHubInstanceRun in event handlers
 * - Encapsulates the relationship between persistent metadata and runtime state
 *
 * Usage in events:
 * Instead of passing only runId and forcing each handler to:
 * 1. Load CaseHubInstanceRun from repository
 * 2. Get StateContext from StateContextService
 *
 * We pass CaseHubInstanceRunState that already contains both.
 *
 * Thread Safety:
 * - This object is IMMUTABLE (final fields)
 * - StateContext is thread-safe internally (uses ReadWriteLock)
 * - Multiple event handlers can safely access the same CaseHubInstanceRunState
 *   from different threads
 * - CaseHubInstanceRun MUST be eagerly loaded (JOIN FETCH) before creating
 *   this object to avoid LazyInitializationException in multi-threaded access
 *
 * IMPORTANT: CaseHubInstanceRun must have CaseHub eagerly loaded via JOIN FETCH,
 * otherwise accessing getCaseHub() from a different thread will fail.
 */
public class CaseHubInstanceRunState {

    private final CaseHubInstanceRun run;
    private final StateContext stateContext;

    /**
     * Creates a CaseHubInstanceRunState.
     *
     * @param run the CaseHubInstanceRun entity (MUST have CaseHub eagerly loaded via JOIN FETCH)
     * @param stateContext the StateContext for this run (thread-safe)
     * @throws IllegalArgumentException if run or stateContext is null
     * @throws IllegalStateException if CaseHub is not loaded (lazy initialization check)
     */
    public CaseHubInstanceRunState(CaseHubInstanceRun run, StateContext stateContext) {
        this.run = run;
        this.stateContext = stateContext;
    }

    /**
     * Returns the persistent CaseHubInstanceRun entity with metadata.
     *
     * Thread-safe: CaseHubInstanceRun is immutable after creation in this context.
     * Reading fields is safe from multiple threads.
     *
     * @return The persistent CaseHubInstanceRun entity with metadata
     */
    public CaseHubInstanceRun getRun() {
        return run;
    }

    /**
     * Returns the in-memory StateContext with current case state.
     *
     * Thread-safe: StateContext implementation (StateContextImpl) uses ReadWriteLock
     * for all operations. Multiple threads can safely call StateContext methods
     * concurrently - reads are concurrent, writes are exclusive.
     *
     * Event handlers running on different EventLoop threads can safely:
     * - Read from the same StateContext concurrently
     * - Write to StateContext (writes are serialized by internal lock)
     * - Use atomic operations like compareAndSet, putIfAbsent
     *
     * @return The in-memory StateContext with current case state (thread-safe)
     */
    public StateContext getStateContext() {
        return stateContext;
    }

    /**
     * Convenience method to get the run ID
     */
    public UUID getRunId() {
        return run.getRunId();
    }

    /**
     * Convenience method to get the CaseHub definition
     */
    public CaseHub getCaseHub() {
        return run.getCaseHub();
    }

    /**
     * Convenience method to get the current run status
     */
    public CaseHubInstanceRun.CaseHubStatus getStatus() {
        return run.getStatus();
    }

    @Override
    public String toString() {
        return "CaseHubInstanceRunState{" +
                "runId=" + getRunId() +
                ", status=" + getStatus() +
                ", stateContextSize=" + stateContext.size() +
                '}';
    }
}
