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
package io.casehub.engine.internal.scheduler;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import io.casehub.api.model.Binding;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ScheduleTrigger;
import io.casehub.api.model.Worker;
import io.casehub.api.model.evaluator.ExpressionEvaluator;
import io.casehub.engine.internal.engine.CaseDefinitionRegistry;
import io.casehub.engine.internal.model.CaseInstance;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;

/**
 * Service for scheduling time-based triggers for Case Hub bindings.
 *
 * <p>This service manages Quartz jobs that fire when scheduled triggers activate. It supports:
 *
 * <ul>
 *   <li><b>Unconditional scheduling</b> - worker executes when trigger fires
 *   <li><b>Conditional scheduling</b> - worker executes only if condition evaluates to true
 *   <li><b>Cancellation</b> - remove all scheduled triggers when a case completes
 * </ul>
 *
 * <p><b>Lifecycle:</b>
 *
 * <ul>
 *   <li>Register triggers when a case is created: {@link #registerScheduledTriggers(CaseInstance)}
 *   <li>Cancel all triggers when a case completes: {@link #cancelAllTriggers(UUID)}
 * </ul>
 *
 * @see ScheduleTrigger
 * @see ScheduledTriggerJob
 * @see ConditionalScheduledTriggerJob
 */
@ApplicationScoped
public class SchedulerService {

  private static final Logger LOG = Logger.getLogger(SchedulerService.class);

  @Inject Scheduler quartz;

  @Inject CaseDefinitionRegistry caseDefinitionRegistry;

  /**
   * Register all scheduled triggers for a case instance.
   *
   * <p>Scans the case definition for bindings with {@link ScheduleTrigger} and creates Quartz jobs
   * for each. Jobs are grouped by case ID for easy bulk cancellation.
   *
   * @param caseInstance the case instance to register triggers for
   * @return Uni that completes when all triggers are registered
   */
  public Uni<Void> registerScheduledTriggers(CaseInstance caseInstance) {
    CaseDefinition definition =
        caseDefinitionRegistry.getCaseDefinition(caseInstance.getCaseMetaModel());

    if (definition == null) {
      return Uni.createFrom()
          .failure(
              new IllegalStateException(
                  "CaseDefinition not found for case: " + caseInstance.getUuid()));
    }

    List<Binding> bindings = definition.getBindings();
    if (bindings == null || bindings.isEmpty()) {
      return Uni.createFrom().voidItem();
    }

    List<Uni<Void>> scheduleOps = new ArrayList<>();

    for (Binding binding : bindings) {
      if (!(binding.getOn() instanceof ScheduleTrigger trigger)) {
        continue;
      }

      // Find worker that provides the capability
      Worker worker = findWorkerForCapability(definition, binding.getCapability());
      if (worker == null) {
        LOG.warnf(
            "No worker found for capability '%s' in binding '%s', skipping",
            binding.getCapability().getName(), binding.getName());
        continue;
      }

      if (binding.getWhen() != null) {
        // Conditional scheduling
        scheduleOps.add(
            scheduleConditionalWorker(
                caseInstance.getUuid(), binding, trigger, binding.getWhen(), worker));
      } else {
        // Unconditional scheduling
        scheduleOps.add(scheduleWorker(caseInstance.getUuid(), binding, trigger, worker));
      }
    }

    if (scheduleOps.isEmpty()) {
      return Uni.createFrom().voidItem();
    }

    return Uni.combine().all().unis(scheduleOps).discardItems();
  }

  /**
   * Schedule unconditional worker execution when the trigger fires.
   *
   * @param caseId the case ID
   * @param binding the binding configuration
   * @param trigger the schedule trigger
   * @param worker the worker to execute
   * @return Uni that completes when the job is scheduled
   */
  public Uni<Void> scheduleWorker(
      UUID caseId, Binding binding, ScheduleTrigger trigger, Worker worker) {

    return Uni.createFrom()
        .item(
            () -> {
              JobKey jobKey = createJobKey(caseId, binding.getName());
              JobDetail job = createJobDetail(jobKey, caseId, binding, worker, null);
              Trigger quartzTrigger = createQuartzTrigger(jobKey, trigger);

              try {
                quartz.scheduleJob(job, quartzTrigger);
                LOG.infof(
                    "Scheduled unconditional trigger: case=%s, binding=%s, trigger=%s",
                    caseId, binding.getName(), trigger);
              } catch (SchedulerException e) {
                throw new RuntimeException(
                    "Failed to schedule trigger for binding: " + binding.getName(), e);
              }

              return null;
            })
        .replaceWithVoid();
  }

  /**
   * Schedule conditional worker execution. The worker executes only if the condition evaluates to
   * true when the trigger fires.
   *
   * @param caseId the case ID
   * @param binding the binding configuration
   * @param trigger the schedule trigger
   * @param condition the condition to evaluate
   * @param worker the worker to execute if condition is true
   * @return Uni that completes when the job is scheduled
   */
  public Uni<Void> scheduleConditionalWorker(
      UUID caseId,
      Binding binding,
      ScheduleTrigger trigger,
      ExpressionEvaluator condition,
      Worker worker) {

    return Uni.createFrom()
        .item(
            Unchecked.supplier(
                () -> {
                  JobKey jobKey = createJobKey(caseId, binding.getName());
                  JobDetail job = createJobDetail(jobKey, caseId, binding, worker, condition);
                  Trigger quartzTrigger = createQuartzTrigger(jobKey, trigger);

                  try {
                    quartz.scheduleJob(job, quartzTrigger);
                    LOG.infof(
                        "Scheduled conditional trigger: case=%s, binding=%s, trigger=%s, condition=%s",
                        caseId, binding.getName(), trigger, condition);
                  } catch (SchedulerException e) {
                    throw new RuntimeException(
                        "Failed to schedule conditional trigger for binding: " + binding.getName(),
                        e);
                  }

                  return null;
                }))
        .replaceWithVoid();
  }

  /**
   * Cancel all scheduled triggers for a case.
   *
   * <p>This should be called when a case completes (COMPLETED, CLOSED, or FAULTED state).
   *
   * @param caseId the case ID
   * @return Uni that completes when all triggers are cancelled
   */
  public Uni<Void> cancelAllTriggers(UUID caseId) {
    return Uni.createFrom()
        .item(
            Unchecked.supplier(
                () -> {
                  try {
                    String groupName = "case-" + caseId;
                    GroupMatcher<JobKey> matcher = GroupMatcher.jobGroupEquals(groupName);
                    Set<JobKey> jobKeys = quartz.getJobKeys(matcher);

                    if (!jobKeys.isEmpty()) {
                      quartz.deleteJobs(new ArrayList<>(jobKeys));
                      LOG.infof(
                          "Cancelled %d scheduled triggers for case %s", jobKeys.size(), caseId);
                    } else {
                      LOG.debugf("No scheduled triggers to cancel for case %s", caseId);
                    }
                  } catch (SchedulerException e) {
                    throw new RuntimeException("Failed to cancel triggers for case: " + caseId, e);
                  }

                  return null;
                }))
        .replaceWithVoid();
  }

  /**
   * Cancel a specific scheduled trigger for a case.
   *
   * @param caseId the case ID
   * @param bindingName the binding name
   * @return Uni that completes when the trigger is cancelled
   */
  public Uni<Void> cancelTrigger(UUID caseId, String bindingName) {
    return Uni.createFrom()
        .item(
            Unchecked.supplier(
                () -> {
                  try {
                    JobKey jobKey = createJobKey(caseId, bindingName);
                    boolean deleted = quartz.deleteJob(jobKey);

                    if (deleted) {
                      LOG.infof(
                          "Cancelled scheduled trigger: case=%s, binding=%s", caseId, bindingName);
                    } else {
                      LOG.debugf(
                          "No trigger found to cancel: case=%s, binding=%s", caseId, bindingName);
                    }
                  } catch (SchedulerException e) {
                    throw new RuntimeException(
                        "Failed to cancel trigger for binding: " + bindingName, e);
                  }

                  return null;
                }))
        .replaceWithVoid();
  }

  private JobKey createJobKey(UUID caseId, String bindingName) {
    return new JobKey("binding-" + bindingName, "case-" + caseId);
  }

  private JobDetail createJobDetail(
      JobKey jobKey, UUID caseId, Binding binding, Worker worker, ExpressionEvaluator condition) {

    Class<? extends org.quartz.Job> jobClass =
        (condition != null) ? ConditionalScheduledTriggerJob.class : ScheduledTriggerJob.class;

    return newJob(jobClass)
        .withIdentity(jobKey)
        .storeDurably(false)
        .usingJobData("caseId", caseId.toString())
        .usingJobData("bindingName", binding.getName())
        .usingJobData("capabilityName", binding.getCapability().getName())
        .usingJobData("workerName", worker.getName())
        .build();
  }

  private Trigger createQuartzTrigger(JobKey jobKey, ScheduleTrigger trigger) {
    if (trigger.isCron()) {
      return createCronTrigger(jobKey, trigger.getCron());
    } else if (trigger.isDelay()) {
      return createDelayTrigger(jobKey, trigger.getDelay().toMillis());
    } else {
      throw new IllegalArgumentException("ScheduleTrigger must have either cron or delay set");
    }
  }

  private CronTrigger createCronTrigger(JobKey jobKey, String cronExpression) {
    return newTrigger()
        .withIdentity("trigger-" + jobKey.getName(), jobKey.getGroup())
        .withSchedule(cronSchedule(cronExpression))
        .build();
  }

  private SimpleTrigger createDelayTrigger(JobKey jobKey, long delayMillis) {
    Date startTime = new Date(System.currentTimeMillis() + delayMillis);

    return newTrigger()
        .withIdentity("trigger-" + jobKey.getName(), jobKey.getGroup())
        .startAt(startTime)
        .withSchedule(simpleSchedule().withRepeatCount(0)) // one-shot
        .build();
  }

  private Worker findWorkerForCapability(CaseDefinition definition, Capability capability) {
    return definition.getWorkers().stream()
        .filter(
            w ->
                w.getCapabilities().stream()
                    .anyMatch(c -> c.getName().equals(capability.getName())))
        .findFirst()
        .orElse(null);
  }
}
