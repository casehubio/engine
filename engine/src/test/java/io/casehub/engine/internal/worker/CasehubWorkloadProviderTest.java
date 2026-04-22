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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

class CasehubWorkloadProviderTest {

  @Test
  void noJobsScheduled_returnsZero() throws SchedulerException {
    Scheduler scheduler = mock(Scheduler.class);
    when(scheduler.getJobGroupNames()).thenReturn(List.of());
    CasehubWorkloadProvider provider = new CasehubWorkloadProvider(scheduler);
    assertThat(provider.getActiveWorkCount("some-worker")).isZero();
  }

  @Test
  void oneMatchingJob_returnsOne() throws SchedulerException {
    Scheduler scheduler = mock(Scheduler.class);
    JobKey key = new JobKey("hash-abc", "case-1");
    JobDetail detail = mock(JobDetail.class);
    JobDataMap dataMap = new JobDataMap();
    dataMap.put("workerId", "my-worker");
    when(scheduler.getJobGroupNames()).thenReturn(List.of("case-1"));
    when(scheduler.getJobKeys(GroupMatcher.groupEquals("case-1"))).thenReturn(Set.of(key));
    when(scheduler.getJobDetail(key)).thenReturn(detail);
    when(detail.getJobDataMap()).thenReturn(dataMap);
    CasehubWorkloadProvider provider = new CasehubWorkloadProvider(scheduler);
    assertThat(provider.getActiveWorkCount("my-worker")).isEqualTo(1);
  }

  @Test
  void multipleGroups_countsAcrossAll() throws SchedulerException {
    Scheduler scheduler = mock(Scheduler.class);
    JobKey key1 = new JobKey("hash-1", "case-1");
    JobKey key2 = new JobKey("hash-2", "case-2");
    JobKey key3 = new JobKey("hash-3", "case-2");
    JobDetail detail1 = jobDetailWithWorker("target-worker");
    JobDetail detail2 = jobDetailWithWorker("target-worker");
    JobDetail detail3 = jobDetailWithWorker("other-worker");
    when(scheduler.getJobGroupNames()).thenReturn(List.of("case-1", "case-2"));
    when(scheduler.getJobKeys(GroupMatcher.groupEquals("case-1"))).thenReturn(Set.of(key1));
    when(scheduler.getJobKeys(GroupMatcher.groupEquals("case-2"))).thenReturn(Set.of(key2, key3));
    when(scheduler.getJobDetail(key1)).thenReturn(detail1);
    when(scheduler.getJobDetail(key2)).thenReturn(detail2);
    when(scheduler.getJobDetail(key3)).thenReturn(detail3);
    CasehubWorkloadProvider provider = new CasehubWorkloadProvider(scheduler);
    assertThat(provider.getActiveWorkCount("target-worker")).isEqualTo(2);
    assertThat(provider.getActiveWorkCount("other-worker")).isEqualTo(1);
  }

  @Test
  void schedulerThrows_returnsZero() throws SchedulerException {
    Scheduler scheduler = mock(Scheduler.class);
    when(scheduler.getJobGroupNames()).thenThrow(new SchedulerException("simulated failure"));
    CasehubWorkloadProvider provider = new CasehubWorkloadProvider(scheduler);
    assertThat(provider.getActiveWorkCount("any-worker")).isZero();
  }

  @Test
  void noJobsMatchingWorker_returnsZero() throws SchedulerException {
    Scheduler scheduler = mock(Scheduler.class);
    JobKey key = new JobKey("hash-abc", "case-1");
    JobDetail detail = jobDetailWithWorker("different-worker");
    when(scheduler.getJobGroupNames()).thenReturn(List.of("case-1"));
    when(scheduler.getJobKeys(GroupMatcher.groupEquals("case-1"))).thenReturn(Set.of(key));
    when(scheduler.getJobDetail(key)).thenReturn(detail);
    CasehubWorkloadProvider provider = new CasehubWorkloadProvider(scheduler);
    assertThat(provider.getActiveWorkCount("my-worker")).isZero();
  }

  @Test
  void jobDetailMissingWorkerId_doesNotCrash() throws SchedulerException {
    Scheduler scheduler = mock(Scheduler.class);
    JobKey key = new JobKey("hash-abc", "case-1");
    JobDetail detail = mock(JobDetail.class);
    JobDataMap dataMap = new JobDataMap(); // no workerId field
    when(scheduler.getJobGroupNames()).thenReturn(List.of("case-1"));
    when(scheduler.getJobKeys(GroupMatcher.groupEquals("case-1"))).thenReturn(Set.of(key));
    when(scheduler.getJobDetail(key)).thenReturn(detail);
    when(detail.getJobDataMap()).thenReturn(dataMap);
    CasehubWorkloadProvider provider = new CasehubWorkloadProvider(scheduler);
    assertThat(provider.getActiveWorkCount("any-worker")).isZero();
  }

  private JobDetail jobDetailWithWorker(String workerId) {
    JobDetail detail = mock(JobDetail.class);
    JobDataMap dataMap = new JobDataMap();
    dataMap.put("workerId", workerId);
    when(detail.getJobDataMap()).thenReturn(dataMap);
    return detail;
  }
}
