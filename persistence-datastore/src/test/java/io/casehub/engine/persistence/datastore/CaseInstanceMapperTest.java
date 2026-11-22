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
package io.casehub.engine.persistence.datastore;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.api.model.CaseStatus;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.internal.model.CaseMetaModel;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CaseInstanceMapperTest {

  @Test
  void roundTrip_preservesAllFields() {
    CaseInstance original = createTestInstance();

    ObjectNode record = CaseInstanceMapper.toRecord(original);
    CaseInstance restored = CaseInstanceMapper.fromRecord(record);

    assertThat(restored.getUuid()).isEqualTo(original.getUuid());
    assertThat(restored.getState()).isEqualTo(original.getState());
    assertThat(restored.getActorId()).isEqualTo(original.getActorId());
    assertThat(restored.getCreatedAt()).isEqualTo(original.getCreatedAt());
    assertThat(restored.getCaseMetaModel().getNamespace())
        .isEqualTo(original.getCaseMetaModel().getNamespace());
    assertThat(restored.getCaseMetaModel().getName())
        .isEqualTo(original.getCaseMetaModel().getName());
    assertThat(restored.getCaseMetaModel().getVersion())
        .isEqualTo(original.getCaseMetaModel().getVersion());
    assertThat(restored.getLabels()).isEqualTo(original.getLabels());
    assertThat(restored.getTypes()).isEqualTo(original.getTypes());
  }

  @Test
  void roundTrip_nullActorId() {
    CaseInstance original = createTestInstance();
    original.setActorId(null);

    ObjectNode record = CaseInstanceMapper.toRecord(original);
    CaseInstance restored = CaseInstanceMapper.fromRecord(record);

    assertThat(restored.getActorId()).isNull();
  }

  @Test
  void roundTrip_emptyLabelsAndTypes() {
    CaseInstance original = createTestInstance();
    original.getLabels().clear();
    original.getTypes().clear();

    ObjectNode record = CaseInstanceMapper.toRecord(original);
    CaseInstance restored = CaseInstanceMapper.fromRecord(record);

    assertThat(restored.getLabels()).isEmpty();
    assertThat(restored.getTypes()).isEmpty();
  }

  @Test
  void toRecord_includesQueryableFields() {
    CaseInstance instance = createTestInstance();
    ObjectNode record = CaseInstanceMapper.toRecord(instance);

    assertThat(record.get("uuid").asText()).isEqualTo(instance.getUuid().toString());
    assertThat(record.get("state").asText()).isEqualTo("RUNNING");
    assertThat(record.get("namespace").asText()).isEqualTo("test-ns");
    assertThat(record.get("name").asText()).isEqualTo("test-case");
  }

  @Test
  void roundTrip_parentReferences() {
    CaseInstance original = createTestInstance();
    UUID parentId = UUID.randomUUID();
    UUID planItemId = UUID.randomUUID();
    original.setParentCaseId(parentId);
    original.setParentPlanItemId(planItemId);
    original.setWaitingForWorkId("work-123");

    ObjectNode record = CaseInstanceMapper.toRecord(original);
    CaseInstance restored = CaseInstanceMapper.fromRecord(record);

    assertThat(restored.getParentCaseId()).isEqualTo(parentId);
    assertThat(restored.getParentPlanItemId()).isEqualTo(planItemId);
    assertThat(restored.getWaitingForWorkId()).isEqualTo("work-123");
  }

  private CaseInstance createTestInstance() {
    CaseMetaModel meta = new CaseMetaModel();
    meta.setNamespace("test-ns");
    meta.setName("test-case");
    meta.setVersion("1.0");

    CaseInstance instance = new CaseInstance();
    instance.setCaseMetaModel(meta);
    instance.setUuid(UUID.randomUUID());
    instance.setState(CaseStatus.RUNNING);
    instance.setActorId("actor-1");
    instance.setCreatedAt(Instant.now());
    instance.getLabels().addAll(Set.of("urgent", "customer"));
    instance.getTypes().addAll(Set.of("helpdesk", "ticket"));
    return instance;
  }
}
