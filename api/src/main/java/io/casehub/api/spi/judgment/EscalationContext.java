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

import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.JudgmentTarget;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Context for post-verification escalation of judgment yields.
 *
 * <p>Refs engine#1012, engine#999, engine#994.
 */
public record EscalationContext(
    UUID caseId,
    String tenancyId,
    String bindingName,
    JudgmentTarget target,
    String decision,
    List<Evidence> evidence,
    VerificationResult verificationResult,
    int escalationCount,
    int maxEscalations,
    @Nullable CaseDefinition definition,
    @Nullable CallerIdentity callerIdentity,
    @Nullable Duration responseTime) {}
