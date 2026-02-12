package io.casehub.engine;

import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.GoalReachedEvent;
import io.casehub.engine.internal.event.MilestoneReachedEvent;
import io.casehub.engine.internal.event.StateContextChangedEvent;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

/**
 * Test utility bean that monitors event processing completion.
 * This allows tests to wait for async event handlers to finish.
 */
@ApplicationScoped
public class EventMonitor {

    private static final Logger LOG = Logger.getLogger(EventMonitor.class);

    private final AtomicInteger contextChangedEventCount = new AtomicInteger(0);
    private final AtomicInteger milestoneReachedEventCount = new AtomicInteger(0);
    private final AtomicInteger goalReachedEventCount = new AtomicInteger(0);
    private final Map<String, Integer> reachedMilestones = new ConcurrentHashMap<>();
    private final Map<String, Integer> reachedGoals = new ConcurrentHashMap<>();
    private volatile boolean lastEventWasWorkerCompleted = false;

    @ConsumeEvent(value = EventBusAddresses.CONTEXT_CHANGED, blocking = false)
    public void onContextChanged(StateContextChangedEvent event) {
        int count = contextChangedEventCount.incrementAndGet();
        LOG.infof("[EventMonitor] StateContextChangedEvent received (count: %d, type: %s)",
                count, event.getChangeType());

        if ("WORKER_COMPLETED".equals(event.getChangeType())) {
            lastEventWasWorkerCompleted = true;
            LOG.infof("[EventMonitor] WORKER_COMPLETED event detected");
        }
    }

    @ConsumeEvent(value = EventBusAddresses.MILESTONE_REACHED, blocking = false)
    public void onMilestoneReached(MilestoneReachedEvent event) {
        int count = milestoneReachedEventCount.incrementAndGet();
        String milestoneName = event.milestone().getName();

        reachedMilestones.merge(milestoneName, 1, Integer::sum);

        LOG.infof("[EventMonitor] MilestoneReachedEvent received (count: %d, milestone: %s)",
                count, milestoneName);
    }

    @ConsumeEvent(value = EventBusAddresses.GOAL_REACHED, blocking = false)
    public void onGoalReached(GoalReachedEvent event) {
        int count = goalReachedEventCount.incrementAndGet();
        String goalName = event.goal().getName();

        reachedGoals.merge(goalName, 1, Integer::sum);

        LOG.infof("[EventMonitor] GoalReachedEvent received (count: %d, goal: %s)",
                count, goalName);
    }

    public int getContextChangedEventCount() {
        return contextChangedEventCount.get();
    }

    public int getMilestoneReachedEventCount() {
        return milestoneReachedEventCount.get();
    }

    public boolean isMilestoneReached(String milestoneName) {
        return reachedMilestones.containsKey(milestoneName);
    }

    public int getMilestoneReachedCount(String milestoneName) {
        return reachedMilestones.getOrDefault(milestoneName, 0);
    }

    public int getGoalReachedEventCount() {
        return goalReachedEventCount.get();
    }

    public boolean isGoalReached(String goalName) {
        return reachedGoals.containsKey(goalName);
    }

    public int getGoalReachedCount(String goalName) {
        return reachedGoals.getOrDefault(goalName, 0);
    }

    public boolean isWorkerCompleted() {
        return lastEventWasWorkerCompleted;
    }

    public void reset() {
        contextChangedEventCount.set(0);
        milestoneReachedEventCount.set(0);
        goalReachedEventCount.set(0);
        reachedMilestones.clear();
        reachedGoals.clear();
        lastEventWasWorkerCompleted = false;
        LOG.infof("[EventMonitor] Reset");
    }
}
