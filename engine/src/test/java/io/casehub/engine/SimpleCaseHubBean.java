package io.casehub.engine;

import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseHubDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.DispatchRule;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.GoalKind;
import io.casehub.api.model.Milestone;
import io.casehub.api.model.Worker;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

import static io.serverlessworkflow.fluent.func.FuncWorkflowBuilder.workflow;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;


@ApplicationScoped
public class SimpleCaseHubBean extends CaseHub {

  @Override
  public CaseHubDefinition getDefinition() {

    Capability capability = Capability.builder()
            .name("processDocument")
            .inputSchema("{ documentId: .documentId, status: .status }")
            .outputSchema("{ processedDocument: ., status: .status }")
            .description("Process a document from the case context")
            .build();

    Goal goal = Goal.builder()
            .name("documentProcessingComplete")
            .condition(".status == \"processed\"")
            .kind(GoalKind.SUCCESS)
            .description("Goal achieved when document processing is complete")
            .build();

    return CaseHubDefinition.builder()
            .namespace("test")
            .name("Document Processing Test")
            .version("1.0.0")
            .title("Test Case with Worker and Capability")
            .capabilities(capability)
            .workers(Worker.builder()
                    .name("document-processor")
                    .capabilities(capability)
                    .function(workflow("step-function-export")
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
                            .build())
                    .description("Processes documents and updates case context")
                    .build())
            .rules(DispatchRule.builder()
                    .name("trigger-on-processing-status")
                    .capability(capability)
                    .on(new ContextChangeTrigger(".status == \"processing\""))
                    .build())
            .milestones(Milestone.builder()
                    .name("documentProcessed")
                    .condition(".status == \"processed\"")
                    .description("Milestone reached when document is processed")
                    .build())
            .goals(goal)
            .completion(GoalExpression.allOf(goal))
            .build();
  }
}
