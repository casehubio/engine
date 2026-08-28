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
package io.casehub.api.spi.judgment;

import io.casehub.platform.api.routing.NamedStrategy;

/**
 * Post-response verification strategy for judgment yields.
 *
 * <p>Resolved via {@code EngineStrategyResolver} from {@code JudgmentTarget.verifierStrategy()}.
 * When no strategy is configured (null), verification is skipped entirely. The {@link
 * VerificationContext} carries both the original yield context and the response fields (decision,
 * evidence, callerId, callerType).
 *
 * <p>Refs engine#997, engine#994.
 */
public interface JudgmentVerifier extends NamedStrategy {

  VerificationResult verify(VerificationContext context);

  @Override
  default String id() {
    return "accept-all";
  }
}
