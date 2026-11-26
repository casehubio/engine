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
package io.casehub.engine.persistence.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.api.model.CaseStatus;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.internal.model.CaseMetaModel;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.pages.data.json.JsonCollectionStore;
import io.casehub.pages.sqlite.SqliteConnectionFactory;
import io.casehub.platform.api.identity.CurrentPrincipal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JsonCollectionCaseInstanceCacheTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String TENANT = "test-tenant";

  JsonCollectionCaseInstanceCache cache;
  CaseDefinitionRegistry registry;

  @BeforeEach
  void setUp() throws Exception {
    SqliteConnectionFactory connFactory = createInMemorySqlite();
    JsonCollectionStore store = new JsonCollectionStore(connFactory);
    registry = mock(CaseDefinitionRegistry.class);
    CurrentPrincipal principal = mock(CurrentPrincipal.class);
    when(principal.tenancyId()).thenReturn(TENANT);
    cache = new JsonCollectionCaseInstanceCache(store, registry, principal);
  }

  @Test
  void put_thenGet_roundTrips() {
    CaseInstance instance = createInstance("test-ns", "test-case", "1.0", CaseStatus.RUNNING);
    UUID uuid = instance.getUuid();

    cache.put(instance);
    CaseInstance retrieved = cache.get(uuid);

    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getUuid()).isEqualTo(uuid);
    assertThat(retrieved.getState()).isEqualTo(CaseStatus.RUNNING);
    assertThat(retrieved.getVersion()).isEqualTo(instance.getVersion());
    assertThat(retrieved.getActorId()).isEqualTo(instance.getActorId());
    assertThat(retrieved.getContextSnapshot()).isEqualTo(instance.getContextSnapshot());
  }

  @Test
  void put_existingEntry_updates() {
    CaseInstance instance = createInstance("ns", "case-upd", "1.0", CaseStatus.RUNNING);
    UUID uuid = instance.getUuid();
    cache.put(instance);

    instance.setState(CaseStatus.COMPLETED);
    instance.setVersion(2L);
    cache.put(instance);

    CaseInstance retrieved = cache.get(uuid);
    assertThat(retrieved.getState()).isEqualTo(CaseStatus.COMPLETED);
    assertThat(retrieved.getVersion()).isEqualTo(2L);
  }

  @Test
  void get_nonExistent_returnsNull() {
    assertThat(cache.get(UUID.randomUUID())).isNull();
  }

  @Test
  void getAll_returnsAllEntries() {
    cache.put(createInstance("ns", "a", "1.0", CaseStatus.RUNNING));
    cache.put(createInstance("ns", "b", "1.0", CaseStatus.RUNNING));
    cache.put(createInstance("ns", "c", "1.0", CaseStatus.COMPLETED));

    assertThat(cache.getAll()).hasSize(3);
  }

  @Test
  void clear_removesAll() {
    cache.put(createInstance("ns", "clear-a", "1.0", CaseStatus.RUNNING));
    cache.put(createInstance("ns", "clear-b", "1.0", CaseStatus.RUNNING));

    cache.clear();

    assertThat(cache.getAll()).isEmpty();
  }

  @Test
  void reconstruction_setsMetadataFromRegistry() {
    CaseMetaModel meta = new CaseMetaModel();
    meta.setNamespace("reg-ns");
    meta.setName("reg-case");
    meta.setVersion("2.0");
    when(registry.findByIdentity("reg-ns", "reg-case", "2.0")).thenReturn(Optional.of(meta));

    CaseInstance instance = createInstance("reg-ns", "reg-case", "2.0", CaseStatus.RUNNING);
    cache.put(instance);

    CaseInstance retrieved = cache.get(instance.getUuid());
    assertThat(retrieved.getCaseMetaModel()).isNotNull();
    assertThat(retrieved.getCaseMetaModel().getNamespace()).isEqualTo("reg-ns");
    assertThat(retrieved.getCaseMetaModel().getName()).isEqualTo("reg-case");
  }

  private CaseInstance createInstance(String ns, String name, String version, CaseStatus status) {
    CaseInstance instance = new CaseInstance();
    instance.setUuid(UUID.randomUUID());
    instance.setState(status);
    instance.setVersion(1L);
    instance.setActorId("test-actor");
    instance.setLabels(Set.of("label-a"));
    instance.setTypes(Set.of("type-a"));
    instance.setCreatedAt(Instant.now());

    CaseMetaModel meta = new CaseMetaModel();
    meta.setNamespace(ns);
    meta.setName(name);
    meta.setVersion(version);
    instance.setCaseMetaModel(meta);

    ObjectNode snapshot = MAPPER.createObjectNode();
    snapshot.put("key", "value");
    instance.setContextSnapshot(snapshot);

    return instance;
  }

  private static SqliteConnectionFactory createInMemorySqlite() throws Exception {
    SqliteConnectionFactory factory = new SqliteConnectionFactory();
    var pathField = SqliteConnectionFactory.class.getDeclaredField("path");
    pathField.setAccessible(true);
    pathField.set(factory, ":memory:");
    var poolField = SqliteConnectionFactory.class.getDeclaredField("maxPoolSize");
    poolField.setAccessible(true);
    poolField.setInt(factory, 1);
    var timeoutField = SqliteConnectionFactory.class.getDeclaredField("busyTimeoutMs");
    timeoutField.setAccessible(true);
    timeoutField.setInt(factory, 5000);
    var initMethod = SqliteConnectionFactory.class.getDeclaredMethod("init");
    initMethod.setAccessible(true);
    initMethod.invoke(factory);
    return factory;
  }
}
