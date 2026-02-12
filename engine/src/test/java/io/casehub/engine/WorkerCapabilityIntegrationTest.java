package io.casehub.engine;

import io.casehub.model.*;
import io.quarkus.test.junit.QuarkusTest;
import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.CallTaskJava;
import io.serverlessworkflow.fluent.func.FuncSetTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.fluent.func.dsl.FuncDSL;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.consume;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test demonstrating:
 * 1. CaseHubDefinition with one Capability
 * 2. One Worker implementing that Capability
 * 3. One DispatchRule with ContextChangeTrigger that reacts to StateContext changes
 */
@QuarkusTest
public class WorkerCapabilityIntegrationTest {

    @Inject
    SimpleCaseHubBean simpleCaseHubBean;

    @Inject
    EventMonitor eventMonitor;

    @BeforeEach
    public void setUp() {
        eventMonitor.reset();
    }

    @Test
    public void testWorkerTriggeredByContextChange() {
        // Start with status = "processing" to match the rule filter
        Map<String, Object> initialContext = Map.of(
                "documentId", "doc-123",
                "status", "processing"
        );

        UUID uuid = simpleCaseHubBean.startCase(initialContext).toCompletableFuture().join();

        // Wait for initial event processing
        // Expected: INITIALIZED event should trigger the rule because status == "processing"
        await()
                .atMost(Duration.ofSeconds(2))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    int eventCount = eventMonitor.getContextChangedEventCount();
                    assertTrue(eventCount >= 1,
                            "Expected at least 1 StateContextChangedEvent, but got: " + eventCount);
                });

        // Store the count after initial processing
        int initialCount = eventMonitor.getContextChangedEventCount();
        System.out.println("Initial StateContextChangedEvent count: " + initialCount);

        // Wait a bit longer to ensure no infinite loop occurred
        // After worker completes, it changes status to "processed",
        // which should NOT match the rule (.status == "processing"),
        // so no further worker executions should occur
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        int finalCount = eventMonitor.getContextChangedEventCount();
        System.out.println("Final StateContextChangedEvent count: " + finalCount);

        // Verify the event count is stable (no infinite loop)
        // We expect:
        // - 1 INITIALIZED event (triggers worker because status == "processing")
        // - 1 WORKER_COMPLETED event (ignored by StateContextChangedEventHandler)
        // Total: 2 events
        assertTrue(finalCount <= 2,
                "Event count should be stable (no infinite loop), but got: " + finalCount);

        System.out.println("All async events processed successfully, no infinite loop detected");
    }

}
