package io.casehub.engine;

import io.casehub.api.CaseHub;
import io.casehub.model.Capability;
import io.casehub.model.CaseCompletion;
import io.casehub.model.CaseDefinitionSpec;
import io.casehub.model.CaseHubDefinition;
import io.casehub.model.ContextChangeTrigger;
import io.casehub.model.DispatchRule;
import io.casehub.model.Goal;
import io.casehub.model.GoalExpression;
import io.casehub.model.Milestone;
import io.casehub.model.Trigger;
import io.casehub.model.Worker;
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
    CaseHubDefinition definition = new CaseHubDefinition();
    definition.setDsl("0.1");
    definition.setVersion("1.0.0");
    definition.setName("Document Processing Test");
    definition.setNamespace("test");
    definition.setTitle("Test Case with Worker and Capability");

    CaseDefinitionSpec spec = new CaseDefinitionSpec();

    Capability capability = new Capability();
    capability.setName("processDocument");
    capability.setDescription("Process a document from the case context");
    capability.setInputSchema("{ documentId: .documentId, status: .status }");
    capability.setOutputSchema("{ processedDocument: ., status: .status }");
    spec.setCapabilities(List.of(capability));

    Worker worker = new Worker();
    worker.setName("document-processor");
    worker.setDescription("Processes documents and updates case context");
    worker.setCapabilities(List.of("processDocument"));

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

    worker.setWorkflow(wf);

    spec.setWorkers(List.of(worker));

    DispatchRule rule = new DispatchRule();
    rule.setName("trigger-on-processing-status");
    rule.setCapability("processDocument");

    Trigger trigger = new Trigger();
    ContextChangeTrigger contextChangeTrigger = new ContextChangeTrigger();
    contextChangeTrigger.setFilter(".status == \"processing\"");
    trigger.setContextChange(contextChangeTrigger);
    rule.setOn(trigger);

    spec.setRules(List.of(rule));

    Milestone milestone = new Milestone();
    milestone.setName("documentProcessed");
    milestone.setDescription("Milestone reached when document is processed");
    milestone.setCondition(".status == \"processed\"");
    spec.setMilestones(List.of(milestone));

    Goal goal = new Goal();
    goal.setName("documentProcessingComplete");
    goal.setDescription("Goal achieved when document processing is complete");
    goal.setCondition(".status == \"processed\"");
    spec.setGoals(List.of(goal));

    CaseCompletion caseCompletion = new CaseCompletion();
    GoalExpression completionCondition = new GoalExpression();
    completionCondition.setAllOf(List.of("documentProcessingComplete"));
    caseCompletion.setSuccess(completionCondition);

    spec.setCompletion(caseCompletion);
    definition.setSpec(spec);

    return definition;
  }
}
