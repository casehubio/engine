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

import java.util.List;
import java.util.Map;

import static io.serverlessworkflow.fluent.func.FuncWorkflowBuilder.workflow;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;

@ApplicationScoped
public class MultiWorkerPipelineBean extends CaseHub {

  @Override
  public CaseHubDefinition getDefinition() {

    Capability validateCap = Capability.builder()
            .name("validateDocument")
            .inputSchema("{ documentId: .documentId, step: .step }")
            .outputSchema("{ valid: .valid, step: .step }")
            .description("Validate a received document")
            .build();

    Capability enrichCap = Capability.builder()
            .name("enrichDocument")
            .inputSchema("{ documentId: .documentId, valid: .valid }")
            .outputSchema("{ enrichedData: .enrichedData, step: .step }")
            .description("Enrich a validated document with metadata")
            .build();

    Capability publishCap = Capability.builder()
            .name("publishDocument")
            .inputSchema("{ documentId: .documentId, enrichedData: .enrichedData }")
            .outputSchema("{ publishedUrl: .publishedUrl, step: .step }")
            .description("Publish an enriched document")
            .build();

    Goal goal = Goal.builder()
            .name("pipelineComplete")
            .condition(".step == \"published\"")
            .kind(GoalKind.SUCCESS)
            .description("All pipeline steps completed successfully")
            .build();

    return CaseHubDefinition.builder()
            .namespace("test")
            .name("Multi-Worker Document Pipeline")
            .version("1.0.0")
            .title("Three-step document processing pipeline")
            .capabilities(validateCap, enrichCap, publishCap)
            .workers(Worker.builder()
                            .name("document-validator")
                            .capabilities(validateCap)
                            .function(workflow("validate-document")
                                    .tasks(
                                            function(s -> {
                                              Map<String, Object> ctx = (Map<String, Object>) s;
                                              if (!ctx.containsKey("documentId")) {
                                                throw new RuntimeException("Missing documentId");
                                              }
                                              return Map.of(
                                                      "valid", true,
                                                      "step", "validated"
                                              );
                                            }, Map.class))
                                    .build())
                            .description("Validates incoming documents")
                            .build(),
                    Worker.builder()
                            .name("document-enricher")
                            .capabilities(enrichCap)
                            .function(workflow("enrich-document")
                                    .tasks(
                                            function(s -> {
                                              Map<String, Object> ctx = (Map<String, Object>) s;
                                              if (!ctx.containsKey("documentId")) {
                                                throw new RuntimeException("Missing documentId");
                                              }
                                              return Map.of(
                                                      "enrichedData", Map.of(
                                                              "source", "internal",
                                                              "tags", List.of("validated", "enriched"),
                                                              "documentId", ctx.get("documentId")
                                                      ),
                                                      "step", "enriched"
                                              );
                                            }, Map.class))
                                    .build())
                            .description("Enriches validated documents with metadata")
                            .build(),
                    Worker.builder()
                            .name("document-publisher")
                            .capabilities(publishCap)
                            .function(workflow("publish-document")
                                    .tasks(
                                            function(s -> {
                                              Map<String, Object> ctx = (Map<String, Object>) s;
                                              if (!ctx.containsKey("documentId")) {
                                                throw new RuntimeException("Missing documentId");
                                              }
                                              return Map.of(
                                                      "publishedUrl", "https://docs.example.com/" + ctx.get("documentId"),
                                                      "step", "published"
                                              );
                                            }, Map.class))
                                    .build())
                            .description("Publishes enriched documents")
                            .build())
            .rules(DispatchRule.builder()
                            .name("trigger-on-received")
                            .capability(validateCap)
                            .on(new ContextChangeTrigger(".step == \"received\""))
                            .build(),
                    DispatchRule.builder()
                            .name("trigger-on-validated")
                            .capability(enrichCap)
                            .on(new ContextChangeTrigger(".step == \"validated\" and .valid == true"))
                            .build(),
                    DispatchRule.builder()
                            .name("trigger-on-enriched")
                            .capability(publishCap)
                            .on(new ContextChangeTrigger(".step == \"enriched\""))
                            .build())
            .milestones(Milestone.builder()
                            .name("documentValidated")
                            .condition(".step == \"validated\"")
                            .description("Document has been validated")
                            .build(),
                    Milestone.builder()
                            .name("documentEnriched")
                            .condition(".step == \"enriched\"")
                            .description("Document has been enriched")
                            .build(),
                    Milestone.builder()
                            .name("documentPublished")
                            .condition(".step == \"published\"")
                            .description("Document has been published")
                            .build())
            .goals(goal)
            .completion(GoalExpression.allOf(goal))
            .build();
  }
}
