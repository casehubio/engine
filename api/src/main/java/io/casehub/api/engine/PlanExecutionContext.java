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
package io.casehub.api.engine;

import io.casehub.api.context.CaseContext;
import io.casehub.api.model.CaseDefinition;
import java.util.UUID;

/**
 * Context passed to {@link LoopControl#select} — carries case identity alongside {@link
 * CaseContext}, enabling implementations to look up plan models and stage hierarchies without
 * requiring access to internal engine structures.
 */
public record PlanExecutionContext(
    UUID caseId, CaseDefinition definition, CaseContext caseContext) {}
