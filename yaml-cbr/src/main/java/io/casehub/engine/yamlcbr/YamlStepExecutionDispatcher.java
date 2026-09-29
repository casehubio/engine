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

import io.casehub.api.spi.YamlStepExecutionEvent;
import io.casehub.api.spi.YamlStepExecutionObserver;
import io.casehub.yaml.step.StepExecutionEvent;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.jboss.logging.Logger;

public class YamlStepExecutionDispatcher implements Consumer<StepExecutionEvent> {

  private static final Logger LOG = Logger.getLogger(YamlStepExecutionDispatcher.class);

  private final UUID caseId;
  private final String tenancyId;
  private final String caseType;
  private final Map<String, Object> contextSnapshot;
  private final String playbookName;
  private final String playbookVersion;
  private final List<YamlStepExecutionObserver> observers;

  public YamlStepExecutionDispatcher(
      UUID caseId,
      String tenancyId,
      String caseType,
      Map<String, Object> contextSnapshot,
      String playbookName,
      String playbookVersion,
      List<YamlStepExecutionObserver> observers) {
    this.caseId = caseId;
    this.tenancyId = tenancyId;
    this.caseType = caseType;
    this.contextSnapshot = contextSnapshot != null ? Map.copyOf(contextSnapshot) : Map.of();
    this.playbookName = playbookName;
    this.playbookVersion = playbookVersion;
    this.observers = List.copyOf(observers);
  }

  @Override
  public void accept(StepExecutionEvent platformEvent) {
    if (observers.isEmpty()) {
      return;
    }

    var engineEvent =
        new YamlStepExecutionEvent(
            caseId,
            tenancyId,
            caseType,
            platformEvent.actionName(),
            platformEvent.durationMs(),
            platformEvent.success(),
            platformEvent.metadata(),
            platformEvent.bindingType(),
            platformEvent.resultClassification(),
            platformEvent.executionEnvironment(),
            platformEvent.parentStepName(),
            playbookName,
            playbookVersion,
            null,
            contextSnapshot);

    for (YamlStepExecutionObserver observer : observers) {
      try {
        observer.onYamlStepExecution(engineEvent);
      } catch (Exception e) {
        LOG.warnf(
            e,
            "YamlStepExecutionObserver failed for caseId=%s action=%s",
            caseId,
            platformEvent.actionName());
      }
    }
  }
}
