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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared utility for computing per-worker success rates from CBR plan trace data. Used by both
 * {@link io.casehub.ledger.routing.TrustWeightedAgentStrategy} (engine-ledger, trust-blended
 * scoring) and {@code CbrAgentRoutingStrategy} (blocks, CBR-first routing).
 *
 * <p>Stateless — all methods are static. Co-located with {@link RetrievedExperience} and {@link
 * ExperiencePlanStep} which it operates on.
 */
public final class ExperienceAnalyser {

  public static final Map<RoutingOutcome, Double> DEFAULT_OUTCOME_WEIGHTS =
      Map.ofEntries(
          Map.entry(RoutingOutcome.SUCCESS, 1.0),
          Map.entry(RoutingOutcome.FAILURE, -1.0),
          Map.entry(RoutingOutcome.GATE_EXPIRED, -0.25),
          Map.entry(RoutingOutcome.GATE_REJECTED, -0.5),
          Map.entry(RoutingOutcome.DECLINED, -0.5),
          Map.entry(RoutingOutcome.CANCELLED, 0.0),
          Map.entry(RoutingOutcome.OBSOLETE, 0.0));

  private ExperienceAnalyser() {}

  /**
   * Computes per-worker success rates using a caller-supplied step filter predicate.
   *
   * <p>Generalisation of {@link #workerSuccessRates(List, Set, String, Map)} — the predicate
   * replaces the hardcoded {@code capabilityName.equals(step.capabilityName())} check, enabling
   * callers to match on any step field (e.g. {@code step.bindingName()} for humanTask traces).
   *
   * @param experiences retrieved similar cases from the CBR store
   * @param eligibleWorkerIds worker IDs to score
   * @param stepFilter predicate selecting which plan trace steps to include in scoring
   * @param outcomeWeights per-outcome scoring weights
   * @return per-worker scores in [-1.0, 1.0]; empty map when no matching data
   */
  public static Map<String, Double> workerSuccessRates(
      final List<RetrievedExperience> experiences,
      final Set<String> eligibleWorkerIds,
      final java.util.function.Predicate<ExperiencePlanStep> stepFilter,
      final Map<RoutingOutcome, Double> outcomeWeights) {
    final Map<String, double[]> workerStats = new HashMap<>();

    for (final RetrievedExperience exp : experiences) {
      final double relevance = exp.similarityScore();
      if (relevance <= 0.0) {
        continue;
      }

      for (final ExperiencePlanStep step : exp.planTrace()) {
        if (!stepFilter.test(step)
            || step.workerName() == null
            || !eligibleWorkerIds.contains(step.workerName())) {
          continue;
        }

        if ("ADDED".equals(step.adaptationAction())
            || "SUBSTITUTED".equals(step.adaptationAction())) {
          continue;
        }

        RoutingOutcome outcome = step.stepOutcome();

        final double outcomeWeight = outcomeWeights.getOrDefault(outcome, 0.0);
        final double[] stats = workerStats.computeIfAbsent(step.workerName(), k -> new double[2]);
        stats[0] += outcomeWeight * relevance;
        stats[1] += relevance;
      }
    }

    final Map<String, Double> scores = new HashMap<>();
    for (final Map.Entry<String, double[]> entry : workerStats.entrySet()) {
      final double evidenceMass = entry.getValue()[1];
      if (evidenceMass > 0.0) {
        scores.put(entry.getKey(), entry.getValue()[0] / evidenceMass);
      }
    }
    return scores;
  }

  /**
   * Computes per-worker success rates, filtering plan trace steps by capability name.
   *
   * <p>Delegates to the predicate overload. Steps with null {@code capabilityName} (e.g. humanTask
   * traces) are naturally excluded since {@code "x".equals(null)} is false.
   */
  public static Map<String, Double> workerSuccessRates(
      final List<RetrievedExperience> experiences,
      final Set<String> eligibleWorkerIds,
      final String capabilityName,
      final Map<RoutingOutcome, Double> outcomeWeights) {
    return workerSuccessRates(
        experiences,
        eligibleWorkerIds,
        step -> capabilityName.equals(step.capabilityName()),
        outcomeWeights);
  }

  public static final double DEFAULT_MAX_COST_FACTOR = 10.0;

  /**
   * Computes per-action cost multipliers from CBR plan traces. Actions with low historical success
   * rates get higher cost factors, steering the planner toward more reliable paths.
   *
   * @param experiences retrieved similar cases from the CBR store
   * @param actionNames action names (capability names) to compute cost factors for
   * @param minSamples minimum discrete sample count before learned costs override declared costs
   * @param maxCostFactor upper bound on the cost multiplier (prevents infinity on zero success)
   * @param outcomeWeights per-outcome scoring weights
   * @return per-action cost multipliers (1.0 = no adjustment); empty map on cold start
   */
  public static Map<String, Double> actionCostFactors(
      final List<RetrievedExperience> experiences,
      final Set<String> actionNames,
      final int minSamples,
      final double maxCostFactor,
      final Map<RoutingOutcome, Double> outcomeWeights) {

    final Map<String, double[]> actionStats = new HashMap<>();
    final Map<String, Integer> actionCounts = new HashMap<>();

    for (final RetrievedExperience exp : experiences) {
      final double relevance = exp.similarityScore();
      if (relevance <= 0.0) {
        continue;
      }

      for (final ExperiencePlanStep step : exp.planTrace()) {
        if (step.capabilityName() == null || !actionNames.contains(step.capabilityName())) {
          continue;
        }
        if ("ADDED".equals(step.adaptationAction())
            || "SUBSTITUTED".equals(step.adaptationAction())) {
          continue;
        }

        final String actionName = step.capabilityName();
        actionCounts.merge(actionName, 1, Integer::sum);

        final double outcomeWeight = outcomeWeights.getOrDefault(step.stepOutcome(), 0.0);
        final double[] stats = actionStats.computeIfAbsent(actionName, k -> new double[2]);
        stats[0] += outcomeWeight * relevance;
        stats[1] += relevance;
      }
    }

    final double minRate = 1.0 / maxCostFactor;
    final Map<String, Double> factors = new HashMap<>();
    for (final Map.Entry<String, double[]> entry : actionStats.entrySet()) {
      final String actionName = entry.getKey();
      final int sampleCount = actionCounts.getOrDefault(actionName, 0);
      if (sampleCount < minSamples) {
        continue;
      }
      final double evidenceMass = entry.getValue()[1];
      if (evidenceMass <= 0.0) {
        continue;
      }
      final double successRate = entry.getValue()[0] / evidenceMass;
      final double clampedRate = Math.max(successRate, minRate);
      factors.put(actionName, 1.0 / clampedRate);
    }
    return factors;
  }

  public static Map<String, Double> actionCostFactors(
      final List<RetrievedExperience> experiences,
      final Set<String> actionNames,
      final int minSamples) {
    return actionCostFactors(
        experiences, actionNames, minSamples, DEFAULT_MAX_COST_FACTOR, DEFAULT_OUTCOME_WEIGHTS);
  }

  public static Map<String, Double> actionFailureRates(
      final List<RetrievedExperience> experiences,
      final Set<String> actionNames,
      final int minSamples) {
    final Map<String, int[]> actionCounts = new HashMap<>();
    for (final RetrievedExperience exp : experiences) {
      for (final ExperiencePlanStep step : exp.planTrace()) {
        if (step.capabilityName() == null || !actionNames.contains(step.capabilityName())) {
          continue;
        }
        if ("ADDED".equals(step.adaptationAction())
            || "SUBSTITUTED".equals(step.adaptationAction())) {
          continue;
        }
        final int[] counts = actionCounts.computeIfAbsent(step.capabilityName(), k -> new int[2]);
        counts[0]++;
        if (step.stepOutcome() != RoutingOutcome.SUCCESS) {
          counts[1]++;
        }
      }
    }
    final Map<String, Double> rates = new HashMap<>();
    for (final var entry : actionCounts.entrySet()) {
      final int total = entry.getValue()[0];
      if (total < minSamples) {
        continue;
      }
      final int failures = entry.getValue()[1];
      rates.put(entry.getKey(), (double) failures / total);
    }
    return Map.copyOf(rates);
  }
}
