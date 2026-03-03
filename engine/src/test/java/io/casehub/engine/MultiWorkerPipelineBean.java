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
public class MultiWorkerPipelineBean extends CaseHub {

  @Override
  public CaseHubDefinition getDefinition() {
    CaseHubDefinition definition = new CaseHubDefinition();
    definition.setDsl("0.1");
    definition.setVersion("1.0.0");
    definition.setName("Multi-Worker Document Pipeline");
    definition.setNamespace("test");
    definition.setTitle("Three-step document processing pipeline");

    CaseDefinitionSpec spec = new CaseDefinitionSpec();

    // --- Capabilities ---

    Capability validateCap = new Capability();
    validateCap.setName("validateDocument");
    validateCap.setDescription("Validate a received document");
    validateCap.setInputSchema("{ documentId: .documentId, step: .step }");
    validateCap.setOutputSchema("{ valid: .valid, step: .step }");

    Capability enrichCap = new Capability();
    enrichCap.setName("enrichDocument");
    enrichCap.setDescription("Enrich a validated document with metadata");
    enrichCap.setInputSchema("{ documentId: .documentId, valid: .valid }");
    enrichCap.setOutputSchema("{ enrichedData: .enrichedData, step: .step }");

    Capability publishCap = new Capability();
    publishCap.setName("publishDocument");
    publishCap.setDescription("Publish an enriched document");
    publishCap.setInputSchema("{ documentId: .documentId, enrichedData: .enrichedData }");
    publishCap.setOutputSchema("{ publishedUrl: .publishedUrl, step: .step }");

    spec.setCapabilities(List.of(validateCap, enrichCap, publishCap));

    // --- Workers ---

    Worker validator = new Worker();
    validator.setName("document-validator");
    validator.setDescription("Validates incoming documents");
    validator.setCapabilities(List.of("validateDocument"));

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
    validator.setWorkflow(validateWf);

    Worker enricher = new Worker();
    enricher.setName("document-enricher");
    enricher.setDescription("Enriches validated documents with metadata");
    enricher.setCapabilities(List.of("enrichDocument"));

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
    enricher.setWorkflow(enrichWf);

    Worker publisher = new Worker();
    publisher.setName("document-publisher");
    publisher.setDescription("Publishes enriched documents");
    publisher.setCapabilities(List.of("publishDocument"));

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
    publisher.setWorkflow(publishWf);

    spec.setWorkers(List.of(validator, enricher, publisher));

    // --- Dispatch Rules ---

    DispatchRule ruleValidate = new DispatchRule();
    ruleValidate.setName("trigger-on-received");
    ruleValidate.setCapability("validateDocument");
    Trigger triggerValidate = new Trigger();
    ContextChangeTrigger ctxValidate = new ContextChangeTrigger();
    ctxValidate.setFilter(".step == \"received\"");
    triggerValidate.setContextChange(ctxValidate);
    ruleValidate.setOn(triggerValidate);

    DispatchRule ruleEnrich = new DispatchRule();
    ruleEnrich.setName("trigger-on-validated");
    ruleEnrich.setCapability("enrichDocument");
    Trigger triggerEnrich = new Trigger();
    ContextChangeTrigger ctxEnrich = new ContextChangeTrigger();
    ctxEnrich.setFilter(".step == \"validated\" and .valid == true");
    triggerEnrich.setContextChange(ctxEnrich);
    ruleEnrich.setOn(triggerEnrich);

    DispatchRule rulePublish = new DispatchRule();
    rulePublish.setName("trigger-on-enriched");
    rulePublish.setCapability("publishDocument");
    Trigger triggerPublish = new Trigger();
    ContextChangeTrigger ctxPublish = new ContextChangeTrigger();
    ctxPublish.setFilter(".step == \"enriched\"");
    triggerPublish.setContextChange(ctxPublish);
    rulePublish.setOn(triggerPublish);

    spec.setRules(List.of(ruleValidate, ruleEnrich, rulePublish));

    // --- Milestones ---

    Milestone msValidated = new Milestone();
    msValidated.setName("documentValidated");
    msValidated.setDescription("Document has been validated");
    msValidated.setCondition(".step == \"validated\"");

    Milestone msEnriched = new Milestone();
    msEnriched.setName("documentEnriched");
    msEnriched.setDescription("Document has been enriched");
    msEnriched.setCondition(".step == \"enriched\"");

    Milestone msPublished = new Milestone();
    msPublished.setName("documentPublished");
    msPublished.setDescription("Document has been published");
    msPublished.setCondition(".step == \"published\"");

    spec.setMilestones(List.of(msValidated, msEnriched, msPublished));

    // --- Goal and Completion ---

    Goal goal = new Goal();
    goal.setName("pipelineComplete");
    goal.setDescription("All pipeline steps completed successfully");
    goal.setCondition(".step == \"published\"");
    spec.setGoals(List.of(goal));

    CaseCompletion completion = new CaseCompletion();
    GoalExpression successExpr = new GoalExpression();
    successExpr.setAllOf(List.of("pipelineComplete"));
    completion.setSuccess(successExpr);
    spec.setCompletion(completion);

    definition.setSpec(spec);
    return definition;
  }
}
