package io.casehub.engine.internal.jq;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class JQEvaluator {

  private static final Scope ROOT_SCOPE;

  static {
    ROOT_SCOPE = Scope.newEmptyScope();
    BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, ROOT_SCOPE);
  }

  public ValidationResult eval(String jqExpr, JsonNode asNode) {
    try {
      Scope childScope = Scope.newChildScope(ROOT_SCOPE);
      JsonQuery query = JsonQuery.compile(jqExpr, Versions.JQ_1_6);

      List<JsonNode> out = new ArrayList<>();
      query.apply(childScope, asNode, out::add);

      return ValidationResult.ok(out);
    } catch (Exception e) {
      return ValidationResult.error(e.getClass().getSimpleName() + ": " + e.getMessage());
    }
  }
}
