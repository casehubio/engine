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
package io.casehub.engine.runtime.spring;

import io.casehub.api.acl.EngineResourceTypes;
import io.casehub.platform.acl.worker.spring.SpringWorkerScopeExtractor;
import io.casehub.platform.api.acl.ResourceId;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SpringCaseScopeExtractor implements SpringWorkerScopeExtractor {

  private static final Pattern CASE_ID_PATTERN = Pattern.compile("cases/([0-9a-f-]{36})");

  @Override
  public Optional<ResourceId> extractResourceId(HttpServletRequest request) {
    Matcher matcher = CASE_ID_PATTERN.matcher(request.getRequestURI());
    if (matcher.find()) {
      return Optional.of(new ResourceId(EngineResourceTypes.CASE, matcher.group(1)));
    }
    return Optional.empty();
  }
}
