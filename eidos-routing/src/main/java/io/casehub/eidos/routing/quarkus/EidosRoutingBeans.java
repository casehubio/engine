/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.eidos.routing.quarkus;

import io.casehub.api.spi.routing.AgentRoutingStrategy;
import io.casehub.eidos.api.AgentSelector;
import io.casehub.eidos.api.CapabilityHealth;
import io.casehub.eidos.routing.EngineAwareAgentSelector;
import io.casehub.ledger.api.spi.TrustScoreSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import java.util.Optional;

@ApplicationScoped
public class EidosRoutingBeans {

  @Produces
  @ApplicationScoped
  AgentSelector engineAwareAgentSelector(
      CapabilityHealth capabilityHealth,
      AgentRoutingStrategy routingStrategy,
      Instance<TrustScoreSource> trustSourceInstance) {
    return new EngineAwareAgentSelector(
        capabilityHealth,
        routingStrategy,
        trustSourceInstance.isResolvable()
            ? Optional.of(trustSourceInstance.get())
            : Optional.empty());
  }
}
