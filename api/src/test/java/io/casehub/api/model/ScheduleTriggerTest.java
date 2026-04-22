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
package io.casehub.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ScheduleTrigger")
class ScheduleTriggerTest {

  @Nested
  @DisplayName("cron() factory")
  class CronFactory {

    @Test
    @DisplayName("creates cron-based trigger with valid expression")
    void createsCronTrigger() {
      ScheduleTrigger trigger = ScheduleTrigger.cron("0 */5 * * * ?");

      assertThat(trigger.getCron()).isEqualTo("0 */5 * * * ?");
      assertThat(trigger.getDelay()).isNull();
      assertThat(trigger.isCron()).isTrue();
      assertThat(trigger.isDelay()).isFalse();
    }

    @Test
    @DisplayName("rejects null cron expression")
    void rejectsNullCron() {
      assertThatThrownBy(() -> ScheduleTrigger.cron(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cronExpression must not be null");
    }

    @Test
    @DisplayName("toString() includes cron expression")
    void toStringIncludesCron() {
      ScheduleTrigger trigger = ScheduleTrigger.cron("0 */5 * * * ?");

      assertThat(trigger.toString()).contains("cron='0 */5 * * * ?'");
    }
  }

  @Nested
  @DisplayName("delay() factory")
  class DelayFactory {

    @Test
    @DisplayName("creates delay-based trigger with valid duration")
    void createsDelayTrigger() {
      ScheduleTrigger trigger = ScheduleTrigger.delay(Duration.ofMinutes(30));

      assertThat(trigger.getDelay()).isEqualTo(Duration.ofMinutes(30));
      assertThat(trigger.getCron()).isNull();
      assertThat(trigger.isDelay()).isTrue();
      assertThat(trigger.isCron()).isFalse();
    }

    @Test
    @DisplayName("rejects null delay")
    void rejectsNullDelay() {
      assertThatThrownBy(() -> ScheduleTrigger.delay(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("delay must not be null");
    }

    @Test
    @DisplayName("rejects negative delay")
    void rejectsNegativeDelay() {
      assertThatThrownBy(() -> ScheduleTrigger.delay(Duration.ofMinutes(-1)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("delay must be positive");
    }

    @Test
    @DisplayName("rejects zero delay")
    void rejectsZeroDelay() {
      assertThatThrownBy(() -> ScheduleTrigger.delay(Duration.ZERO))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("delay must be positive");
    }

    @Test
    @DisplayName("toString() includes delay duration")
    void toStringIncludesDelay() {
      ScheduleTrigger trigger = ScheduleTrigger.delay(Duration.ofMinutes(30));

      assertThat(trigger.toString()).contains("delay=PT30M");
    }
  }

  @Nested
  @DisplayName("type checking")
  class TypeChecking {

    @Test
    @DisplayName("isCron() returns true only for cron triggers")
    void isCronOnlyForCronTriggers() {
      ScheduleTrigger cronTrigger = ScheduleTrigger.cron("0 0 * * * ?");
      ScheduleTrigger delayTrigger = ScheduleTrigger.delay(Duration.ofSeconds(10));

      assertThat(cronTrigger.isCron()).isTrue();
      assertThat(delayTrigger.isCron()).isFalse();
    }

    @Test
    @DisplayName("isDelay() returns true only for delay triggers")
    void isDelayOnlyForDelayTriggers() {
      ScheduleTrigger cronTrigger = ScheduleTrigger.cron("0 0 * * * ?");
      ScheduleTrigger delayTrigger = ScheduleTrigger.delay(Duration.ofSeconds(10));

      assertThat(cronTrigger.isDelay()).isFalse();
      assertThat(delayTrigger.isDelay()).isTrue();
    }
  }

  @Nested
  @DisplayName("common use cases")
  class CommonUseCases {

    @Test
    @DisplayName("every 5 minutes cron expression")
    void everyFiveMinutes() {
      ScheduleTrigger trigger = ScheduleTrigger.cron("0 */5 * * * ?");

      assertThat(trigger.isCron()).isTrue();
      assertThat(trigger.getCron()).isEqualTo("0 */5 * * * ?");
    }

    @Test
    @DisplayName("30 minute delay")
    void thirtyMinuteDelay() {
      ScheduleTrigger trigger = ScheduleTrigger.delay(Duration.ofMinutes(30));

      assertThat(trigger.isDelay()).isTrue();
      assertThat(trigger.getDelay()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("2 hour deadline")
    void twoHourDeadline() {
      ScheduleTrigger trigger = ScheduleTrigger.delay(Duration.ofHours(2));

      assertThat(trigger.isDelay()).isTrue();
      assertThat(trigger.getDelay()).isEqualTo(Duration.ofHours(2));
    }

    @Test
    @DisplayName("daily at midnight cron")
    void dailyAtMidnight() {
      ScheduleTrigger trigger = ScheduleTrigger.cron("0 0 0 * * ?");

      assertThat(trigger.isCron()).isTrue();
      assertThat(trigger.getCron()).isEqualTo("0 0 0 * * ?");
    }
  }
}
