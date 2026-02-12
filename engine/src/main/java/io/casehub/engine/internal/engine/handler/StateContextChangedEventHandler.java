package io.casehub.engine.internal.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.context.StateContext;
import io.casehub.engine.internal.engine.CaseHubDefinitionRegistry;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.GoalReachedEvent;
import io.casehub.engine.internal.event.MilestoneReachedEvent;
import io.casehub.engine.internal.event.StateContextChangedEvent;
import io.casehub.engine.internal.event.WorkerScheduleEvent;
import io.casehub.engine.internal.jq.JQEvaluator;
import io.casehub.engine.internal.jq.ValidationResult;
import io.casehub.engine.internal.model.CaseHub;
import io.casehub.model.Capability;
import io.casehub.model.CaseHubDefinition;
import io.casehub.model.ContextChangeTrigger;
import io.casehub.model.DispatchRule;
import io.casehub.model.Goal;
import io.casehub.model.Milestone;
import io.casehub.model.Trigger;
import io.casehub.model.Worker;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Handles StateContextChangedEvent events published on the EventBus.
 * This handler is triggered when StateContext is modified.
 * <p>
 * Responsibilities:
 * - Observe state changes
 * - React to context modifications
 * - Trigger workers based on dispatch rules (future)
 * - Evaluate milestones (future)
 */
@ApplicationScoped
public class StateContextChangedEventHandler {

  private static final Logger LOG = Logger.getLogger(StateContextChangedEventHandler.class);

  @Inject
  ObjectMapper objectMapper;

  @Inject
  JQEvaluator jqEvaluator;

  @Inject
  CaseHubDefinitionRegistry definitionRegistry;

  @Inject
  EventBus eventBus;

  /**
   * Processes StateContextChangedEvent when context is modified.
   * This method is called automatically when an event is published to the CONTEXT_CHANGED address.
   *
   * @param event the context changed event containing CaseHubInstanceRunState
   * @return Uni signaling completion
   */
  @ConsumeEvent(value = EventBusAddresses.CONTEXT_CHANGED)
  public Uni<Void> onContextChanged(StateContextChangedEvent event) {
    logEventHeader(event);

    StateContext context = event.getRunState().getStateContext();
    CaseHub caseHub = event.getRunState().getCaseHub();

    logContext(context);

    return loadDefinition(event, caseHub)
            .invoke(definition -> process(event, context, definition))   // side-effects: publish
            .onFailure().invoke(e -> LOG.errorf(e,
                    "Failed to process StateContextChangedEvent for runId: %s", event.getRunId()))
            .replaceWithVoid()
            .onTermination().invoke(() -> LOG.infof("================================"));
  }

  private void logEventHeader(StateContextChangedEvent event) {
    LOG.infof("=== StateContextChangedEvent received ===");
    LOG.infof("Run ID: %s", event.getRunId());
    LOG.infof("Change Type: %s", event.getChangeType());
  }

  private void logContext(StateContext context) {
    LOG.debugf("Current state size: %d keys", context.size());
    LOG.debugf("Current state: %s", context.asJsonNode().toPrettyString());
  }

  private Uni<CaseHubDefinition> loadDefinition(StateContextChangedEvent event, CaseHub caseHub) {
    return Uni.createFrom().item(() -> {
              JsonNode definitionJson = caseHub.getDefinition();
              if (definitionJson == null) {
                LOG.warnf("CaseHub definition is null for runId: %s", event.getRunId());
                throw new IllegalStateException("CaseHub definition is null for runId: " + event.getRunId());
              }

              // TODO we currently rely on the definition registry since we can't ser/deser workflows defined programmatically (e.g. via Java DSL)
              CaseHubDefinition definition = definitionRegistry.getDefinition(caseHub);

              if (definition == null) {
                LOG.warnf("CaseHub definition not found in registry for runId: %s", event.getRunId());
                // TODO replace with actual deserialization once we can handle programmatically defined workflows
                // definition = objectMapper.treeToValue(definitionJson, CaseHubDefinition.class);
              }

              if (definition == null || definition.getSpec() == null) {
                if (definition == null) {
                  LOG.warnf("CaseHub definition is null for runId: %s", event.getRunId());
                } else {
                  LOG.warnf("CaseHub definition spec is null for runId: %s", event.getRunId());
                }
              }

              return definition;
            })
            .onFailure().invoke(e -> LOG.errorf(e,
                    "Failed to load/resolve CaseHubDefinition for runId: %s", event.getRunId()));
  }

  private void process(StateContextChangedEvent event, StateContext context, CaseHubDefinition definition) {
    if (definition == null || definition.getSpec() == null) {
      return;
    }

    rules(event, context, definition);
    milestones(event, context, definition);
    goals(event, context, definition);
  }

  private void rules(StateContextChangedEvent event,
                     StateContext context,
                     CaseHubDefinition definition) {
    List<DispatchRule> rules = definition.getSpec().getRules();
    LOG.infof("Loaded %d dispatch rules", rules != null ? rules.size() : 0);

    if (rules == null || rules.isEmpty()) {
      return;
    }

    List<Worker> workers = definition.getSpec().getWorkers();
    List<Capability> capabilities = definition.getSpec().getCapabilities();

    for (DispatchRule rule : rules) {
      Trigger trigger = rule.getOn();
      if (trigger == null || trigger.getContextChange() == null) {
        LOG.debugf("Rule '%s' has no context change trigger", rule.getName());
        continue;
      }

      ContextChangeTrigger cct = trigger.getContextChange();
      String jqExpression = normalizeJqFilter(rule, cct);

      ValidationResult matches = jqEvaluator.eval(jqExpression, context.asJsonNode());
      if (!matches.ok()) {
        LOG.warnf("Rule '%s' JQ expression evaluation failed: %s", rule.getName(), matches.error());
        continue;
      }

      if (!matches.isTrue()) {
        LOG.infof("Rule '%s' does NOT match (JQ returned false), would NOT trigger capability: %s",
                rule.getName(), rule.getCapability());
        continue;
      }

      LOG.infof("Rule '%s' matches, would trigger capability: %s", rule.getName(), rule.getCapability());

      Capability capability = findCapabilityByName(capabilities, rule.getCapability());
      if (capability == null) {
        LOG.warnf("Capability '%s' referenced by rule '%s' not found", rule.getCapability(), rule.getName());
        continue;
      }

      publishWorkerSchedules(event, workers, rule, capability);
    }
  }

  private String normalizeJqFilter(DispatchRule rule, ContextChangeTrigger cct) {
    String jqExpression = cct.getFilter();
    if (jqExpression == null || jqExpression.isBlank()) {
      LOG.debugf("Rule '%s' has no JQ filter defined, treating as always true", rule.getName());
      return ".";
    }
    return jqExpression;
  }

  private Capability findCapabilityByName(List<Capability> capabilities, String name) {
    if (capabilities == null || name == null) return null;
    for (Capability c : capabilities) {
      if (name.equals(c.getName())) return c;
    }
    return null;
  }

  private void publishWorkerSchedules(StateContextChangedEvent event,
                                      List<Worker> workers,
                                      DispatchRule rule,
                                      Capability capability) {

    if (workers == null || workers.isEmpty()) {
      LOG.warnf("No workers defined; cannot schedule capability '%s' for rule '%s'",
              rule.getCapability(), rule.getName());
      return;
    }

    for (Worker worker : workers) {
      if (worker.getCapabilities() == null) {
        continue;
      }
      if (!worker.getCapabilities().contains(rule.getCapability())) {
        continue;
      }

      LOG.infof("Worker '%s' matches capability '%s' from rule '%s' -> scheduling",
              worker.getName(), rule.getCapability(), rule.getName());

      eventBus.publish(
              EventBusAddresses.SCHEDULE_WORKER_RUN,
              new WorkerScheduleEvent(event.getRunState(), worker, capability)
      );
    }
  }

  /**
   * Process milestones from the CaseHub definition.
   * Evaluates each milestone condition using JQ and publishes MilestoneReachedEvent
   * when a milestone condition evaluates to true.
   *
   * @param event the context changed event
   * @param context the current state context
   * @param definition the CaseHub definition containing milestone definitions
   */
  private void milestones(StateContextChangedEvent event,
                          StateContext context,
                          CaseHubDefinition definition) {
    List<Milestone> milestones = definition.getSpec().getMilestones();
    LOG.infof("Loaded %d milestones", milestones != null ? milestones.size() : 0);

    if (milestones == null || milestones.isEmpty()) {
      return;
    }

    LOG.debugf("Milestones: %s", milestones);

    for (Milestone milestone : milestones) {
      if (milestone.getCondition() == null || milestone.getCondition().isBlank()) {
        LOG.warnf("Milestone '%s' has no condition defined, skipping", milestone.getName());
        continue;
      }

      LOG.debugf("Evaluating milestone '%s' with condition: %s",
                 milestone.getName(), milestone.getCondition());

      ValidationResult result = jqEvaluator.eval(milestone.getCondition(), context.asJsonNode());

      if (!result.ok()) {
        LOG.warnf("Milestone '%s' condition evaluation failed: %s",
                  milestone.getName(), result.error());
        continue;
      }

      if (!result.isTrue()) {
        LOG.debugf("Milestone '%s' condition NOT met (JQ returned false)", milestone.getName());
        continue;
      }

      LOG.infof("Milestone '%s' REACHED! Publishing MilestoneReachedEvent", milestone.getName());

      MilestoneReachedEvent milestoneEvent = new MilestoneReachedEvent(
              event.getRunState(),
              milestone
      );

      eventBus.publish(EventBusAddresses.MILESTONE_REACHED, milestoneEvent);
    }
  }

  /**
   * Process goals from the CaseHub definition.
   * Evaluates each goal condition using JQ and publishes GoalReachedEvent
   * when a goal condition evaluates to true.
   *
   * @param event the context changed event
   * @param context the current state context
   * @param definition the CaseHub definition containing goal definitions
   */
  private void goals(StateContextChangedEvent event,
                     StateContext context,
                     CaseHubDefinition definition) {
    List<Goal> goals = definition.getSpec().getGoals();
    LOG.infof("Loaded %d goals", goals != null ? goals.size() : 0);

    if (goals == null || goals.isEmpty()) {
      return;
    }

    LOG.debugf("Goals: %s", goals);

    for (Goal goal : goals) {
      if (goal.getCondition() == null || goal.getCondition().isBlank()) {
        LOG.warnf("Goal '%s' has no condition defined, skipping", goal.getName());
        continue;
      }

      LOG.debugf("Evaluating goal '%s' with condition: %s",
                 goal.getName(), goal.getCondition());

      ValidationResult result = jqEvaluator.eval(goal.getCondition(), context.asJsonNode());

      if (!result.ok()) {
        LOG.warnf("Goal '%s' condition evaluation failed: %s",
                  goal.getName(), result.error());
        continue;
      }

      if (!result.isTrue()) {
        LOG.debugf("Goal '%s' condition NOT met (JQ returned false)", goal.getName());
        continue;
      }

      // Goal reached!
      LOG.infof("Goal '%s' REACHED! Publishing GoalReachedEvent", goal.getName());

      GoalReachedEvent goalEvent = new GoalReachedEvent(
              event.getRunState(),
              goal
      );

      eventBus.publish(EventBusAddresses.GOAL_REACHED, goalEvent);
    }
  }
}
