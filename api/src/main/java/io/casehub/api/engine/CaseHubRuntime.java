package io.casehub.api.engine;

import io.casehub.api.model.CaseHubDefinition;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface CaseHubRuntime {

  CompletionStage<UUID> startCase(CaseHubDefinition definition);

  CompletionStage<UUID> startCase(CaseHubDefinition definition, Map<String, Object> inputData);

  void signal(UUID caseId, String path, Object value);

  CompletionStage<Object> query(UUID caseId, String path);

  <T> CompletionStage<T> query(UUID caseId, String path, Class<T> clazz);
}