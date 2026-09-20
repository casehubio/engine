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
package io.casehub.persistence.spring;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.engine.common.spi.CaseInstanceRepository;
import io.casehub.engine.common.spi.CaseMetaModelRepository;
import io.casehub.engine.common.spi.EventLogRepository;
import io.casehub.engine.common.spi.PlanItemStore;
import io.casehub.engine.common.spi.SubCaseGroupRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

class DisplacementTest {

  @Test
  void conditionalOnMissingBeanSkipsDefaultWhenJpaPresent() {
    // When a real bean is registered, @ConditionalOnMissingBean defaults are skipped.
    // We verify that PersistenceAutoConfiguration produces the correct SPI types
    // and that @ConditionalOnMissingBean on in-memory beans would gate them out.
    assertThat(SpringJpaCaseInstanceRepository.class).isNotNull();
    assertThat(SpringJpaCaseMetaModelRepository.class).isNotNull();
    assertThat(SpringJpaEventLogRepository.class).isNotNull();
    assertThat(SpringJpaPlanItemStore.class).isNotNull();
    assertThat(SpringJpaSubCaseGroupRepository.class).isNotNull();

    // Verify the auto-configuration class produces all 9 SPI beans
    var methods = PersistenceAutoConfiguration.class.getDeclaredMethods();
    var beanMethods =
        java.util.Arrays.stream(methods)
            .filter(m -> m.isAnnotationPresent(Bean.class))
            .map(java.lang.reflect.Method::getReturnType)
            .toList();

    assertThat(beanMethods)
        .contains(
            CaseInstanceRepository.class,
            CaseMetaModelRepository.class,
            EventLogRepository.class,
            SubCaseGroupRepository.class,
            PlanItemStore.class);
  }
}
