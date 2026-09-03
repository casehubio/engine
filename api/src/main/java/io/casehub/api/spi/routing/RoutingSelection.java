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
package io.casehub.api.spi.routing;

import java.util.List;

/**
 * Routing rationale captured at selection time.
 *
 * <p>Carried on {@link RoutingResult.Selected} so the handler that schedules the worker can bridge
 * it into the event-layer {@code SelectionContext} for ledger auditing.
 *
 * @param strategyId which strategy made the selection
 * @param selected the chosen candidate
 * @param alternatives other candidates considered (may be empty)
 */
public record RoutingSelection(
    String strategyId, Candidate selected, List<Candidate> alternatives) {

  public record Candidate(String workerId, double score, String phase, String reason) {}
}
