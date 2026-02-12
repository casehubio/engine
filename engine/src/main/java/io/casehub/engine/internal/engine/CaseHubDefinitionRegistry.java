package io.casehub.engine.internal.engine;

import io.casehub.engine.internal.model.CaseHub;
import io.casehub.model.CaseHubDefinition;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class CaseHubDefinitionRegistry {

  private final Map<CaseHub, CaseHubDefinition> registry = new ConcurrentHashMap<>();

  public void registerDefinition(CaseHub caseHub, CaseHubDefinition definition) {
    registry.put(caseHub, definition);
  }

  public CaseHubDefinition getDefinition(CaseHub caseHub) {
    return registry.get(caseHub);
  }

}
