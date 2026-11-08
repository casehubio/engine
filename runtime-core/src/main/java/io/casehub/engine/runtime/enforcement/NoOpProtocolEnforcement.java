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
package io.casehub.engine.runtime.enforcement;

import io.casehub.api.model.enforcement.EnforcementLifecycleEvent;
import io.casehub.api.model.enforcement.InteractionRule;
import io.casehub.api.model.enforcement.TurnDecision;
import io.casehub.api.model.enforcement.TurnRequest;
import io.casehub.api.model.enforcement.Violation;
import io.casehub.api.model.enforcement.ViolationResponse;
import io.casehub.api.spi.enforcement.ProtocolEnforcement;
import java.util.List;

public class NoOpProtocolEnforcement implements ProtocolEnforcement {

  @Override
  public TurnDecision requestTurn(TurnRequest request, List<InteractionRule> activeRules) {
    return new TurnDecision.Permitted(request.agentId());
  }

  @Override
  public ViolationResponse handleViolation(Violation violation) {
    return new ViolationResponse.Warned(violation.agentId(), "no moderator configured");
  }

  @Override
  public boolean checkCompliance(String agentId, List<InteractionRule> rules) {
    return true;
  }

  @Override
  public void onLifecycleEvent(EnforcementLifecycleEvent event) {}
}
