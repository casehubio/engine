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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.api.context.CaseContext;
import io.casehub.api.engine.ExpressionEngineRegistry;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.api.spi.WorkerFunctionProviderRegistry;
import io.casehub.platform.api.expression.ExpressionEvaluator;
import java.io.IOException;
import java.io.InputStream;
import org.jboss.logging.Logger;

/**
 * Centralized YAML marshaller for CaseDefinition.
 *
 * <p>Reads YAML CaseDefinition files and deserializes directly to API models via {@link
 * CaseDefinitionModule}. Post-processing of worker functions and GOAP shorthands is handled by
 * {@link CaseDefinitionPostProcessor}.
 *
 * <p>Use {@link #load(InputStream, ObjectMapper, ExpressionEngineRegistry,
 * WorkerFunctionProviderRegistry)} in CDI contexts. Use {@link #load(InputStream)} for non-CDI
 * contexts (tests, tooling) — JQ only.
 */
public final class CaseDefinitionYamlMapper {

  private static final Logger LOG = Logger.getLogger(CaseDefinitionYamlMapper.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** JQ-only registry for non-CDI contexts. Does not support custom expression languages. */
  private static final ExpressionEngineRegistry JQ_ONLY =
      new ExpressionEngineRegistry() {
        @Override
        public ExpressionEvaluator create(final String expression, final String expressionLang) {
          if (!JQExpressionEvaluator.TYPE.equals(expressionLang)) {
            throw new IllegalArgumentException(
                "No CDI registry available; only '"
                    + JQExpressionEvaluator.TYPE
                    + "' is supported without injection. Got: "
                    + expressionLang);
          }
          return new JQExpressionEvaluator(expression);
        }

        @Override
        public void assertLanguageSupported(final String expressionLang) {
          if (!JQExpressionEvaluator.TYPE.equals(expressionLang)) {
            throw new IllegalArgumentException(
                "No CDI registry available; only '"
                    + JQExpressionEvaluator.TYPE
                    + "' is supported without injection. Got: "
                    + expressionLang);
          }
        }

        @Override
        public boolean evaluate(final ExpressionEvaluator evaluator, final CaseContext context) {
          throw new UnsupportedOperationException(
              "JQ_ONLY loading registry does not support evaluation");
        }

        @Override
        public boolean evaluate(final ExpressionEvaluator evaluator, final JsonNode asNode) {
          throw new UnsupportedOperationException(
              "JQ_ONLY loading registry does not support evaluation");
        }

        @Override
        public void validate(final ExpressionEvaluator evaluator) {
          // no-op: loading-only registry; validation occurs through the CDI path
          // during case definition registration in DefaultCaseDefinitionRegistry
        }

        @Override
        public java.util.List<JsonNode> transform(
            final ExpressionEvaluator evaluator, final JsonNode input) {
          throw new UnsupportedOperationException(
              "JQ_ONLY loading registry does not support transformation");
        }

        @Override
        public java.util.Optional<String> extractString(
            final ExpressionEvaluator evaluator, final CaseContext context) {
          throw new UnsupportedOperationException(
              "JQ_ONLY loading registry does not support string extraction");
        }
      };

  /**
   * Empty WorkerFunctionProviderRegistry for non-CDI contexts. Returns null for all worker nodes,
   * causing mapper to use API-local construction (agent, sync).
   */
  private static final WorkerFunctionProviderRegistry EMPTY_PROVIDERS = rawWorkerNode -> null;

  private CaseDefinitionYamlMapper() {}

  /**
   * Loads a CaseDefinition from a YAML InputStream using the CDI-managed ObjectMapper and
   * ExpressionEngineRegistry. Supports all registered expression languages.
   *
   * @param yamlStream InputStream containing YAML CaseDefinition
   * @param objectMapper ObjectMapper configured for YAML (with config/secret placeholder support)
   * @param registry ExpressionEngineRegistry for creating evaluators from YAML expression strings
   * @param providerRegistry WorkerFunctionProviderRegistry for SDK-dependent worker construction
   * @return API model CaseDefinition
   * @throws IOException if reading or parsing fails
   */
  public static CaseDefinition load(
      final InputStream yamlStream,
      final ObjectMapper objectMapper,
      final ExpressionEngineRegistry registry,
      final WorkerFunctionProviderRegistry providerRegistry)
      throws IOException {
    if (yamlStream == null) {
      throw new IllegalArgumentException("InputStream cannot be null");
    }
    final byte[] bytes = yamlStream.readAllBytes();
    final JsonNode rawNode = objectMapper.readTree(bytes);
    final JsonNode processedNode = flattenExpressionOverrides(rawNode, objectMapper);
    final com.fasterxml.jackson.databind.ObjectMapper moduleMapper =
        objectMapper
            .copy()
            .registerModule(new CaseDefinitionModule(registry != null ? registry : JQ_ONLY))
            .disable(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    moduleMapper.addHandler(UnknownPropertyWarningHandler.INSTANCE);
    final CaseDefinition def = moduleMapper.convertValue(processedNode, CaseDefinition.class);
    new CaseDefinitionPostProcessor(providerRegistry != null ? providerRegistry : EMPTY_PROVIDERS)
        .apply(def, rawNode);
    return def;
  }

  /**
   * Loads a CaseDefinition from a pre-merged JsonNode. For use with the YAML overlay/merge pipeline
   * where base and overlay documents have already been merged via YamlMerger.
   *
   * @param mergedNode pre-merged JsonNode containing the complete case definition
   * @param objectMapper ObjectMapper for type conversion
   * @param registry ExpressionEngineRegistry (nullable — falls back to JQ-only)
   * @param providerRegistry WorkerFunctionProviderRegistry (nullable — falls back to no-op)
   * @return API model CaseDefinition
   */
  public static CaseDefinition load(
      final JsonNode mergedNode,
      final ObjectMapper objectMapper,
      final ExpressionEngineRegistry registry,
      final WorkerFunctionProviderRegistry providerRegistry) {
    if (mergedNode == null) {
      throw new IllegalArgumentException("JsonNode cannot be null");
    }
    final JsonNode processedNode = flattenExpressionOverrides(mergedNode, objectMapper);
    final com.fasterxml.jackson.databind.ObjectMapper moduleMapper =
        objectMapper
            .copy()
            .registerModule(new CaseDefinitionModule(registry != null ? registry : JQ_ONLY))
            .disable(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    moduleMapper.addHandler(UnknownPropertyWarningHandler.INSTANCE);
    final CaseDefinition def = moduleMapper.convertValue(processedNode, CaseDefinition.class);
    new CaseDefinitionPostProcessor(providerRegistry != null ? providerRegistry : EMPTY_PROVIDERS)
        .apply(def, mergedNode);
    return def;
  }

  /**
   * Resolves a YAML expression node to an {@link ExpressionEvaluator}.
   *
   * <p>Accepts two forms:
   *
   * <ul>
   *   <li>String: {@code ".amount > 1000"} — uses {@code defaultLang}
   *   <li>Single-key map: {@code {mvel: "transaction.amount > 1000"}} — language is the map key
   * </ul>
   *
   * @param node raw YAML node (null or NullNode returns null)
   * @param registry registry for creating evaluators
   * @param defaultLang language to use when {@code node} is a plain string
   * @return ExpressionEvaluator, or null if node is absent/null
   */
  public static ExpressionEvaluator resolveExpression(
      final JsonNode node, final ExpressionEngineRegistry registry, final String defaultLang) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      return registry.create(node.asText(), defaultLang);
    }
    if (node.isObject()) {
      if (node.size() == 0) {
        throw new IllegalArgumentException(
            "Expression must be a single-key map {lang: expr}, got empty map");
      }
      if (node.size() > 1) {
        throw new IllegalArgumentException(
            "Expression must be a single-key map {lang: expr}, got " + node.size() + " keys");
      }
      java.util.Map.Entry<String, JsonNode> entry = node.fields().next();
      return registry.create(entry.getValue().asText(), entry.getKey());
    }
    throw new IllegalArgumentException(
        "Expression must be a string or single-key map {lang: expr}, got "
            + node.getNodeType().name());
  }

  private static JsonNode flattenExpressionOverrides(JsonNode node, ObjectMapper mapper) {
    if (!node.has("labelRules")) {
      return node;
    }
    JsonNode rules = node.get("labelRules");
    boolean needsFlatten = false;
    for (JsonNode rule : rules) {
      if (rule.has("when") && rule.get("when").isObject()) {
        needsFlatten = true;
        break;
      }
    }
    if (!needsFlatten) {
      return node;
    }
    com.fasterxml.jackson.databind.node.ObjectNode copy = mapper.valueToTree(node);
    com.fasterxml.jackson.databind.node.ArrayNode rulesArr =
        (com.fasterxml.jackson.databind.node.ArrayNode) copy.get("labelRules");
    for (int i = 0; i < rulesArr.size(); i++) {
      com.fasterxml.jackson.databind.node.ObjectNode rule =
          (com.fasterxml.jackson.databind.node.ObjectNode) rulesArr.get(i);
      JsonNode when = rule.get("when");
      if (when != null && when.isObject() && when.size() == 1) {
        rule.put("when", when.fields().next().getValue().asText());
      }
    }
    return copy;
  }

  /**
   * Loads a CaseDefinition from a YAML InputStream using a plain ObjectMapper and JQ-only
   * expression support.
   *
   * <p>For non-CDI contexts (tests, tooling). Does not support custom expression languages — use
   * {@link #load(InputStream, ObjectMapper, ExpressionEngineRegistry,
   * WorkerFunctionProviderRegistry)} in CDI deployments.
   *
   * @param yamlStream InputStream containing YAML CaseDefinition
   * @return API model CaseDefinition
   * @throws IOException if reading or parsing fails
   */
  public static CaseDefinition load(final InputStream yamlStream) throws IOException {
    return load(yamlStream, new ObjectMapper(new YAMLFactory()), JQ_ONLY, EMPTY_PROVIDERS);
  }
}
