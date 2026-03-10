package io.casehub.api;

import io.casehub.api.engine.CaseHubRuntime;
import io.casehub.engine.internal.context.StateContextImpl;
import io.casehub.api.model.CaseHubDefinition;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public abstract class CaseHub {

  @Inject
  CaseHubRuntime runtime;

  public abstract CaseHubDefinition getDefinition();

  public CompletionStage<UUID> startCase() {
    return runtime.startCase(getDefinition());
  }

  public CompletionStage<UUID> startCase(Map<String, Object> inputData) {
    return runtime.startCase(getDefinition(), new StateContextImpl(inputData));
  }

  public CompletionStage<Object> query(UUID caseId, String path) {
    return runtime.query(caseId, path);
  }

  public <T> CompletionStage<T> query(UUID caseId, String path, Class<T> clazz) {
    return runtime.query(caseId, path, clazz);
  }
}
