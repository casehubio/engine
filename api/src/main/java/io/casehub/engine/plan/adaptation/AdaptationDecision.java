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
package io.casehub.engine.plan.adaptation;

import java.util.Objects;

public sealed interface AdaptationDecision {

  String reason();

  record Persist(String reason) implements AdaptationDecision {
    public Persist {
      Objects.requireNonNull(reason, "reason");
    }
  }

  record Refine(RefineScope scope, String reason) implements AdaptationDecision {
    public Refine {
      Objects.requireNonNull(scope, "scope");
      Objects.requireNonNull(reason, "reason");
    }
  }

  record Concede(String reason, String compoundId) implements AdaptationDecision {
    public Concede {
      Objects.requireNonNull(reason, "reason");
      Objects.requireNonNull(compoundId, "compoundId");
    }
  }
}
