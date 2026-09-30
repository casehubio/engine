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
package io.casehub.engine.spring.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.engine.common.spi.CaseInstanceRepository;
import io.casehub.engine.common.spi.EventLogRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpringBootCompositionTest {

  @Autowired private ApplicationContext context;

  @Autowired private ObjectMapper objectMapper;

  @LocalServerPort private int port;

  @Test
  void contextLoads() {}

  @Test
  void commonBeansRegistered() {
    assertThat(
            context.getBean(
                io.casehub.engine.common.internal.channel.InMemoryDataChannelFactory.class))
        .isNotNull();
    assertThat(
            context.getBean(io.casehub.engine.common.internal.executor.WorkerExecutionConfig.class))
        .isNotNull();
    assertThat(context.getBean(io.casehub.engine.common.internal.context.DataRefRegistry.class))
        .isNotNull();
    assertThat(context.getBean(io.casehub.engine.common.internal.context.BridgeResolver.class))
        .isNotNull();
  }

  @Test
  void jpaBeansRegistered() {
    assertThat(context.getBean(CaseInstanceRepository.class)).isNotNull();
    assertThat(context.getBean(EventLogRepository.class)).isNotNull();
    assertThat(context.getBean(io.casehub.engine.common.spi.CaseMetaModelRepository.class))
        .isNotNull();
    assertThat(context.getBean(io.casehub.engine.common.spi.PlanItemStore.class)).isNotNull();
  }

  @Test
  void healthCheckReturnsUp() throws Exception {
    var client = HttpClient.newHttpClient();
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/actuator/health"))
            .GET()
            .build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("UP");
  }

  @Test
  void jacksonBridgeActive() {
    assertThat(objectMapper).isInstanceOf(ObjectMapper.class);
    assertThat(objectMapper.getClass().getName()).startsWith("com.fasterxml.jackson");
  }
}
