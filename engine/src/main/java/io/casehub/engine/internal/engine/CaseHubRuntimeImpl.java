package io.casehub.engine.internal.engine;

import io.casehub.api.engine.CaseHubRuntime;
import io.casehub.engine.internal.context.StateContextImpl;
import io.casehub.api.model.CaseHubDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
class CaseHubRuntimeImpl implements CaseHubRuntime {

  @Inject
  CaseHubReactor reactor;

  @Override
  public CompletionStage<UUID> startCase(CaseHubDefinition definition) {
    return reactor.startCase(definition, new StateContextImpl());
  }

  @Override
  public CompletionStage<UUID> startCase(CaseHubDefinition definition, Map<String, Object> inputData) {
    return reactor.startCase(definition, new StateContextImpl(inputData));
  }

  @Override
  public void signal(UUID caseId, String path, Object value) {
    reactor.signal(caseId, path, value);
  }

  @Override
  public CompletionStage<Object> query(UUID caseId, String path) {
    return reactor.query(caseId, path);
  }

  @Override
  public <T> CompletionStage<T> query(UUID caseId, String path, Class<T> clazz) {
    return reactor.query(caseId, path, clazz);
  }
}
