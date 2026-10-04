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
package io.casehub.work.engine;

import io.casehub.engine.runtime.routing.EngineStrategyResolver;
import io.casehub.work.api.spi.ClaimSlaPolicy;
import io.casehub.work.api.spi.InstanceAssignmentStrategy;
import io.casehub.work.api.spi.SlaBreachPolicy;
import io.casehub.work.api.spi.WorkerSelectionStrategy;

import java.util.List;

public class WorkStrategyContributor {

    private final EngineStrategyResolver           resolver;
    private final List<WorkerSelectionStrategy>    workerStrategies;
    private final List<ClaimSlaPolicy>             claimPolicies;
    private final List<SlaBreachPolicy>            breachPolicies;
    private final List<InstanceAssignmentStrategy> assignmentStrategies;

    public WorkStrategyContributor(
            EngineStrategyResolver resolver,
            List<WorkerSelectionStrategy> workerStrategies,
            List<ClaimSlaPolicy> claimPolicies,
            List<SlaBreachPolicy> breachPolicies,
            List<InstanceAssignmentStrategy> assignmentStrategies) {
        this.resolver             = resolver;
        this.workerStrategies     = workerStrategies;
        this.claimPolicies        = claimPolicies;
        this.breachPolicies       = breachPolicies;
        this.assignmentStrategies = assignmentStrategies;
    }

    public void init() {
        workerStrategies.forEach(this::safeRegister);
        claimPolicies.forEach(this::safeRegister);
        breachPolicies.forEach(this::safeRegister);
        assignmentStrategies.forEach(this::safeRegister);
    }

    private void safeRegister(io.casehub.platform.api.routing.NamedStrategy strategy) {
        try {
            resolver.registerEntry(strategy, false);
        } catch (IllegalStateException e) {
            // already registered — ignore
        }
    }
}
