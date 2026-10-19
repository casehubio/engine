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

import io.casehub.api.acl.AclCheck;
import io.casehub.engine.rest.dto.ProblemDetail;
import io.casehub.platform.api.acl.AccessControlProvider;
import io.casehub.platform.api.acl.ResourceId;
import io.casehub.platform.api.identity.CurrentPrincipal;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import java.lang.reflect.Method;
import java.util.Optional;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

@ApplicationScoped
public class AclEnforcementFilter {

  @Inject AccessControlProvider accessControlProvider;
  @Inject CurrentPrincipal currentPrincipal;

  @ServerRequestFilter
  public Optional<RestResponse<ProblemDetail>> enforce(
      ContainerRequestContext ctx, ResourceInfo resourceInfo) {
    AclCheck check = discoverAclCheck(resourceInfo);
    if (check == null) {
      return Optional.empty();
    }

    String resourceIdParam = check.resourceIdParam();
    if (resourceIdParam.isEmpty()) {
      return Optional.empty();
    }

    String resourceId = ctx.getUriInfo().getPathParameters().getFirst(resourceIdParam);
    if (resourceId == null) {
      return Optional.empty();
    }

    String actorId = currentPrincipal.actorId();
    ResourceId rid = new ResourceId(check.resourceType(), resourceId);

    if (!accessControlProvider.canAccess(actorId, rid, check.action())) {
      return Optional.of(
          RestResponse.status(
              RestResponse.Status.FORBIDDEN,
              new ProblemDetail("Access denied", 403, "Insufficient permissions")));
    }

    return Optional.empty();
  }

  private AclCheck discoverAclCheck(ResourceInfo resourceInfo) {
    Method method = resourceInfo.getResourceMethod();
    AclCheck check = method.getAnnotation(AclCheck.class);
    if (check != null) {
      return check;
    }

    Class<?> resourceClass = resourceInfo.getResourceClass();
    for (Class<?> iface : resourceClass.getInterfaces()) {
      try {
        Method ifaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
        check = ifaceMethod.getAnnotation(AclCheck.class);
        if (check != null) {
          return check;
        }
      } catch (NoSuchMethodException ignored) {
      }
    }

    return null;
  }
}
