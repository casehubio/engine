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
package io.casehub.api.acl;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.engine.rest.EngineCaseApi;
import io.casehub.api.engine.rest.EngineCaseControlApi;
import io.casehub.api.engine.rest.EngineCaseDefinitionApi;
import io.casehub.api.engine.rest.EngineEventLogApi;
import io.casehub.api.engine.rest.EnginePlanApi;
import io.casehub.platform.api.mcp.PathParam;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AclCheckAnnotationCoverageTest {

  private static final List<Class<?>> API_INTERFACES =
      List.of(
          EngineCaseApi.class,
          EngineCaseControlApi.class,
          EngineCaseDefinitionApi.class,
          EngineEventLogApi.class,
          EnginePlanApi.class);

  private static boolean hasCaseIdPathParam(Method m) {
    for (Parameter p : m.getParameters()) {
      if (p.isAnnotationPresent(PathParam.class) && UUID.class.equals(p.getType())) {
        return true;
      }
    }
    return false;
  }

  @Test
  void everyMethodWithCaseIdParam_hasAclCheck() {
    List<String> missing = new ArrayList<>();
    for (Class<?> api : API_INTERFACES) {
      for (Method m : api.getDeclaredMethods()) {
        if (hasCaseIdPathParam(m) && m.getAnnotation(AclCheck.class) == null) {
          missing.add(api.getSimpleName() + "." + m.getName());
        }
      }
    }
    assertThat(missing).as("Methods with @PathParam UUID but no @AclCheck").isEmpty();
  }

  @Test
  void everyAclCheck_hasValidResourceType() {
    List<String> invalid = new ArrayList<>();
    for (Class<?> api : API_INTERFACES) {
      for (Method m : api.getDeclaredMethods()) {
        AclCheck check = m.getAnnotation(AclCheck.class);
        if (check != null && (check.resourceType() == null || check.resourceType().isBlank())) {
          invalid.add(api.getSimpleName() + "." + m.getName());
        }
      }
    }
    assertThat(invalid).as("@AclCheck with blank resourceType").isEmpty();
  }
}
