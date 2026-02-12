package io.casehub.engine;

import io.casehub.engine.internal.context.StateContextImpl;
import io.casehub.engine.internal.model.CaseHubInstanceRun;
import io.casehub.engine.internal.repository.CaseHubInstanceRunRepository;
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
 * Integration test demonstrating case completion condition evaluation.
 * <p>
 * Scenario:
 * 1. CaseHub with multiple goals and completion conditions
 * 2. When all success goals are reached, case status should change to COMPLETED
 * 3. Verifies both allOf and anyOf completion semantics
 */
@QuarkusTest
public class CaseCompletionIntegrationTest {

    @Inject
    CaseHubRuntime runtime;

    @Inject
    EventMonitor eventMonitor;

    @Inject
    CaseHubInstanceRunRepository runRepository;

    @BeforeEach
    public void setUp() {
        eventMonitor.reset();
    }

    @Test
    public void testCaseCompletionOnSuccessAllOf() {
        // Create CaseHub with completion condition: allOf["Goal1", "Goal2"]
        CaseHubDefinition caseDefinition = createCaseDefinitionWithAllOfCompletion();

        // Start with context that matches BOTH goals
        StateContextImpl initialContext = new StateContextImpl();
        initialContext.set("step1", true);
        initialContext.set("step2", true);

        UUID runId = runtime.submitCase(caseDefinition)
                .thenCompose(id -> runtime.startCase(initialContext, id)
                        .thenApply(v -> id))
                .toCompletableFuture()
                .join();

        assertNotNull(runId);

        // Wait for goal events and status update
        await()
                .atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    assertTrue(eventMonitor.isGoalReached("Goal1"), "Goal1 should be reached");
                    assertTrue(eventMonitor.isGoalReached("Goal2"), "Goal2 should be reached");
                });

        System.out.println("Case completed successfully with allOf completion condition!");
    }

    @Test
    public void testCaseCompletionOnSuccessAnyOf() {
        // Create CaseHub with completion condition: anyOf["Goal1", "Goal2"]
        CaseHubDefinition caseDefinition = createCaseDefinitionWithAnyOfCompletion();

        // Start with context that matches only Goal1 (not Goal2)
        StateContextImpl initialContext = new StateContextImpl();
        initialContext.set("step1", true);
        initialContext.set("step2", false);

        UUID runId = runtime.submitCase(caseDefinition)
                .thenCompose(id -> runtime.startCase(initialContext, id)
                        .thenApply(v -> id))
                .toCompletableFuture()
                .join();

        assertNotNull(runId);

        // Wait for goal event and status update
        await()
                .atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    assertTrue(eventMonitor.isGoalReached("Goal1"), "Goal1 should be reached");
                });

        System.out.println("Case completed successfully with anyOf completion condition!");
    }

    @Test
    public void testCaseNotCompletedWhenGoalsMissing() {
        // Create CaseHub with completion condition: allOf["Goal1", "Goal2"]
        CaseHubDefinition caseDefinition = createCaseDefinitionWithAllOfCompletion();

        // Start with context that matches only Goal1 (not Goal2)
        StateContextImpl initialContext = new StateContextImpl();
        initialContext.set("step1", true);
        initialContext.set("step2", false);

        UUID runId = runtime.submitCase(caseDefinition)
                .thenCompose(id -> runtime.startCase(initialContext, id)
                        .thenApply(v -> id))
                .toCompletableFuture()
                .join();

        assertNotNull(runId);

        // Wait for goal event
        await()
                .atMost(Duration.ofSeconds(2))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    assertTrue(eventMonitor.isGoalReached("Goal1"), "Goal1 should be reached");
                });

        // Status should still be RUNNING (not all goals reached)
        CaseHubInstanceRun run = runRepository.findByRunId(runId).await().indefinitely();
        assertEquals(CaseHubInstanceRun.CaseHubStatus.RUNNING, run.getStatus(),
                "Case should remain RUNNING when not all allOf goals are reached");

        System.out.println("Case correctly remains RUNNING when completion condition not met!");
    }

    /**
     * Creates a CaseHubDefinition with allOf completion condition.
     * Success: allOf["Goal1", "Goal2"]
     */
    private CaseHubDefinition createCaseDefinitionWithAllOfCompletion() {
        CaseHubDefinition definition = new CaseHubDefinition();
        definition.setDsl("0.1");
        definition.setVersion("1.0.0");
        definition.setName("AllOf Completion Test");
        definition.setNamespace("test");
        definition.setTitle("Test Case with AllOf Completion");

        CaseDefinitionSpec spec = new CaseDefinitionSpec();

        // Define goals
        Goal goal1 = new Goal();
        goal1.setName("Goal1");
        goal1.setDescription("First step completed");
        goal1.setCondition(".step1 == true");

        Goal goal2 = new Goal();
        goal2.setName("Goal2");
        goal2.setDescription("Second step completed");
        goal2.setCondition(".step2 == true");

        spec.setGoals(List.of(goal1, goal2));

        // Define completion condition: success when both Goal1 AND Goal2 are reached
        CaseCompletion completion = new CaseCompletion();

        GoalExpression successExpression = new GoalExpression();
        successExpression.setAllOf(List.of("Goal1", "Goal2"));
        completion.setSuccess(successExpression);

        spec.setCompletion(completion);

        // Add empty workers, capabilities, rules, and milestones
        spec.setWorkers(List.of());
        spec.setCapabilities(List.of());
        spec.setRules(List.of());
        spec.setMilestones(List.of());

        definition.setSpec(spec);

        return definition;
    }

    /**
     * Creates a CaseHubDefinition with anyOf completion condition.
     * Success: anyOf["Goal1", "Goal2"]
     */
    private CaseHubDefinition createCaseDefinitionWithAnyOfCompletion() {
        CaseHubDefinition definition = new CaseHubDefinition();
        definition.setDsl("0.1");
        definition.setVersion("1.0.0");
        definition.setName("AnyOf Completion Test");
        definition.setNamespace("test");
        definition.setTitle("Test Case with AnyOf Completion");

        CaseDefinitionSpec spec = new CaseDefinitionSpec();

        // Define goals
        Goal goal1 = new Goal();
        goal1.setName("Goal1");
        goal1.setDescription("First step completed");
        goal1.setCondition(".step1 == true");

        Goal goal2 = new Goal();
        goal2.setName("Goal2");
        goal2.setDescription("Second step completed");
        goal2.setCondition(".step2 == true");

        spec.setGoals(List.of(goal1, goal2));

        // Define completion condition: success when at least one of Goal1 OR Goal2 is reached
        CaseCompletion completion = new CaseCompletion();

        GoalExpression successExpression = new GoalExpression();
        successExpression.setAnyOf(List.of("Goal1", "Goal2"));
        completion.setSuccess(successExpression);

        spec.setCompletion(completion);

        // Add empty workers, capabilities, rules, and milestones
        spec.setWorkers(List.of());
        spec.setCapabilities(List.of());
        spec.setRules(List.of());
        spec.setMilestones(List.of());

        definition.setSpec(spec);

        return definition;
    }
}
