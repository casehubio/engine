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

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.agent;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.get;

@ApplicationScoped
public class AgentPipelineBean extends CaseHub {

  Agents.SentimentAnalysisAgent sentimentAgent;
  Agents.ContentSummarizerAgent summarizerAgent;

  public void setAgents(Agents.SentimentAnalysisAgent sentimentAgent,
                        Agents.ContentSummarizerAgent summarizerAgent) {
    this.sentimentAgent = sentimentAgent;
    this.summarizerAgent = summarizerAgent;
  }

  @Override
  public CaseHubDefinition getDefinition() {
    CaseHubDefinition definition = new CaseHubDefinition();
    definition.setDsl("0.1");
    definition.setVersion("1.0.0");
    definition.setName("Agent Document Review Pipeline");
    definition.setNamespace("test");
    definition.setTitle("Three-step document analysis pipeline with HTTP fetch and two agents");

    CaseDefinitionSpec spec = new CaseDefinitionSpec();

    // --- Capabilities ---

    Capability fetchCap = new Capability();
    fetchCap.setName("fetchDocument");
    fetchCap.setDescription("Fetch document data from external API");
    fetchCap.setInputSchema("{ documentId: .documentId }");
    fetchCap.setOutputSchema("{ data: .data, step: .step }");

    Capability sentimentCap = new Capability();
    sentimentCap.setName("analyzeSentiment");
    sentimentCap.setDescription("Analyze document sentiment using LLM agent");
    sentimentCap.setInputSchema("{ documentId: .documentId, content: .data.content }");
    sentimentCap.setOutputSchema("{ sentiment: .sentiment, step: .step }");

    Capability summaryCap = new Capability();
    summaryCap.setName("summarizeContent");
    summaryCap.setDescription("Summarize document content using LLM agent");
    summaryCap.setInputSchema("{ documentId: .documentId, content: .data.content }");
    summaryCap.setOutputSchema("{ summary: .summary, step: .step }");

    spec.setCapabilities(List.of(fetchCap, sentimentCap, summaryCap));

    // --- Workers ---

    // 1. HTTP fetch worker
    Worker fetchWorker = new Worker();
    fetchWorker.setName("document-fetcher");
    fetchWorker.setDescription("Fetches document data from external API");
    fetchWorker.setCapabilities(List.of("fetchDocument"));

    Workflow fetchWf =
            FuncWorkflowBuilder.workflow("fetch-document")
                    .tasks(
                            get("fetchDocumentData", "http://localhost:8889/fetch/{id}")
                                    .inputFrom("{ id: .documentId }")
                                    .outputAs("{ data: ., step: \"fetched\" }")
                    )
                    .build();
    fetchWorker.setWorkflow(fetchWf);

    // 2. Sentiment analysis agent worker
    Worker sentimentWorker = new Worker();
    sentimentWorker.setName("sentiment-analyzer");
    sentimentWorker.setDescription("Analyzes document sentiment via LLM");
    sentimentWorker.setCapabilities(List.of("analyzeSentiment"));

    Workflow sentimentWf =
            FuncWorkflowBuilder.workflow("analyze-sentiment")
                    .tasks(
                            agent(sentimentAgent::analyze, Agents.SentimentRequest.class)
                                    .inputFrom("{ documentId: .documentId, content: .data.content }")
                                    .outputAs("{ sentiment: ., step: \"analyzed\" }")
                    )
                    .build();
    sentimentWorker.setWorkflow(sentimentWf);

    // 3. Content summarizer agent worker
    Worker summaryWorker = new Worker();
    summaryWorker.setName("content-summarizer");
    summaryWorker.setDescription("Summarizes document content via LLM");
    summaryWorker.setCapabilities(List.of("summarizeContent"));

    Workflow summaryWf =
            FuncWorkflowBuilder.workflow("summarize-content")
                    .tasks(
                            agent(summarizerAgent::summarize, Agents.SummaryRequest.class)
                                    .inputFrom("{ documentId: .documentId, content: .data.content }")
                                    .outputAs("{ summary: ., step: \"summarized\" }")
                    )
                    .build();
    summaryWorker.setWorkflow(summaryWf);

    spec.setWorkers(List.of(fetchWorker, sentimentWorker, summaryWorker));

    // --- Dispatch Rules ---

    DispatchRule ruleFetch = new DispatchRule();
    ruleFetch.setName("trigger-on-submitted");
    ruleFetch.setCapability("fetchDocument");
    Trigger triggerFetch = new Trigger();
    ContextChangeTrigger ctxFetch = new ContextChangeTrigger();
    ctxFetch.setFilter(".step == \"submitted\"");
    triggerFetch.setContextChange(ctxFetch);
    ruleFetch.setOn(triggerFetch);

    DispatchRule ruleSentiment = new DispatchRule();
    ruleSentiment.setName("trigger-on-fetched");
    ruleSentiment.setCapability("analyzeSentiment");
    Trigger triggerSentiment = new Trigger();
    ContextChangeTrigger ctxSentiment = new ContextChangeTrigger();
    ctxSentiment.setFilter(".step == \"fetched\"");
    triggerSentiment.setContextChange(ctxSentiment);
    ruleSentiment.setOn(triggerSentiment);

    DispatchRule ruleSummary = new DispatchRule();
    ruleSummary.setName("trigger-on-analyzed");
    ruleSummary.setCapability("summarizeContent");
    Trigger triggerSummary = new Trigger();
    ContextChangeTrigger ctxSummary = new ContextChangeTrigger();
    ctxSummary.setFilter(".step == \"analyzed\"");
    triggerSummary.setContextChange(ctxSummary);
    ruleSummary.setOn(triggerSummary);

    spec.setRules(List.of(ruleFetch, ruleSentiment, ruleSummary));

    // --- Milestones ---

    Milestone msFetched = new Milestone();
    msFetched.setName("documentFetched");
    msFetched.setDescription("Document data has been fetched from API");
    msFetched.setCondition(".step == \"fetched\"");

    Milestone msSentiment = new Milestone();
    msSentiment.setName("sentimentAnalyzed");
    msSentiment.setDescription("Document sentiment has been analyzed");
    msSentiment.setCondition(".step == \"analyzed\"");

    Milestone msSummary = new Milestone();
    msSummary.setName("contentSummarized");
    msSummary.setDescription("Document content has been summarized");
    msSummary.setCondition(".step == \"summarized\"");

    spec.setMilestones(List.of(msFetched, msSentiment, msSummary));

    // --- Goal and Completion ---

    Goal goal = new Goal();
    goal.setName("reviewComplete");
    goal.setDescription("Document review is complete with sentiment and summary");
    goal.setCondition(".step == \"summarized\"");
    spec.setGoals(List.of(goal));

    CaseCompletion completion = new CaseCompletion();
    GoalExpression successExpr = new GoalExpression();
    successExpr.setAllOf(List.of("reviewComplete"));
    completion.setSuccess(successExpr);
    spec.setCompletion(completion);

    definition.setSpec(spec);
    return definition;
  }
}
