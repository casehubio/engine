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
package io.casehub.engine.yamlcbr;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.casehub.api.spi.YamlStepExecutionEvent;
import io.casehub.api.spi.YamlStepExecutionObserver;
import io.casehub.yaml.step.ActionExecutionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class YamlStepExecutionDispatcherTest {

  @Test
  void augmentsPlatformEventWithCaseContext() {
    var caseId = UUID.randomUUID();
    var received = new ArrayList<YamlStepExecutionEvent>();
    YamlStepExecutionObserver observer = received::add;

    var dispatcher =
        new YamlStepExecutionDispatcher(
            caseId,
            "tenant-1",
            "incident-triage",
            Map.of("severity", "high"),
            "playbook-v1",
            "2.0",
            List.of(observer));

    var platformEvent =
        new ActionExecutionEvent(
            "rest-call",
            150L,
            true,
            Map.of("status", 200),
            "rest",
            "SUCCESS",
            null,
            null,
            null,
            null,
            null);

    dispatcher.accept(platformEvent);

    assertEquals(1, received.size());
    var event = received.get(0);
    assertEquals(caseId, event.caseId());
    assertEquals("tenant-1", event.tenancyId());
    assertEquals("incident-triage", event.caseType());
    assertEquals("rest-call", event.actionName());
    assertEquals(150L, event.durationMs());
    assertTrue(event.success());
    assertEquals("rest", event.bindingType());
    assertEquals("SUCCESS", event.resultClassification());
    assertEquals("playbook-v1", event.playbookName());
    assertEquals("2.0", event.playbookVersion());
    assertEquals(Map.of("severity", "high"), event.contextSnapshot());
  }

  @Test
  void dispatchesToMultipleObservers() {
    var received1 = new ArrayList<YamlStepExecutionEvent>();
    var received2 = new ArrayList<YamlStepExecutionEvent>();

    var dispatcher =
        new YamlStepExecutionDispatcher(
            UUID.randomUUID(),
            "t",
            "ct",
            Map.of(),
            null,
            null,
            List.of(received1::add, received2::add));

    dispatcher.accept(
        new ActionExecutionEvent(
            "action", 10L, true, Map.of(), "process", "SUCCESS", null, null, null, null, null));

    assertEquals(1, received1.size());
    assertEquals(1, received2.size());
  }

  @Test
  void continuesWhenObserverThrows() {
    var received = new ArrayList<YamlStepExecutionEvent>();
    YamlStepExecutionObserver thrower =
        e -> {
          throw new RuntimeException("boom");
        };

    var dispatcher =
        new YamlStepExecutionDispatcher(
            UUID.randomUUID(), "t", "ct", Map.of(), null, null, List.of(thrower, received::add));

    assertDoesNotThrow(
        () ->
            dispatcher.accept(
                new ActionExecutionEvent(
                    "action", 10L, true, Map.of(), "process", "SUCCESS", null, null, null, null,
                    null)));

    assertEquals(1, received.size());
  }

  @Test
  void skipsDispatchWhenNoObservers() {
    var dispatcher =
        new YamlStepExecutionDispatcher(
            UUID.randomUUID(), "t", "ct", Map.of(), null, null, List.of());

    assertDoesNotThrow(
        () ->
            dispatcher.accept(
                new ActionExecutionEvent(
                    "action", 10L, true, Map.of(), "process", "SUCCESS", null, null, null, null,
                    null)));
  }

  @Test
  void passesNullableFieldsFromPlatformEvent() {
    var received = new ArrayList<YamlStepExecutionEvent>();
    var dispatcher =
        new YamlStepExecutionDispatcher(
            UUID.randomUUID(), "t", "ct", Map.of(), null, null, List.of(received::add));

    var platformEvent =
        new ActionExecutionEvent(
            "action",
            10L,
            true,
            Map.of(),
            "process",
            "SUCCESS",
            "actor-1",
            "tenant-1",
            "input-hash",
            "parent-step",
            "prod");

    dispatcher.accept(platformEvent);

    var event = received.get(0);
    assertEquals("prod", event.executionEnvironment());
    assertEquals("parent-step", event.parentStepName());
  }
}
