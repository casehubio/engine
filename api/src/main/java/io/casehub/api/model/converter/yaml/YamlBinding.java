package io.casehub.api.model.converter.yaml;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.casehub.api.model.Trigger;
import io.casehub.api.model.converter.deser.ExpressionEvaluatorDeserializer;
import io.casehub.api.model.converter.deser.TriggerDeserializer;
import io.casehub.platform.api.expression.ExpressionEvaluator;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YamlBinding(
    String name,
    String capability,
    @JsonDeserialize(using = TriggerDeserializer.class) Trigger on,
    @JsonDeserialize(using = ExpressionEvaluatorDeserializer.class) ExpressionEvaluator when,
    @JsonDeserialize(using = ExpressionEvaluatorDeserializer.class)
        ExpressionEvaluator inputProjectionOverride,
    String outcomePolicy,
    String conflictResolverStrategy,
    String lifecycleScope,
    String participation,
    String executionMode,
    @JsonAlias("replanAfter") String replanHint,
    JsonNode exchangeProjection,
    String produces,
    String consumes,
    List<String> producedKeys,
    List<String> contingency,
    Map<String, Object> contextWrite,
    Map<String, Object> signal,
    String sideEffectClassification,
    List<String> permissionIntent,
    YamlHumanTaskTarget humanTask,
    YamlJudgmentTarget judgment,
    YamlSubCaseTarget subCase,
    YamlRecoveryOverride recoveryOverride) {

  public YamlBinding {
    if (producedKeys == null) producedKeys = List.of();
    if (contingency == null) contingency = List.of();
    if (contextWrite == null) contextWrite = Map.of();
    if (permissionIntent == null) permissionIntent = List.of();
  }
}
