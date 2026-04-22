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

import io.quarkiverse.work.api.WorkloadProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import org.jboss.logging.Logger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

/**
 * Counts active Quartz jobs per worker name by iterating all scheduled job groups and matching the
 * {@code workerId} field in each job's data map.
 *
 * <p>Used by {@link io.quarkiverse.work.core.strategy.LeastLoadedStrategy} to prefer workers with
 * fewer in-flight tasks.
 */
@ApplicationScoped
public class CasehubWorkloadProvider implements WorkloadProvider {

  private static final Logger LOG = Logger.getLogger(CasehubWorkloadProvider.class);

  private final Scheduler scheduler;

  @Inject
  public CasehubWorkloadProvider(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  @Override
  public int getActiveWorkCount(String workerId) {
    try {
      List<String> groups = scheduler.getJobGroupNames();
      int count = 0;
      for (String group : groups) {
        Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.groupEquals(group));
        for (JobKey key : keys) {
          JobDetail detail = scheduler.getJobDetail(key);
          if (detail != null && workerId.equals(detail.getJobDataMap().getString("workerId"))) {
            count++;
          }
        }
      }
      return count;
    } catch (SchedulerException e) {
      LOG.warnf(
          "Failed to count active jobs for worker '%s' — returning 0: %s",
          workerId, e.getMessage());
      return 0;
    }
  }
}
