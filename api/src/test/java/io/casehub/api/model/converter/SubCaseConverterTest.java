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
package io.casehub.api.model.converter;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.DefaultSubCaseCompletionStrategy;
import io.casehub.api.model.SubCase;
import org.junit.jupiter.api.Test;

class SubCaseConverterTest {

  @Test
  void fromSchemaModel_convertsAllFields() {
    io.casehub.model.SubCase schemaModel = new io.casehub.model.SubCase();
    schemaModel.setNamespace("test-namespace");
    schemaModel.setName("child-case");
    schemaModel.setVersion("1.0.0");
    schemaModel.setCompletionStrategy(io.casehub.model.SubCase.CompletionStrategy.DEFAULT);
    schemaModel.setWaitForCompletion(true);
    schemaModel.setInputMapping(".parentData");
    schemaModel.setOutputMapping(".childResult");

    SubCase apiModel = SubCaseConverter.fromSchemaModel(schemaModel);

    assertThat(apiModel.namespace()).isEqualTo("test-namespace");
    assertThat(apiModel.name()).isEqualTo("child-case");
    assertThat(apiModel.version()).isEqualTo("1.0.0");
    assertThat(apiModel.completionStrategy()).isInstanceOf(DefaultSubCaseCompletionStrategy.class);
    assertThat(apiModel.waitForCompletion()).isTrue();
    assertThat(apiModel.inputMapping()).isEqualTo(".parentData");
    assertThat(apiModel.outputMapping()).isEqualTo(".childResult");
  }

  @Test
  void fromSchemaModel_handlesDefaults() {
    io.casehub.model.SubCase schemaModel = new io.casehub.model.SubCase();
    schemaModel.setNamespace("ns");
    schemaModel.setName("name");
    schemaModel.setVersion("0.1.0");

    SubCase apiModel = SubCaseConverter.fromSchemaModel(schemaModel);

    assertThat(apiModel.waitForCompletion()).isTrue(); // default
    assertThat(apiModel.inputMapping()).isEqualTo("."); // default
    assertThat(apiModel.outputMapping()).isNull();
  }

  @Test
  void toSchemaModel_convertsAllFields() {
    SubCase apiModel =
        SubCase.builder()
            .namespace("test-namespace")
            .name("child-case")
            .version("1.0.0")
            .completionStrategy(new DefaultSubCaseCompletionStrategy())
            .waitForCompletion(false)
            .inputMapping(".customInput")
            .outputMapping(".customOutput")
            .build();

    io.casehub.model.SubCase schemaModel = SubCaseConverter.toSchemaModel(apiModel);

    assertThat(schemaModel.getNamespace()).isEqualTo("test-namespace");
    assertThat(schemaModel.getName()).isEqualTo("child-case");
    assertThat(schemaModel.getVersion()).isEqualTo("1.0.0");
    assertThat(schemaModel.getCompletionStrategy())
        .isEqualTo(io.casehub.model.SubCase.CompletionStrategy.DEFAULT);
    assertThat(schemaModel.getWaitForCompletion()).isFalse();
    assertThat(schemaModel.getInputMapping()).isEqualTo(".customInput");
    assertThat(schemaModel.getOutputMapping()).isEqualTo(".customOutput");
  }

  @Test
  void roundTrip_preservesData() {
    SubCase original =
        SubCase.builder()
            .namespace("org.example")
            .name("approval-process")
            .version("2.1.0")
            .waitForCompletion(true)
            .inputMapping(".request")
            .outputMapping(".decision")
            .build();

    io.casehub.model.SubCase schemaModel = SubCaseConverter.toSchemaModel(original);
    SubCase roundTripped = SubCaseConverter.fromSchemaModel(schemaModel);

    assertThat(roundTripped.namespace()).isEqualTo(original.namespace());
    assertThat(roundTripped.name()).isEqualTo(original.name());
    assertThat(roundTripped.version()).isEqualTo(original.version());
    assertThat(roundTripped.waitForCompletion()).isEqualTo(original.waitForCompletion());
    assertThat(roundTripped.inputMapping()).isEqualTo(original.inputMapping());
    assertThat(roundTripped.outputMapping()).isEqualTo(original.outputMapping());
  }

  @Test
  void fromSchemaModel_null_returnsNull() {
    assertThat(SubCaseConverter.fromSchemaModel(null)).isNull();
  }

  @Test
  void toSchemaModel_null_returnsNull() {
    assertThat(SubCaseConverter.toSchemaModel(null)).isNull();
  }
}
