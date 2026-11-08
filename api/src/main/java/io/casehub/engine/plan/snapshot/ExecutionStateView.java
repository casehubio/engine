package io.casehub.engine.plan.snapshot;

import java.time.Instant;
import java.util.List;

public record ExecutionStateView(
    String executionId,
    String state,
    ExecutionModelView model,
    String result,
    List<AgentRefView> activeAgents,
    List<AgentResultView> completedAgents,
    Integer iteration,
    Instant startedAt,
    Instant completedAt) {

  public record ExecutionModelView(
      String pattern, String routingStrategy,
      String decompositionStrategy, String activationStrategy,
      String aggregationStrategy, String terminationStrategy,
      FailurePolicyView failurePolicy) {}

  public record FailurePolicyView(
      String routingFailureAction, String aggregationFailureAction,
      AgentRetryPolicyView agentRetryPolicy) {}

  public record AgentRetryPolicyView(
      int maxRetries, String backoffStrategy, Integer initialDelayMs) {}

  public record AgentRefView(String id, String type, String name) {}

  public record AgentResultView(
      AgentRefView agentRef, String status,
      String detail, String error, Long duration) {}
}
