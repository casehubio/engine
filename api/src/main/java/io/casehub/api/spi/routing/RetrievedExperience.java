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

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A retrieved case experience from the CBR memory store. Represents a past case with a similar
 * problem to the current case, including the solution that was applied, the outcome achieved, and
 * the full plan trace showing which bindings were selected.
 *
 * @param problem the problem description from the past case
 * @param solution the solution that was applied
 * @param outcome the final case outcome (COMPLETED, FAULTED, etc.)
 * @param confidence the quality/success score of the outcome (0.0-1.0, nullable)
 * @param similarityScore how similar this past case is to the current case (-1.0 to 1.0)
 * @param features extracted features from the past case (empty map if none)
 * @param planTrace the sequence of plan steps that were executed (empty list if none)
 * @param featureSimilarities per-feature similarity contributions (empty map when unavailable)
 * @param caseType the case definition type that produced this experience (nullable — null for
 *     same-type queries where the type is implicit)
 * @param sourceType discriminator: PLAN_TRACE for historical execution traces, RESOLUTION_GUIDE for
 *     knowledge base documents
 * @param documentContent prose solution from a ResolutionGuide (nullable — null for plan trace
 *     results)
 * @param documentSteps structured steps from a ResolutionGuide (nullable — null for plan trace
 *     results)
 * @param caseId the CBR case store ID for this experience (nullable — null when unavailable or for
 *     backward compatibility)
 */
public record RetrievedExperience(
    String problem,
    String solution,
    String outcome,
    Double confidence,
    double similarityScore,
    Map<String, Object> features,
    List<ExperiencePlanStep> planTrace,
    Map<String, Double> featureSimilarities,
    String caseType,
    ResolutionSourceType sourceType,
    @Nullable String documentContent,
    @Nullable List<DocumentStep> documentSteps,
    @Nullable String caseId) {

  public RetrievedExperience(
      String problem,
      String solution,
      String outcome,
      Double confidence,
      double similarityScore,
      Map<String, Object> features,
      List<ExperiencePlanStep> planTrace,
      Map<String, Double> featureSimilarities) {
    this(
        problem,
        solution,
        outcome,
        confidence,
        similarityScore,
        features,
        planTrace,
        featureSimilarities,
        null,
        ResolutionSourceType.PLAN_TRACE,
        null,
        null,
        null);
  }

  public RetrievedExperience(
      String problem,
      String solution,
      String outcome,
      Double confidence,
      double similarityScore,
      Map<String, Object> features,
      List<ExperiencePlanStep> planTrace,
      Map<String, Double> featureSimilarities,
      String caseType) {
    this(
        problem,
        solution,
        outcome,
        confidence,
        similarityScore,
        features,
        planTrace,
        featureSimilarities,
        caseType,
        ResolutionSourceType.PLAN_TRACE,
        null,
        null,
        null);
  }

  public RetrievedExperience(
      String problem,
      String solution,
      String outcome,
      Double confidence,
      double similarityScore,
      Map<String, Object> features,
      List<ExperiencePlanStep> planTrace,
      Map<String, Double> featureSimilarities,
      String caseType,
      ResolutionSourceType sourceType,
      @Nullable String documentContent,
      @Nullable List<DocumentStep> documentSteps) {
    this(
        problem,
        solution,
        outcome,
        confidence,
        similarityScore,
        features,
        planTrace,
        featureSimilarities,
        caseType,
        sourceType,
        documentContent,
        documentSteps,
        null);
  }

  public RetrievedExperience {
    Objects.requireNonNull(problem, "problem must not be null");
    Objects.requireNonNull(solution, "solution must not be null");
    if (similarityScore < -1.0 || similarityScore > 1.0) {
      throw new IllegalArgumentException(
          "similarityScore must be in range [-1.0, 1.0], got: " + similarityScore);
    }
    features = features != null ? Map.copyOf(features) : Map.of();
    planTrace = planTrace != null ? List.copyOf(planTrace) : List.of();
    featureSimilarities = featureSimilarities != null ? Map.copyOf(featureSimilarities) : Map.of();
    documentSteps = documentSteps != null ? List.copyOf(documentSteps) : null;
    if (sourceType == null) {
      sourceType = ResolutionSourceType.PLAN_TRACE;
    }
  }
}
