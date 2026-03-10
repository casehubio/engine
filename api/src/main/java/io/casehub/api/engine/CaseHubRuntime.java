package io.casehub.api.engine;

import io.casehub.api.context.StateContext;
import io.casehub.api.model.CaseHubDefinition;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface CaseHubRuntime {

  CompletionStage<UUID> startCase(CaseHubDefinition definition);

  CompletionStage<UUID> startCase(CaseHubDefinition definition, StateContext context);

  CompletionStage<Object> query(UUID caseId, String path);

  <T> CompletionStage<T> query(UUID caseId, String path, Class<T> clazz);
}