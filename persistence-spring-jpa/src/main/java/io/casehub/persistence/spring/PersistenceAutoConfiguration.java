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
package io.casehub.persistence.spring;

import io.casehub.engine.common.spi.CaseInstanceRepository;
import io.casehub.engine.common.spi.CaseMetaModelRepository;
import io.casehub.engine.common.spi.CrossTenantCaseInstanceRepository;
import io.casehub.engine.common.spi.CrossTenantEventLogRepository;
import io.casehub.engine.common.spi.EventLogRepository;
import io.casehub.engine.common.spi.PlanItemStore;
import io.casehub.engine.common.spi.SubCaseGroupRepository;
import io.casehub.engine.common.spi.recovery.ExecutionSnapshotStore;
import io.casehub.engine.common.spi.recovery.PlanVersionStore;
import io.casehub.persistence.jpa.RlsPolicySetup;
import io.casehub.persistence.jpa.TenantContextManager;
import jakarta.persistence.EntityManager;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(EntityManager.class)
@EntityScan(basePackages = "io.casehub.persistence.jpa")
public class PersistenceAutoConfiguration {

  @Bean
  TenantContextManager tenantContextManager(
      EntityManager em, @Value("${casehub.rls.enabled:false}") boolean rlsEnabled) {
    return new TenantContextManager(em, rlsEnabled);
  }

  @Bean
  RlsPolicySetup rlsPolicySetup(
      DataSource dataSource, @Value("${casehub.rls.enabled:false}") boolean rlsEnabled) {
    return new RlsPolicySetup(dataSource, rlsEnabled);
  }

  @Bean
  CommandLineRunner rlsPolicyRunner(RlsPolicySetup setup) {
    return args -> setup.apply();
  }

  @Bean
  CaseInstanceRepository caseInstanceRepository(
      EntityManager em,
      TenantContextManager tcm,
      org.springframework.beans.factory.ObjectProvider<
              io.casehub.engine.common.spi.recovery.CaseContextRecoveryStrategy>
          recoveryStrategy) {
    return new SpringJpaCaseInstanceRepository(em, tcm, recoveryStrategy);
  }

  @Bean
  CaseMetaModelRepository caseMetaModelRepository(EntityManager em, TenantContextManager tcm) {
    return new SpringJpaCaseMetaModelRepository(em, tcm);
  }

  @Bean
  EventLogRepository eventLogRepository(EntityManager em, TenantContextManager tcm) {
    return new SpringJpaEventLogRepository(em, tcm);
  }

  @Bean
  SubCaseGroupRepository subCaseGroupRepository(EntityManager em, TenantContextManager tcm) {
    return new SpringJpaSubCaseGroupRepository(em, tcm);
  }

  @Bean
  CrossTenantCaseInstanceRepository crossTenantCaseInstanceRepository(
      EntityManager em, TenantContextManager tcm) {
    return new SpringJpaCrossTenantCaseInstanceRepository(em, tcm);
  }

  @Bean
  CrossTenantEventLogRepository crossTenantEventLogRepository(
      EntityManager em, TenantContextManager tcm) {
    return new SpringJpaCrossTenantEventLogRepository(em, tcm);
  }

  @Bean
  PlanItemStore planItemStore(EntityManager em, TenantContextManager tcm) {
    return new SpringJpaPlanItemStore(em, tcm);
  }

  @Bean
  PlanVersionStore planVersionStore(EntityManager em, TenantContextManager tcm) {
    return new SpringJpaPlanVersionStore(em, tcm);
  }

  @Bean
  ExecutionSnapshotStore executionSnapshotStore(EntityManager em, TenantContextManager tcm) {
    return new SpringJpaExecutionSnapshotStore(em, tcm);
  }

  @Bean
  io.casehub.engine.queue.spi.CaseQueueEntryStore caseQueueEntryStore(
      EntityManager em, TenantContextManager tcm) {
    return new SpringJpaCaseQueueEntryStore(em, tcm);
  }
}
