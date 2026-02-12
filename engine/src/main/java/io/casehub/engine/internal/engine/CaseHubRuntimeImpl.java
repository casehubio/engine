package io.casehub.engine.internal.engine;

import io.casehub.context.StateContext;
import io.casehub.engine.CaseHubRuntime;
import io.casehub.model.CaseHubDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class CaseHubRuntimeImpl implements CaseHubRuntime {

  @Inject
  CaseHubReactor reactor;

  @Override
  public CompletionStage<UUID> submitCase(CaseHubDefinition definition) {
    return reactor.submitCase(definition);
  }

  @Override
  public CompletionStage<Void> startCase(UUID caseId) {
    return reactor.startCase(caseId);
  }

  @Override
  public CompletionStage<Void> startCase(StateContext context, UUID caseId) {
    return reactor.startCase(context, caseId);
  }
}
