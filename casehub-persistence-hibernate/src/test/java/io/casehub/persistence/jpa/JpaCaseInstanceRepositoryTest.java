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
package io.casehub.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.CaseStatus;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.model.CaseMetaModel;
import io.casehub.engine.spi.CaseInstanceRepository;
import io.casehub.engine.spi.CaseMetaModelRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class JpaCaseInstanceRepositoryTest {

  @Inject CaseInstanceRepository instanceRepository;
  @Inject CaseMetaModelRepository metaModelRepository;

  private CaseMetaModel savedMeta;

  @BeforeEach
  void setUp() {
    String unique = UUID.randomUUID().toString().substring(0, 8);
    CaseMetaModel meta = new CaseMetaModel();
    meta.setName("instance-test-" + unique);
    meta.setNamespace("test-ns");
    meta.setVersion("1.0");
    savedMeta = run(() -> metaModelRepository.save(meta));
  }

  @Test
  void save_populatesId() {
    CaseInstance instance = newInstance(savedMeta, CaseStatus.RUNNING);

    CaseInstance saved = run(() -> instanceRepository.save(instance));

    assertThat(saved.id).isNotNull().isPositive();
  }

  @Test
  void findByUuid_returnsNullForUnknown() {
    CaseInstance result = run(() -> instanceRepository.findByUuid(UUID.randomUUID()));

    assertThat(result).isNull();
  }

  @Test
  void findByUuid_returnsSavedInstanceWithMetaModel() {
    UUID uuid = UUID.randomUUID();
    CaseInstance instance = newInstance(savedMeta, CaseStatus.RUNNING);
    instance.setUuid(uuid);
    run(() -> instanceRepository.save(instance));

    CaseInstance found = run(() -> instanceRepository.findByUuid(uuid));

    assertThat(found).isNotNull();
    assertThat(found.getUuid()).isEqualTo(uuid);
    assertThat(found.getState()).isEqualTo(CaseStatus.RUNNING);
    assertThat(found.getCaseMetaModel()).isNotNull();
    assertThat(found.getCaseMetaModel().getId()).isEqualTo(savedMeta.getId());
  }

  @Test
  void update_changesState() {
    CaseInstance instance = newInstance(savedMeta, CaseStatus.RUNNING);
    run(() -> instanceRepository.save(instance));

    instance.setState(CaseStatus.COMPLETED);
    run(() -> instanceRepository.update(instance));

    CaseInstance reloaded = run(() -> instanceRepository.findByUuid(instance.getUuid()));
    assertThat(reloaded.getState()).isEqualTo(CaseStatus.COMPLETED);
  }

  private <T> T run(Supplier<Uni<T>> supplier) {
    try {
      return VertxContextSupport.subscribeAndAwait(supplier);
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private CaseInstance newInstance(CaseMetaModel meta, CaseStatus status) {
    CaseInstance instance = new CaseInstance();
    instance.setUuid(UUID.randomUUID());
    instance.setState(status);
    instance.setCaseMetaModel(meta);
    return instance;
  }
}
