package io.casehub.api;

import io.casehub.engine.CaseHubRuntime;
import io.casehub.engine.internal.context.StateContextImpl;
import io.casehub.model.CaseHubDefinition;
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
}
