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

/**
 * Sealed interface for caller-type-specific routing hints on {@link JudgmentTarget}.
 *
 * <p>Separates WHO should answer a yield from WHAT is being asked (yield semantics on
 * JudgmentTarget) and HOW the answer is verified (verifier/escalator on JudgmentTarget).
 *
 * <p>Refs engine#995, engine#994.
 */
public sealed interface RoutingConfig permits HumanRoutingConfig {}
