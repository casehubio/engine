package io.casehub.engine.internal.engine;

import io.casehub.context.StateContext;
import io.casehub.engine.CaseHubRuntime;
import io.casehub.engine.internal.context.StateContextImpl;
import io.casehub.model.CaseHubDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
  public CompletionStage<UUID> startCase(CaseHubDefinition definition, StateContext context) {
    return reactor.startCase(definition, context);
  }
}
