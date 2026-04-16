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

import io.casehub.engine.internal.model.CaseMetaModel;
import io.casehub.engine.spi.CaseMetaModelRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

@QuarkusTest
class JpaCaseMetaModelRepositoryTest {

  @Inject CaseMetaModelRepository repository;

  /**
   * Panache.withSession/withTransaction requires a Vert.x duplicated context marked as safe. JUnit
   * test threads have no such context. VertxContextSupport creates the correct context and blocks
   * until the Uni completes.
   */
  private <T> T run(Supplier<Uni<T>> supplier) {
    try {
      return VertxContextSupport.subscribeAndAwait(supplier);
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void save_populatesIdAndCreatedAt() {
    CaseMetaModel saved = run(() -> repository.save(metaModel("save-populates", "ns", "1.0")));

    assertThat(saved.getId()).isNotNull().isPositive();
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  @Test
  void findByKey_returnsNullForUnknown() {
    CaseMetaModel result = run(() -> repository.findByKey("no-such-ns", "no-such-name", "9.9"));

    assertThat(result).isNull();
  }

  @Test
  void findByKey_returnsRegisteredMetaModel() {
    run(() -> repository.save(metaModel("find-by-key", "repo-ns", "2.0")));

    CaseMetaModel found = run(() -> repository.findByKey("repo-ns", "find-by-key", "2.0"));

    assertThat(found).isNotNull();
    assertThat(found.getName()).isEqualTo("find-by-key");
    assertThat(found.getNamespace()).isEqualTo("repo-ns");
    assertThat(found.getVersion()).isEqualTo("2.0");
    assertThat(found.getId()).isNotNull();
  }

  @Test
  void save_thenFindByKey_roundTrip() {
    CaseMetaModel meta = metaModel("round-trip", "rt-ns", "3.0");
    meta.setTitle("Round Trip Title");
    meta.setDsl("yaml");

    CaseMetaModel saved = run(() -> repository.save(meta));
    CaseMetaModel found = run(() -> repository.findByKey("rt-ns", "round-trip", "3.0"));

    assertThat(found.getId()).isEqualTo(saved.getId());
    assertThat(found.getTitle()).isEqualTo("Round Trip Title");
    assertThat(found.getDsl()).isEqualTo("yaml");
    assertThat(found.getCreatedAt()).isEqualTo(saved.getCreatedAt());
  }

  private CaseMetaModel metaModel(String name, String namespace, String version) {
    CaseMetaModel m = new CaseMetaModel();
    m.setName(name);
    m.setNamespace(namespace);
    m.setVersion(version);
    return m;
  }
}
