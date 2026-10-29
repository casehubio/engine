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
package io.casehub.engine.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class PlaybookWorkerFunctionProviderTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void handles_workers_with_steps_block() {
    var provider = new PlaybookWorkerFunctionProvider();
    ObjectNode node = MAPPER.createObjectNode();
    node.putArray("steps");
    assertThat(provider.handles(node)).isTrue();
  }

  @Test
  void does_not_handle_workers_without_steps_block() {
    var provider = new PlaybookWorkerFunctionProvider();
    ObjectNode node = MAPPER.createObjectNode().put("name", "test");
    assertThat(provider.handles(node)).isFalse();
  }

  @Test
  void does_not_handle_workers_with_do_block() {
    var provider = new PlaybookWorkerFunctionProvider();
    ObjectNode node = MAPPER.createObjectNode();
    node.putArray("do");
    assertThat(provider.handles(node)).isFalse();
  }

  @Test
  void creates_playbook_worker_function_from_steps() {
    var provider = new PlaybookWorkerFunctionProvider();
    ObjectNode node = MAPPER.createObjectNode();
    ObjectNode step = MAPPER.createObjectNode();
    step.putObject("rest-request").put("method", "GET").put("url", "/api/test");
    node.putArray("steps").add(step);

    var fn = provider.create(node);
    assertThat(fn).isInstanceOf(PlaybookWorkerFunction.class);
    PlaybookWorkerFunction pbFn = (PlaybookWorkerFunction) fn;
    assertThat(pbFn.workflow()).isNotNull();
    assertThat(pbFn.compilationResult()).isNotNull();
    assertThat(pbFn.compilationResult().sourceMap()).isNotNull();
  }

  @Test
  void throws_on_invalid_playbook_syntax() {
    var provider = new PlaybookWorkerFunctionProvider();
    ObjectNode node = MAPPER.createObjectNode();
    node.putArray("steps").add("not-a-valid-step");

    assertThatThrownBy(() -> provider.create(node)).isInstanceOf(IllegalArgumentException.class);
  }
}
