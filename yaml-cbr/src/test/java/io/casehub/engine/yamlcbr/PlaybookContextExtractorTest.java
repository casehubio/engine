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

import static org.junit.jupiter.api.Assertions.*;

import io.casehub.api.model.cbr.CbrConfig;
import io.casehub.api.spi.YamlStepExecutionEvent;
import io.casehub.neocortex.memory.cbr.FeatureValue;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlaybookContextExtractorTest {

  @Test
  void interfaceDefinesSupportsAndExtract() {
    PlaybookContextExtractor extractor =
        new PlaybookContextExtractor() {
          @Override
          public boolean supports(CbrConfig config) {
            return "test-domain".equals(config.domain());
          }

          @Override
          public Map<String, FeatureValue> extract(YamlStepExecutionEvent event) {
            return Map.of("custom", FeatureValue.of("value"));
          }
        };

    var config = CbrConfig.builder().feature("f1", ".x").domain("test-domain").build();

    assertTrue(extractor.supports(config));

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
            Map.of());

    Map<String, FeatureValue> features = extractor.extract(event);
    assertEquals(1, features.size());
    assertNotNull(features.get("custom"));
  }

  @Test
  void supportsReturnsFalseForNonMatchingDomain() {
    PlaybookContextExtractor extractor =
        new PlaybookContextExtractor() {
          @Override
          public boolean supports(CbrConfig config) {
            return "my-domain".equals(config.domain());
          }

          @Override
          public Map<String, FeatureValue> extract(YamlStepExecutionEvent event) {
            return Map.of();
          }
        };

    var config = CbrConfig.builder().feature("f1", ".x").domain("other-domain").build();

    assertFalse(extractor.supports(config));
  }
}
