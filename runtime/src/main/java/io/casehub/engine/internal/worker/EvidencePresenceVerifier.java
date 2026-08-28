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
package io.casehub.engine.internal.worker;

import io.casehub.api.spi.judgment.JudgmentVerifier;
import io.casehub.api.spi.judgment.VerificationContext;
import io.casehub.api.spi.judgment.VerificationResult;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class EvidencePresenceVerifier implements JudgmentVerifier {

  @Override
  public VerificationResult verify(VerificationContext context) {
    List<String> required = context.target().evidenceRequirements();
    if (required.isEmpty()) return new VerificationResult.Accepted();
    List<String> missing =
        required.stream().filter(key -> !context.evidence().containsKey(key)).toList();
    if (missing.isEmpty()) return new VerificationResult.Accepted();
    return new VerificationResult.InsufficientEvidence(
        "Missing required evidence keys: " + missing, missing);
  }

  @Override
  public String id() {
    return "evidence-presence";
  }
}
