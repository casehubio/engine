package io.casehub.engine;

import io.casehub.engine.internal.context.StateContextImpl;
import io.casehub.model.*;
import io.quarkus.test.junit.QuarkusTest;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating milestone evaluation and MilestoneReachedEvent publishing.
 * <p>
 * Scenario:
 * 1. CaseHub with multiple milestones
 * 2. StateContext changes trigger milestone condition evaluation
 * 3. MilestoneReachedEvent is published when conditions are met
 */
@QuarkusTest
public class MilestoneIntegrationTest {

    @Inject
    CaseHubRuntime runtime;

    @Inject
    EventMonitor eventMonitor;

    @BeforeEach
    public void setUp() {
        eventMonitor.reset();
    }

    @Test
    public void testMilestoneReachedWhenConditionMet() {
        // Create CaseHub with milestones
        CaseHubDefinition caseDefinition = createCaseDefinitionWithMilestones();

        // Start with initial context that does NOT match any milestone
        StateContextImpl initialContext = new StateContextImpl();
        initialContext.set("documentId", "doc-123");
        initialContext.set("status", "pending");
        initialContext.set("processedCount", 0);

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

        // No milestones should be reached yet
        assertEquals(0, eventMonitor.getMilestoneReachedEventCount(),
                "No milestones should be reached with initial context");

        System.out.println("Initial state: no milestones reached ✓");
        System.out.println("Milestone reached count: " + eventMonitor.getMilestoneReachedEventCount());
    }

    @Test
    public void testMultipleMilestonesReached() {
        CaseHubDefinition caseDefinition = createCaseDefinitionWithMilestones();

        // Start with context that matches the "DocumentProcessingStarted" milestone
        StateContextImpl initialContext = new StateContextImpl();
        initialContext.set("documentId", "doc-456");
        initialContext.set("status", "processing");
        initialContext.set("processedCount", 1);

        UUID runId = runtime.submitCase(caseDefinition)
                .thenCompose(id -> runtime.startCase(initialContext, id)
                        .thenApply(v -> id))
                .toCompletableFuture()
                .join();

        assertNotNull(runId);

        // Wait for milestone events
        await()
                .atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    assertTrue(eventMonitor.getMilestoneReachedEventCount() >= 1,
                            "Expected at least 1 milestone to be reached");
                });

        // Verify specific milestone was reached
        assertTrue(eventMonitor.isMilestoneReached("DocumentProcessingStarted"),
                "DocumentProcessingStarted milestone should be reached");

        System.out.println("Milestones reached: " + eventMonitor.getMilestoneReachedEventCount());
        System.out.println("DocumentProcessingStarted reached: " +
                eventMonitor.isMilestoneReached("DocumentProcessingStarted"));
    }

    @Test
    public void testMilestoneWithComplexCondition() {
        CaseHubDefinition caseDefinition = createCaseDefinitionWithMilestones();

        // Start with context that matches the "AllDocumentsProcessed" milestone
        StateContextImpl initialContext = new StateContextImpl();
        initialContext.set("documentId", "doc-789");
        initialContext.set("status", "completed");
        initialContext.set("processedCount", 10);

        UUID runId = runtime.submitCase(caseDefinition)
                .thenCompose(id -> runtime.startCase(initialContext, id)
                        .thenApply(v -> id))
                .toCompletableFuture()
                .join();

        assertNotNull(runId);

        // Wait for milestone events
        await()
                .atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    assertTrue(eventMonitor.getMilestoneReachedEventCount() >= 1,
                            "Expected at least 1 milestone to be reached");
                });

        // Verify the complex milestone was reached
        assertTrue(eventMonitor.isMilestoneReached("AllDocumentsProcessed"),
                "AllDocumentsProcessed milestone should be reached");

        System.out.println("Milestones reached: " + eventMonitor.getMilestoneReachedEventCount());
        System.out.println("AllDocumentsProcessed reached: " +
                eventMonitor.isMilestoneReached("AllDocumentsProcessed"));
    }

    /**
     * Creates a CaseHubDefinition with multiple milestones that test different JQ conditions.
     */
    private CaseHubDefinition createCaseDefinitionWithMilestones() {
        CaseHubDefinition definition = new CaseHubDefinition();
        definition.setDsl("0.1");
        definition.setVersion("1.0.0");
        definition.setName("Milestone Test Case");
        definition.setNamespace("test");
        definition.setTitle("Test Case with Milestones");

        CaseDefinitionSpec spec = new CaseDefinitionSpec();

        // Define multiple milestones with different conditions

        // Milestone 1: Simple equality check
        Milestone milestone1 = new Milestone();
        milestone1.setName("DocumentProcessingStarted");
        milestone1.setDescription("Document processing has started");
        milestone1.setCondition(".status == \"processing\"");

        // Milestone 2: Complex condition with AND
        Milestone milestone2 = new Milestone();
        milestone2.setName("AllDocumentsProcessed");
        milestone2.setDescription("All documents have been processed");
        milestone2.setCondition(".status == \"completed\" and .processedCount >= 10");

        // Milestone 3: Condition with OR
        Milestone milestone3 = new Milestone();
        milestone3.setName("ReadyForReview");
        milestone3.setDescription("Document is ready for review");
        milestone3.setCondition(".status == \"completed\" or .status == \"pending_review\"");

        spec.setMilestones(List.of(milestone1, milestone2, milestone3));

        // Add empty workers and capabilities (not needed for milestone tests)
        spec.setWorkers(List.of());
        spec.setCapabilities(List.of());
        spec.setRules(List.of());
        spec.setGoals(List.of());

        definition.setSpec(spec);

        return definition;
    }
}
