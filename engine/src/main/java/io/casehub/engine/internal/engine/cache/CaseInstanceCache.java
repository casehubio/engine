package io.casehub.engine.internal.engine.cache;


import io.casehub.engine.internal.model.CaseInstance;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class CaseInstanceCache {

  private final Map<UUID, CaseInstance> cache = new ConcurrentHashMap<>();

  public void put(CaseInstance instance) {
    cache.put(instance.getUuid(), instance);
  }

  public CaseInstance get(UUID caseId) {
    return cache.get(caseId);
  }

  public void clear() {
    cache.clear();
  }

}
