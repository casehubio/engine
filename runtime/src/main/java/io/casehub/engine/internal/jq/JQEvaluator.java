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
package io.casehub.engine.internal.jq;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;

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
