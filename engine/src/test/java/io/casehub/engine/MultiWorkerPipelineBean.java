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
public class MultiWorkerPipelineBean extends CaseHub {

  @Override
  public CaseHubDefinition getDefinition() {
    CaseHubDefinition definition = new CaseHubDefinition("test", "Multi-Worker Document Pipeline", "1.0.0");
    definition.setDsl("0.1");
    definition.setTitle("Three-step document processing pipeline");

    // --- Capabilities ---

    Capability validateCap = new Capability("validateDocument",
            "{ documentId: .documentId, step: .step }",
            "{ valid: .valid, step: .step }");
    validateCap.setDescription("Validate a received document");

    Capability enrichCap = new Capability("enrichDocument",
            "{ documentId: .documentId, valid: .valid }",
            "{ enrichedData: .enrichedData, step: .step }");
    enrichCap.setDescription("Enrich a validated document with metadata");

    Capability publishCap = new Capability("publishDocument",
            "{ documentId: .documentId, enrichedData: .enrichedData }",
            "{ publishedUrl: .publishedUrl, step: .step }");
    publishCap.setDescription("Publish an enriched document");

    definition.getCapabilities().addAll(List.of(validateCap, enrichCap, publishCap));

    // --- Workers ---

    Workflow validateWf =
            FuncWorkflowBuilder.workflow("validate-document")
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
                    .build();

    Worker validator = new Worker("document-validator", List.of(validateCap), new WorkflowFunction(validateWf));
    validator.setDescription("Validates incoming documents");

    Workflow enrichWf =
            FuncWorkflowBuilder.workflow("enrich-document")
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
                    .build();

    Worker enricher = new Worker("document-enricher", List.of(enrichCap), new WorkflowFunction(enrichWf));
    enricher.setDescription("Enriches validated documents with metadata");

    Workflow publishWf =
            FuncWorkflowBuilder.workflow("publish-document")
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
                    .build();

    Worker publisher = new Worker("document-publisher", List.of(publishCap), new WorkflowFunction(publishWf));
    publisher.setDescription("Publishes enriched documents");

    definition.getWorkers().addAll(List.of(validator, enricher, publisher));

    // --- Dispatch Rules ---

    DispatchRule ruleValidate = new DispatchRule("trigger-on-received", validateCap,
            new ContextChangeTrigger(new JQExpressionEvaluator(".step == \"received\"")), null);

    DispatchRule ruleEnrich = new DispatchRule("trigger-on-validated", enrichCap,
            new ContextChangeTrigger(new JQExpressionEvaluator(".step == \"validated\" and .valid == true")), null);

    DispatchRule rulePublish = new DispatchRule("trigger-on-enriched", publishCap,
            new ContextChangeTrigger(new JQExpressionEvaluator(".step == \"enriched\"")), null);

    definition.getRules().addAll(List.of(ruleValidate, ruleEnrich, rulePublish));

    // --- Milestones ---

    Milestone msValidated = new Milestone("documentValidated",
            new JQExpressionEvaluator(".step == \"validated\""));
    msValidated.setDescription("Document has been validated");

    Milestone msEnriched = new Milestone("documentEnriched",
            new JQExpressionEvaluator(".step == \"enriched\""));
    msEnriched.setDescription("Document has been enriched");

    Milestone msPublished = new Milestone("documentPublished",
            new JQExpressionEvaluator(".step == \"published\""));
    msPublished.setDescription("Document has been published");

    definition.getMilestones().addAll(List.of(msValidated, msEnriched, msPublished));

    // --- Goal and Completion ---

    Goal goal = new Goal("pipelineComplete",
            new JQExpressionEvaluator(".step == \"published\""), GoalKind.SUCCESS);
    goal.setDescription("All pipeline steps completed successfully");
    definition.getGoals().add(goal);

    GoalBasedCompletion completion = new GoalBasedCompletion(
            new AllOfGoalExpression(List.of(goal)), null);
    definition.setCompletion(completion);

    return definition;
  }
}
