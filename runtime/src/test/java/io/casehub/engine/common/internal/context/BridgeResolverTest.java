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
package io.casehub.engine.common.internal.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.casehub.api.context.CaseContext;
import io.casehub.api.context.ContextBridge;
import io.casehub.api.context.JacksonPojoBridge;
import io.casehub.api.context.MapBridge;
import io.casehub.api.model.CaseDefinition;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BridgeResolverTest {

  record TestPojo(String name) {}

  @Test
  void resolvesMapBridgeForMapInputType() {
    var resolver = new BridgeResolver(List.of(), noOpRegistry());
    var worker =
        Worker.builder()
            .name("w")
            .capabilityName("c")
            .function(input -> WorkerResult.of(input))
            .build();

    ContextBridge<?> bridge = resolver.resolve(worker, null);

    assertThat(bridge).isInstanceOf(MapBridge.class);
  }

  @Test
  void resolvesJacksonPojoBridgeForUnknownClass() {
    var resolver = new BridgeResolver(List.of(), noOpRegistry());
    var worker =
        Worker.builder()
            .name("w")
            .capabilityName("c")
            .<TestPojo>fn()
            .apply(p -> WorkerResult.of(Map.of()))
            .build();

    ContextBridge<?> bridge = resolver.resolve(worker, null);

    assertThat(bridge).isInstanceOf(JacksonPojoBridge.class);
    assertThat(bridge.contextType()).isEqualTo(TestPojo.class);
  }

  @Test
  void resolvesCdiDiscoveredBridgeByContextType() {
    ContextBridge<TestPojo> customBridge =
        new ContextBridge<>() {
          @Override
          public TestPojo initialise(CaseContext ctx, JsonNode in) {
            return null;
          }

          @Override
          public JsonNode serialise(TestPojo ctx) {
            return null;
          }

          @Override
          public TestPojo deserialise(JsonNode payload) {
            return null;
          }

          @Override
          public Class<TestPojo> contextType() {
            return TestPojo.class;
          }
        };
    var resolver = new BridgeResolver(List.of(customBridge), noOpRegistry());
    var worker =
        Worker.builder()
            .name("w")
            .capabilityName("c")
            .<TestPojo>fn()
            .apply(p -> WorkerResult.of(Map.of()))
            .build();

    ContextBridge<?> bridge = resolver.resolve(worker, null);

    assertThat(bridge).isSameAs(customBridge);
  }

  @Test
  void resolvesCaseDefinitionDefaultWhenInputTypeMatches() {
    var defaultBridge = new JacksonPojoBridge<>(TestPojo.class);
    var definition =
        CaseDefinition.builder()
            .namespace("test")
            .name("test")
            .version("1.0")
            .defaultWorkerBridge(defaultBridge)
            .build();
    var resolver = new BridgeResolver(List.of(), noOpRegistry());
    var worker =
        Worker.builder()
            .name("w")
            .capabilityName("c")
            .<TestPojo>fn()
            .apply(p -> WorkerResult.of(Map.of()))
            .build();

    ContextBridge<?> bridge = resolver.resolve(worker, definition);

    assertThat(bridge).isSameAs(defaultBridge);
  }

  @Test
  void skipsDefaultBridgeWhenInputTypeDoesNotMatch() {
    var defaultBridge = new JacksonPojoBridge<>(TestPojo.class);
    var definition =
        CaseDefinition.builder()
            .namespace("test")
            .name("test")
            .version("1.0")
            .defaultWorkerBridge(defaultBridge)
            .build();
    var resolver = new BridgeResolver(List.of(), noOpRegistry());
    var worker =
        Worker.builder()
            .name("w")
            .capabilityName("c")
            .function(input -> WorkerResult.of(input))
            .build();

    ContextBridge<?> bridge = resolver.resolve(worker, definition);

    assertThat(bridge).isInstanceOf(MapBridge.class);
  }

  @Test
  void resolveByTypeNameFallsBackToMapBridgeForNull() {
    var resolver = new BridgeResolver(List.of(), noOpRegistry());
    assertThat(resolver.resolveByTypeName(null)).isInstanceOf(MapBridge.class);
  }

  @Test
  void resolveByTypeNameFindsMatchingCdiBridge() {
    ContextBridge<TestPojo> customBridge =
        new ContextBridge<>() {
          @Override
          public TestPojo initialise(CaseContext ctx, JsonNode in) {
            return null;
          }

          @Override
          public JsonNode serialise(TestPojo ctx) {
            return null;
          }

          @Override
          public TestPojo deserialise(JsonNode payload) {
            return null;
          }

          @Override
          public Class<TestPojo> contextType() {
            return TestPojo.class;
          }
        };
    var resolver = new BridgeResolver(List.of(customBridge), noOpRegistry());

    ContextBridge<?> bridge = resolver.resolveByTypeName(TestPojo.class.getName());
    assertThat(bridge).isSameAs(customBridge);
  }

  @Test
  void resolveByTypeNameCreatesJacksonBridgeForKnownClass() {
    var resolver = new BridgeResolver(List.of(), noOpRegistry());
    ContextBridge<?> bridge = resolver.resolveByTypeName(TestPojo.class.getName());
    assertThat(bridge).isInstanceOf(JacksonPojoBridge.class);
    assertThat(bridge.contextType()).isEqualTo(TestPojo.class);
  }

  @Test
  void resolveByTypeNameFallsBackToMapBridgeForUnknownClass() {
    var resolver = new BridgeResolver(List.of(), noOpRegistry());
    assertThat(resolver.resolveByTypeName("com.nonexistent.FooBar")).isInstanceOf(MapBridge.class);
  }

  @Test
  void caseDefinitionDefaultTakesPriorityOverCdi() {
    var cdiPojoBridge = new JacksonPojoBridge<>(TestPojo.class);
    var defPojoBridge = new JacksonPojoBridge<>(TestPojo.class);
    var definition =
        CaseDefinition.builder()
            .namespace("test")
            .name("test")
            .version("1.0")
            .defaultWorkerBridge(defPojoBridge)
            .build();
    var resolver = new BridgeResolver(List.of(cdiPojoBridge), noOpRegistry());
    var worker =
        Worker.builder()
            .name("w")
            .capabilityName("c")
            .<TestPojo>fn()
            .apply(p -> WorkerResult.of(Map.of()))
            .build();

    ContextBridge<?> bridge = resolver.resolve(worker, definition);

    assertThat(bridge).isSameAs(defPojoBridge);
  }

  private static DataRefRegistry noOpRegistry() {
    return new DataRefRegistry(List.of());
  }
}
