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
package io.casehub.engine.scheduler.dbscheduler;

final class CronUtils {

  private static final com.github.kagkarlsson.shaded.cronutils.parser.CronParser PARSER =
      new com.github.kagkarlsson.shaded.cronutils.parser.CronParser(
          com.github.kagkarlsson.shaded.cronutils.model.definition.CronDefinitionBuilder
              .instanceDefinitionFor(
                  com.github.kagkarlsson.shaded.cronutils.model.CronType.QUARTZ));

  private CronUtils() {}

  static java.util.Optional<java.time.Instant> nextExecution(String cronExpression) {
    com.github.kagkarlsson.shaded.cronutils.model.Cron cron = PARSER.parse(cronExpression);
    cron.validate();
    com.github.kagkarlsson.shaded.cronutils.model.time.ExecutionTime executionTime =
        com.github.kagkarlsson.shaded.cronutils.model.time.ExecutionTime.forCron(cron);
    return executionTime
        .nextExecution(java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault()))
        .map(java.time.ZonedDateTime::toInstant);
  }
}
