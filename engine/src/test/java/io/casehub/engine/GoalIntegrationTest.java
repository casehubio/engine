package io.casehub.engine;

import io.casehub.engine.internal.context.StateContextImpl;
import io.casehub.model.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating goal evaluation and GoalReachedEvent publishing.
 * <p>
 * Scenario:
 * 1. CaseHub with multiple goals
 * 2. StateContext changes trigger goal condition evaluation
 * 3. GoalReachedEvent is published when conditions are met
 */
@QuarkusTest
public class GoalIntegrationTest {

    @Inject
    CaseHubRuntime runtime;

    @Inject
    EventMonitor eventMonitor;

    @BeforeEach
    public void setUp() {
        eventMonitor.reset();
    }

    @Test
    public void testGoalReachedWhenConditionMet() {
        // Create CaseHub with goals
        CaseHubDefinition caseDefinition = createCaseDefinitionWithGoals();

        // Start with initial context that does NOT match any goal
        StateContextImpl initialContext = new StateContextImpl();
        initialContext.set("taskId", "task-123");
        initialContext.set("status", "pending");
        initialContext.set("completedSteps", 0);

        UUID runId = runtime.submitCase(caseDefinition)
                .thenCompose(id -> runtime.startCase(initialContext, id)
                        .thenApply(v -> id))
                .toCompletableFuture()
                .join();

        assertNotNull(runId);

        // Wait for initial processing
        await()
                .atMost(Duration.ofSeconds(2))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    assertTrue(eventMonitor.getContextChangedEventCount() >= 1,
                            "Expected at least 1 StateContextChangedEvent");
                });

        // No goals should be reached yet
        assertEquals(0, eventMonitor.getGoalReachedEventCount(),
                "No goals should be reached with initial context");

        System.out.println("Initial state: no goals reached ✓");
        System.out.println("Goal reached count: " + eventMonitor.getGoalReachedEventCount());
    }

    @Test
    public void testMultipleGoalsReached() {
        CaseHubDefinition caseDefinition = createCaseDefinitionWithGoals();

        // Start with context that matches the "TaskInProgress" goal
        StateContextImpl initialContext = new StateContextImpl();
        initialContext.set("taskId", "task-456");
        initialContext.set("status", "in_progress");
        initialContext.set("completedSteps", 3);

        UUID runId = runtime.submitCase(caseDefinition)
                .thenCompose(id -> runtime.startCase(initialContext, id)
                        .thenApply(v -> id))
                .toCompletableFuture()
                .join();

        assertNotNull(runId);

        // Wait for goal events
        await()
                .atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    assertTrue(eventMonitor.getGoalReachedEventCount() >= 1,
                            "Expected at least 1 goal to be reached");
                });

        // Verify specific goal was reached
        assertTrue(eventMonitor.isGoalReached("TaskInProgress"),
                "TaskInProgress goal should be reached");

        System.out.println("Goals reached: " + eventMonitor.getGoalReachedEventCount());
        System.out.println("TaskInProgress reached: " +
                eventMonitor.isGoalReached("TaskInProgress"));
    }

    @Test
    public void testGoalWithComplexCondition() {
        CaseHubDefinition caseDefinition = createCaseDefinitionWithGoals();

        // Start with context that matches the "AllStepsCompleted" goal
        StateContextImpl initialContext = new StateContextImpl();
        initialContext.set("taskId", "task-789");
        initialContext.set("status", "completed");
        initialContext.set("completedSteps", 10);

        UUID runId = runtime.submitCase(caseDefinition)
                .thenCompose(id -> runtime.startCase(initialContext, id)
                        .thenApply(v -> id))
                .toCompletableFuture()
                .join();

        assertNotNull(runId);

        // Wait for goal events
        await()
                .atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    assertTrue(eventMonitor.getGoalReachedEventCount() >= 1,
                            "Expected at least 1 goal to be reached");
                });

        // Verify the complex goal was reached
        assertTrue(eventMonitor.isGoalReached("AllStepsCompleted"),
                "AllStepsCompleted goal should be reached");

        System.out.println("Goals reached: " + eventMonitor.getGoalReachedEventCount());
        System.out.println("AllStepsCompleted reached: " +
                eventMonitor.isGoalReached("AllStepsCompleted"));
    }

    @Test
    public void testMultipleGoalsReachedSimultaneously() {
        CaseHubDefinition caseDefinition = createCaseDefinitionWithGoals();

        // Start with context that matches BOTH "TaskReadyForReview" AND "AllStepsCompleted"
        StateContextImpl initialContext = new StateContextImpl();
        initialContext.set("taskId", "task-999");
        initialContext.set("status", "completed");
        initialContext.set("completedSteps", 15);

        UUID runId = runtime.submitCase(caseDefinition)
                .thenCompose(id -> runtime.startCase(initialContext, id)
                        .thenApply(v -> id))
                .toCompletableFuture()
                .join();

        assertNotNull(runId);

        // Wait for goal events
        await()
                .atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    assertTrue(eventMonitor.getGoalReachedEventCount() >= 2,
                            "Expected at least 2 goals to be reached");
                });

        // Verify both goals were reached
        assertTrue(eventMonitor.isGoalReached("AllStepsCompleted"),
                "AllStepsCompleted goal should be reached");
        assertTrue(eventMonitor.isGoalReached("TaskReadyForReview"),
                "TaskReadyForReview goal should be reached");

        System.out.println("Goals reached: " + eventMonitor.getGoalReachedEventCount());
        System.out.println("AllStepsCompleted reached: " +
                eventMonitor.isGoalReached("AllStepsCompleted"));
        System.out.println("TaskReadyForReview reached: " +
                eventMonitor.isGoalReached("TaskReadyForReview"));
    }

    /**
     * Creates a CaseHubDefinition with multiple goals that test different JQ conditions.
     */
    private CaseHubDefinition createCaseDefinitionWithGoals() {
        CaseHubDefinition definition = new CaseHubDefinition();
        definition.setDsl("0.1");
        definition.setVersion("1.0.0");
        definition.setName("Goal Test Case");
        definition.setNamespace("test");
        definition.setTitle("Test Case with Goals");

        CaseDefinitionSpec spec = new CaseDefinitionSpec();

        // Define multiple goals with different conditions

        // Goal 1: Simple equality check
        Goal goal1 = new Goal();
        goal1.setName("TaskInProgress");
        goal1.setDescription("Task is actively in progress");
        goal1.setCondition(".status == \"in_progress\"");

        // Goal 2: Complex condition with AND
        Goal goal2 = new Goal();
        goal2.setName("AllStepsCompleted");
        goal2.setDescription("All steps have been completed");
        goal2.setCondition(".status == \"completed\" and .completedSteps >= 10");

        // Goal 3: Condition with OR
        Goal goal3 = new Goal();
        goal3.setName("TaskReadyForReview");
        goal3.setDescription("Task is ready for review");
        goal3.setCondition(".status == \"completed\" or .status == \"pending_review\"");

        spec.setGoals(List.of(goal1, goal2, goal3));

        // Add empty workers, capabilities, rules, and milestones (not needed for goal tests)
        spec.setWorkers(List.of());
        spec.setCapabilities(List.of());
        spec.setRules(List.of());
        spec.setMilestones(List.of());

        definition.setSpec(spec);

        return definition;
    }
}
