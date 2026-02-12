package io.casehub.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JType;
import org.jsonschema2pojo.rules.Rule;
import org.jsonschema2pojo.rules.RuleFactory;

public class CasehubRuleFactory extends RuleFactory {

    private static final String WORKER_FQCN = "io.casehub.model.Worker";

    @Override
    public Rule<JClassContainer, JType> getSchemaRule() {
        Rule<JClassContainer, JType> defaultRule = super.getSchemaRule();
        return (nodeName, node, parent, jPackage, schema) -> {
            if (matches(nodeName, node, "Worker")) {
                return jPackage.owner().directClass(WORKER_FQCN);
            }
            return defaultRule.apply(nodeName, node, parent, jPackage, schema);
        };
    }

    private static boolean matches(String nodeName, JsonNode node, String typeName) {
        if (typeName.equals(nodeName)) {
            return true;
        }
        if (node.has("$ref")) {
            String ref = node.get("$ref").asText();
            return ref.endsWith("/" + typeName) || ref.equals("#/$defs/" + typeName);
        }
        return false;
    }
}
