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
package io.casehub.engine.common.spi;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.CaseStatus;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.internal.model.CaseMetaModel;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public abstract class CrossTenantCaseInstanceRepositoryContractTest {

  protected abstract CrossTenantCaseInstanceRepository repository();

  protected abstract CaseInstance saveInstance(CaseInstance instance, String tenancyId);

  protected CaseInstance newInstance(String tenancyId) {
    CaseMetaModel meta = new CaseMetaModel();
    meta.setNamespace("ns");
    meta.setName("test-case");
    meta.setVersion("1.0");

    CaseInstance instance = new CaseInstance();
    instance.setUuid(UUID.randomUUID());
    instance.setCaseMetaModel(meta);
    instance.setState(CaseStatus.RUNNING);
    instance.tenancyId = tenancyId;
    return instance;
  }

  @Test
  void findByUuid_returnsStoredInstance() {
    CaseInstance saved = saveInstance(newInstance("tenant-a"), "tenant-a");

    assertThat(repository().findByUuid(saved.getUuid()))
        .isPresent()
        .get()
        .extracting(CaseInstance::getUuid)
        .isEqualTo(saved.getUuid());
  }

  @Test
  void findByUuid_returnsEmptyForUnknownId() {
    assertThat(repository().findByUuid(UUID.randomUUID())).isEmpty();
  }

  @Test
  void findByUuid_findsAcrossTenants() {
    CaseInstance saved = saveInstance(newInstance("tenant-a"), "tenant-a");
    CaseInstance savedB = saveInstance(newInstance("tenant-b"), "tenant-b");

    assertThat(repository().findByUuid(saved.getUuid())).isPresent();
    assertThat(repository().findByUuid(savedB.getUuid())).isPresent();
  }
}
