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
package io.casehub.engine.rest.filter;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.acl.AclCheck;
import io.casehub.api.acl.EngineResourceTypes;
import io.casehub.platform.api.acl.AccessControlProvider;
import io.casehub.platform.api.acl.AclAction;
import io.casehub.platform.api.acl.ResourceId;
import io.casehub.platform.api.identity.CurrentPrincipal;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AclEnforcementFilterTest {

  private AclEnforcementFilter filter;
  private boolean aclAllowed;
  private ResourceId capturedResourceId;

  interface TestApi {
    @AclCheck(
        resourceType = EngineResourceTypes.CASE,
        action = AclAction.READ,
        resourceIdParam = "caseId")
    void getCaseById(UUID caseId);

    @AclCheck(resourceType = EngineResourceTypes.CASE, action = AclAction.READ)
    void listCases();

    void noAclMethod();
  }

  static class TestApiImpl implements TestApi {
    @Override
    public void getCaseById(UUID caseId) {}

    @Override
    public void listCases() {}

    @Override
    public void noAclMethod() {}
  }

  @BeforeEach
  void setUp() {
    aclAllowed = true;
    capturedResourceId = null;

    AccessControlProvider aclProvider =
        new AccessControlProvider() {
          @Override
          public boolean canAccess(String actorId, ResourceId resourceId, AclAction action) {
            capturedResourceId = resourceId;
            return aclAllowed;
          }
        };
    CurrentPrincipal principal =
        new CurrentPrincipal() {
          @Override
          public String actorId() {
            return "alice";
          }

          @Override
          public Set<String> groups() {
            return Set.of();
          }

          @Override
          public String tenancyId() {
            return "t1";
          }

          @Override
          public boolean isCrossTenantAdmin() {
            return false;
          }
        };

    filter = new AclEnforcementFilter();
    filter.accessControlProvider = aclProvider;
    filter.currentPrincipal = principal;
  }

  @Test
  void withResourceIdParam_denied_returns403() throws Exception {
    aclAllowed = false;
    UUID caseId = UUID.randomUUID();

    Method implMethod = TestApiImpl.class.getMethod("getCaseById", UUID.class);
    var pathParams = new MultivaluedHashMap<String, String>();
    pathParams.add("caseId", caseId.toString());

    var result = filter.enforce(mockRequestContext(pathParams), mockResourceInfo(implMethod));

    assertThat(result).isPresent();
    assertThat(result.get().getStatus()).isEqualTo(403);
  }

  @Test
  void withResourceIdParam_allowed_passesThrough() throws Exception {
    UUID caseId = UUID.randomUUID();

    Method implMethod = TestApiImpl.class.getMethod("getCaseById", UUID.class);
    var pathParams = new MultivaluedHashMap<String, String>();
    pathParams.add("caseId", caseId.toString());

    var result = filter.enforce(mockRequestContext(pathParams), mockResourceInfo(implMethod));

    assertThat(result).isEmpty();
    assertThat(capturedResourceId)
        .isEqualTo(new ResourceId(EngineResourceTypes.CASE, caseId.toString()));
  }

  @Test
  void emptyResourceIdParam_passesThrough() throws Exception {
    Method implMethod = TestApiImpl.class.getMethod("listCases");

    var result =
        filter.enforce(
            mockRequestContext(new MultivaluedHashMap<>()), mockResourceInfo(implMethod));

    assertThat(result).isEmpty();
  }

  @Test
  void noAnnotation_passesThrough() throws Exception {
    Method implMethod = TestApiImpl.class.getMethod("noAclMethod");

    var result =
        filter.enforce(
            mockRequestContext(new MultivaluedHashMap<>()), mockResourceInfo(implMethod));

    assertThat(result).isEmpty();
  }

  @Test
  void annotationDiscovery_walksInterfaces() throws Exception {
    Method implMethod = TestApiImpl.class.getMethod("getCaseById", UUID.class);
    assertThat(implMethod.getAnnotation(AclCheck.class)).isNull();

    aclAllowed = false;
    var pathParams = new MultivaluedHashMap<String, String>();
    pathParams.add("caseId", UUID.randomUUID().toString());

    var result = filter.enforce(mockRequestContext(pathParams), mockResourceInfo(implMethod));

    assertThat(result).isPresent();
    assertThat(result.get().getStatus()).isEqualTo(403);
  }

  @Test
  void failClosed_onException() throws Exception {
    filter.accessControlProvider =
        new AccessControlProvider() {
          @Override
          public boolean canAccess(String actorId, ResourceId resourceId, AclAction action) {
            throw new RuntimeException("ACL backend unavailable");
          }
        };

    UUID caseId = UUID.randomUUID();
    Method implMethod = TestApiImpl.class.getMethod("getCaseById", UUID.class);
    var pathParams = new MultivaluedHashMap<String, String>();
    pathParams.add("caseId", caseId.toString());

    org.junit.jupiter.api.Assertions.assertThrows(
        RuntimeException.class,
        () -> filter.enforce(mockRequestContext(pathParams), mockResourceInfo(implMethod)));
  }

  @SuppressWarnings("unchecked")
  private ContainerRequestContext mockRequestContext(
      MultivaluedHashMap<String, String> pathParams) {
    return (ContainerRequestContext)
        java.lang.reflect.Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[] {ContainerRequestContext.class},
            (proxy, method, args) -> {
              if ("getUriInfo".equals(method.getName())) {
                return java.lang.reflect.Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class[] {UriInfo.class},
                    (p2, m2, a2) -> {
                      if ("getPathParameters".equals(m2.getName())) return pathParams;
                      return null;
                    });
              }
              return null;
            });
  }

  private ResourceInfo mockResourceInfo(Method method) {
    return new ResourceInfo() {
      @Override
      public Method getResourceMethod() {
        return method;
      }

      @Override
      public Class<?> getResourceClass() {
        return TestApiImpl.class;
      }
    };
  }
}
