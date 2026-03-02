package io.casehub.engine.internal.engine.handler;

import io.casehub.engine.internal.engine.CaseDefinitionRegistry;
import io.casehub.engine.internal.event.CaseStateContextChangedEvent;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.GoalReachedEvent;
import io.casehub.engine.internal.event.MilestoneReachedEvent;
import io.casehub.engine.internal.event.WorkerScheduleEvent;
import io.casehub.engine.internal.jq.JQEvaluator;
import io.casehub.engine.internal.jq.ValidationResult;
import io.casehub.engine.internal.model.CaseDefinition;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.model.CaseState;
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

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class CaseStateContextChangedEventHandler {

  private static final Logger LOG = Logger.getLogger(CaseStartedEventHandler.class);

  @Inject
  EventBus eventBus;

  @Inject
  CaseDefinitionRegistry caseDefinitionRegistry;

  @Inject
  JQEvaluator jqEvaluator;

  @ConsumeEvent(value = EventBusAddresses.CONTEXT_CHANGED)
  public Uni<Void> onCaseStateContextChangedEventHandler(CaseStateContextChangedEvent event) {
    CaseInstance caseInstance = event.instance();
    if(!caseInstance.getState().equals(CaseState.ACTIVE)){
      return Uni.createFrom().voidItem();
    }

    CaseDefinition caseDefinition = caseInstance.getCaseDefinition();
    CaseHubDefinition caseHubDefinition = caseDefinitionRegistry.getCaseDefinition(caseDefinition);

    if (caseHubDefinition == null) {
      return Uni.createFrom().failure(
              new RuntimeException("Case definition not found for caseId: " + caseInstance.getUuid())
      );
    }

    LOG.infof("Handling CaseStateContextChangedEvent for caseId: %s", caseInstance.getUuid());

    return rules(caseInstance, caseHubDefinition)
            .chain(() -> milestones(caseInstance, caseHubDefinition))
            .chain(() -> goals(caseInstance, caseHubDefinition))
            .invoke(() -> LOG.debugf("Rules+milestones+goals processed for caseId: %s", caseInstance.getUuid()))
            .onFailure().invoke(t -> LOG.errorf(t, "Failed handling context changed for caseId: %s", caseInstance.getUuid()));

  }

  private Uni<Void> rules(CaseInstance caseInstance, CaseHubDefinition definition) {
    List<DispatchRule> rules = definition.getSpec().getRules();
    if (rules == null || rules.isEmpty()) {
      return Uni.createFrom().voidItem();
    }

    List<Worker> workers = definition.getSpec().getWorkers();
    List<Capability> capabilities = definition.getSpec().getCapabilities();

    List<Uni<Void>> unis = new ArrayList<>();

    for (DispatchRule rule : rules) {
      Trigger trigger = rule.getOn();
      if (trigger == null || trigger.getContextChange() == null) {
        continue;
      }

      ContextChangeTrigger cct = trigger.getContextChange();
      String jqExpression = normalizeJqFilter(rule, cct);

      ValidationResult matches = jqEvaluator.eval(jqExpression, caseInstance.getStateContext().asJsonNode());
      if (!matches.ok() || !matches.isTrue()) {
        continue;
      }

      Capability capability = findCapabilityByName(capabilities, rule.getCapability());
      if (capability == null) {
        LOG.warnf("Capability '%s' referenced by rule '%s' not found", rule.getCapability(), rule.getName());
        continue;
      }

      unis.add(publishWorkerSchedules(caseInstance, workers, rule, capability));
    }

    if (unis.isEmpty()) {
      return Uni.createFrom().voidItem();
    }

    return Uni.combine().all().unis(unis).discardItems();
  }

  private Uni<Void> milestones(CaseInstance caseInstance, CaseHubDefinition definition) {
    List<Milestone> milestones = definition.getSpec().getMilestones();
    if (milestones == null || milestones.isEmpty()) {
      return Uni.createFrom().voidItem();
    }

    for (Milestone milestone : milestones) {
      if (milestone.getCondition() == null || milestone.getCondition().isBlank()) continue;

      ValidationResult result = jqEvaluator.eval(milestone.getCondition(), caseInstance.getStateContext().asJsonNode());
      if (!result.ok() || !result.isTrue()) continue;

      LOG.infof("Milestone '%s' REACHED! Publishing MilestoneReachedEvent", milestone.getName());

      eventBus.publish(
              EventBusAddresses.MILESTONE_REACHED,
              new MilestoneReachedEvent(caseInstance, milestone)
      );
    }

    return Uni.createFrom().voidItem();
  }

  private Uni<Void> goals(CaseInstance caseInstance, CaseHubDefinition definition) {
    List<Goal> goals = definition.getSpec().getGoals();
    if (goals == null || goals.isEmpty()) {
      return Uni.createFrom().voidItem();
    }

    for (Goal goal : goals) {
      if (goal.getCondition() == null || goal.getCondition().isBlank()) continue;

      ValidationResult result = jqEvaluator.eval(goal.getCondition(), caseInstance.getStateContext().asJsonNode());
      if (!result.ok() || !result.isTrue()) continue;

      LOG.infof("Goal '%s' REACHED! Publishing GoalReachedEvent", goal.getName());

      eventBus.publish(
              EventBusAddresses.GOAL_REACHED,
              new GoalReachedEvent(caseInstance, goal)
      );
    }

    return Uni.createFrom().voidItem();
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

  private Uni<Void> publishWorkerSchedules(CaseInstance caseInstance,
                                           List<Worker> workers,
                                           DispatchRule rule,
                                           Capability capability) {

    if (workers == null || workers.isEmpty()) {
      LOG.warnf("No workers defined; cannot schedule capability '%s' for rule '%s'",
              rule.getCapability(), rule.getName());
      return Uni.createFrom().voidItem();
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
              EventBusAddresses.SCHEDULE_WORKER,
              new WorkerScheduleEvent(caseInstance, worker, capability)
      );
    }
    return Uni.createFrom().voidItem();
  }

}
