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
package io.casehub.engine.scheduler.quartz;

import io.casehub.engine.common.spi.scheduler.JobScheduler;
import io.casehub.engine.common.spi.scheduler.JobSchedulerContractTest;
import java.util.Properties;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

class QuartzJobSchedulerContractTest extends JobSchedulerContractTest {

  private Scheduler quartzScheduler;

  @Override
  protected JobScheduler createJobScheduler() {
    try {
      Properties props = new Properties();
      props.setProperty("org.quartz.scheduler.instanceName", "test-" + System.nanoTime());
      props.setProperty("org.quartz.threadPool.threadCount", "1");
      props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
      StdSchedulerFactory factory = new StdSchedulerFactory(props);
      quartzScheduler = factory.getScheduler();
      quartzScheduler.start();

      QuartzJobScheduler jobScheduler = new QuartzJobScheduler();
      jobScheduler.quartz = quartzScheduler;
      return jobScheduler;
    } catch (SchedulerException e) {
      throw new RuntimeException("Failed to create Quartz scheduler", e);
    }
  }

  @Override
  protected void destroyJobScheduler() {
    if (quartzScheduler != null) {
      try {
        quartzScheduler.shutdown(true);
      } catch (SchedulerException ignored) {
      }
    }
  }
}
