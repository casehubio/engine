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

import io.casehub.api.model.DefaultSubCaseCompletionStrategy;
import io.casehub.api.model.SubCase;
import io.casehub.api.model.SubCaseCompletionStrategy;

/**
 * Converter between generated schema model (io.casehub.model.SubCase) and API model
 * (io.casehub.api.model.SubCase).
 */
public final class SubCaseConverter {

  private SubCaseConverter() {}

  /**
   * Converts from generated schema model to API model.
   *
   * @param schemaModel generated model from JSON schema
   * @return API model with builder pattern
   */
  public static SubCase fromSchemaModel(io.casehub.model.SubCase schemaModel) {
    if (schemaModel == null) {
      return null;
    }

    SubCaseCompletionStrategy strategy =
        convertCompletionStrategy(schemaModel.getCompletionStrategy());

    return SubCase.builder()
        .namespace(schemaModel.getNamespace())
        .name(schemaModel.getName())
        .version(schemaModel.getVersion())
        .completionStrategy(strategy)
        .waitForCompletion(
            schemaModel.getWaitForCompletion() != null ? schemaModel.getWaitForCompletion() : true)
        .inputMapping(schemaModel.getInputMapping() != null ? schemaModel.getInputMapping() : ".")
        .outputMapping(schemaModel.getOutputMapping())
        .build();
  }

  /**
   * Converts from API model to generated schema model.
   *
   * @param apiModel API model with builder pattern
   * @return generated model for JSON serialization
   */
  public static io.casehub.model.SubCase toSchemaModel(SubCase apiModel) {
    if (apiModel == null) {
      return null;
    }

    io.casehub.model.SubCase schemaModel = new io.casehub.model.SubCase();
    schemaModel.setNamespace(apiModel.namespace());
    schemaModel.setName(apiModel.name());
    schemaModel.setVersion(apiModel.version());
    schemaModel.setCompletionStrategy(convertCompletionStrategy(apiModel.completionStrategy()));
    schemaModel.setWaitForCompletion(apiModel.waitForCompletion());
    schemaModel.setInputMapping(apiModel.inputMapping());
    schemaModel.setOutputMapping(apiModel.outputMapping());

    return schemaModel;
  }

  private static SubCaseCompletionStrategy convertCompletionStrategy(
      io.casehub.model.SubCase.CompletionStrategy schemaStrategy) {
    if (schemaStrategy == null
        || schemaStrategy == io.casehub.model.SubCase.CompletionStrategy.DEFAULT) {
      return new DefaultSubCaseCompletionStrategy();
    }
    // For CUSTOM strategy, return default implementation
    // Real custom strategies would be configured separately
    return new DefaultSubCaseCompletionStrategy();
  }

  private static io.casehub.model.SubCase.CompletionStrategy convertCompletionStrategy(
      SubCaseCompletionStrategy apiStrategy) {
    if (apiStrategy instanceof DefaultSubCaseCompletionStrategy) {
      return io.casehub.model.SubCase.CompletionStrategy.DEFAULT;
    }
    // Custom strategies map to CUSTOM
    return io.casehub.model.SubCase.CompletionStrategy.CUSTOM;
  }
}
