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
package io.casehub.api.model;

import io.casehub.api.spi.routing.CandidateSetSpec;
import org.jspecify.annotations.Nullable;

/**
 * Routing configuration for human callers — carried on {@link JudgmentTarget#routingConfig()}.
 *
 * <p>These fields are consumed by the scheduler layer to create WorkItems.
 *
 * <p>Refs engine#995.
 */
public record HumanRoutingConfig(
    @Nullable String templateRef,
    @Nullable CandidateSetSpec candidateGroups,
    @Nullable CandidateSetSpec candidateUsers,
    @Nullable Integer claimDeadlineHours,
    @Nullable Class<?> payloadType)
    implements RoutingConfig {}
