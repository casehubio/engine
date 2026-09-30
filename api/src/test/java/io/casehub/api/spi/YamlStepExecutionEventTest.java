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
package io.casehub.api.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class YamlStepExecutionEventTest {

  @Test
  void constructsWithAllFields() {
    var caseId = UUID.randomUUID();
    var event =
        new YamlStepExecutionEvent(
            caseId,
            "tenant-1",
            "incident-triage",
            "rest-call",
            150L,
            true,
            Map.of("status", 200),
            "rest",
            "SUCCESS",
            "prod",
            "parent-step",
            "playbook-v1",
            "1.0.0",
            "trace-abc",
            Map.of("severity", "high"));

    assertEquals(caseId, event.caseId());
    assertEquals("tenant-1", event.tenancyId());
    assertEquals("incident-triage", event.caseType());
    assertEquals("rest-call", event.actionName());
    assertEquals(150L, event.durationMs());
    assertTrue(event.success());
    assertEquals("rest", event.bindingType());
    assertEquals("SUCCESS", event.resultClassification());
    assertEquals("prod", event.executionEnvironment());
    assertEquals("parent-step", event.parentStepName());
    assertEquals("playbook-v1", event.playbookName());
    assertEquals("1.0.0", event.playbookVersion());
    assertEquals("trace-abc", event.traceId());
    assertEquals(Map.of("severity", "high"), event.contextSnapshot());
  }

  @Test
  void nullableFieldsAcceptNull() {
    var event =
        new YamlStepExecutionEvent(
            UUID.randomUUID(),
            "t",
            "ct",
            "action",
            10L,
            false,
            Map.of(),
            "process",
            "FAILURE",
            null,
            null,
            null,
            null,
            null,
            Map.of());

    assertNull(event.executionEnvironment());
    assertNull(event.parentStepName());
    assertNull(event.playbookName());
    assertNull(event.playbookVersion());
    assertNull(event.traceId());
  }

  @Test
  void contextSnapshotIsDefensivelyCopied() {
    var mutableMap = new java.util.HashMap<String, Object>();
    mutableMap.put("key", "val");
    var event =
        new YamlStepExecutionEvent(
            UUID.randomUUID(),
            "t",
            "ct",
            "action",
            10L,
            true,
            Map.of(),
            "process",
            "SUCCESS",
            null,
            null,
            null,
            null,
            null,
            mutableMap);

    mutableMap.put("new-key", "new-val");
    assertFalse(event.contextSnapshot().containsKey("new-key"));
  }

  @Test
  void metadataIsDefensivelyCopied() {
    var mutableMeta = new java.util.HashMap<String, Object>();
    mutableMeta.put("k", "v");
    var event =
        new YamlStepExecutionEvent(
            UUID.randomUUID(),
            "t",
            "ct",
            "action",
            10L,
            true,
            mutableMeta,
            "process",
            "SUCCESS",
            null,
            null,
            null,
            null,
            null,
            Map.of());

    mutableMeta.put("extra", "x");
    assertFalse(event.metadata().containsKey("extra"));
  }
}
