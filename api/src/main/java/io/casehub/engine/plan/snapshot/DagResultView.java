package io.casehub.engine.plan.snapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public record DagResultView(
    Map<String, NodeStateView> nodeStates,
    Map<String, Object> completedResults,
    boolean allSucceeded,
    Duration elapsed,
    Instant timestamp,
    Map<String, Long> nodeDurationsMs) {

  public record NodeStateView(
      String kind, String reason,
      DagResultView contingencyResult) {}
}
