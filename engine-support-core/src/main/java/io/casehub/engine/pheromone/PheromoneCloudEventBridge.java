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
package io.casehub.engine.pheromone;

import io.casehub.engine.common.spi.event.PheromoneStateChangedEvent;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

@ApplicationScoped
public class PheromoneCloudEventBridge {

  static final String CE_TYPE_DEPOSITED = "io.casehub.pheromone.deposited";
  static final String CE_TYPE_EXPIRED = "io.casehub.pheromone.expired";
  static final String CE_SOURCE = "/casehub/engine/pheromone";

  private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
      new com.fasterxml.jackson.databind.ObjectMapper();

  @Inject Event<CloudEvent> cloudEventEmitter;

  void onPheromoneStateChanged(@ObservesAsync PheromoneStateChangedEvent event) {
    String ceType =
        event.type() == PheromoneStateChangedEvent.Type.DEPOSITED
            ? CE_TYPE_DEPOSITED
            : CE_TYPE_EXPIRED;

    byte[] payload;
    try {
      payload =
          MAPPER.writeValueAsBytes(
              java.util.Map.of(
                  "caseId", event.caseId().toString(),
                  "signal", event.signalName(),
                  "strength", event.strength(),
                  "reinforcements", event.reinforcementCount(),
                  "source", event.source() != null ? event.source() : ""));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize pheromone event", e);
    }

    CloudEvent ce =
        CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withType(ceType)
            .withSource(URI.create(CE_SOURCE))
            .withTime(OffsetDateTime.now())
            .withDataContentType("application/json")
            .withData(payload)
            .withExtension("caseid", event.caseId().toString())
            .withExtension("signalname", event.signalName())
            .build();

    cloudEventEmitter.fireAsync(ce);
  }
}
