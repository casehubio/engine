package io.casehub.engine;

import io.casehub.context.StateContext;
import io.casehub.model.CaseHubDefinition;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface CaseHubRuntime {

  CompletionStage<UUID> submitCase(CaseHubDefinition definition);

  CompletionStage<Void> startCase(UUID caseId);

  CompletionStage<Void> startCase(StateContext context, UUID caseId);
}