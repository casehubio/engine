package io.casehub.engine;

import io.casehub.api.CaseHub;
import io.casehub.api.model.AllOfGoalExpression;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseHubDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.DispatchRule;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalBasedCompletion;
import io.casehub.api.model.GoalKind;
import io.casehub.api.model.Milestone;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.worker.WorkflowFunction;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;


@ApplicationScoped
public class SimpleCaseHubBean extends CaseHub {

  @Override
  public CaseHubDefinition getDefinition() {
    CaseHubDefinition definition = new CaseHubDefinition("test", "Document Processing Test", "1.0.0");
    definition.setDsl("0.1");
    definition.setTitle("Test Case with Worker and Capability");

    Capability capability = new Capability("processDocument",
            "{ documentId: .documentId, status: .status }",
            "{ processedDocument: ., status: .status }");
    capability.setDescription("Process a document from the case context");
    definition.getCapabilities().add(capability);

    Workflow wf =
            FuncWorkflowBuilder.workflow("step-function-export")
                    .tasks(
                            function(s -> {
                              Map<String, Object> context = (Map<String, Object>) s;
                              if (!context.containsKey("documentId")) {
                                throw new RuntimeException("Missing documentId in context");
                              }
                              if (!context.containsKey("status")) {
                                throw new RuntimeException("Missing status in context");
                              }
                              return Map.of(
                                      "processedDocument", Map.of(
                                              "id", context.get("documentId"),
                                              "content", "Processed content for document " + context.get("documentId")
                                      ),
                                      "status", "processed"
                              );
                            }, Map.class))
                    .build();

    Worker worker = new Worker("document-processor", List.of(capability), new WorkflowFunction(wf));
    worker.setDescription("Processes documents and updates case context");
    definition.getWorkers().add(worker);

    ContextChangeTrigger contextChangeTrigger = new ContextChangeTrigger(
            new JQExpressionEvaluator(".status == \"processing\""));

    DispatchRule rule = new DispatchRule("trigger-on-processing-status", capability, contextChangeTrigger, null);
    definition.getRules().add(rule);

    Milestone milestone = new Milestone("documentProcessed",
            new JQExpressionEvaluator(".status == \"processed\""));
    milestone.setDescription("Milestone reached when document is processed");
    definition.getMilestones().add(milestone);

    Goal goal = new Goal("documentProcessingComplete",
            new JQExpressionEvaluator(".status == \"processed\""), GoalKind.SUCCESS);
    goal.setDescription("Goal achieved when document processing is complete");
    definition.getGoals().add(goal);

    GoalBasedCompletion completion = new GoalBasedCompletion(
            new AllOfGoalExpression(List.of(goal)), null);
    definition.setCompletion(completion);

    return definition;
  }
}
