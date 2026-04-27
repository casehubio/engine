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
package io.casehub.workadapter;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and constructs the {@code callerRef} string embedded by CaseHub when spawning a
 * quarkus-work WorkItem child.
 *
 * <p>Format: {@code case:{caseId}/pi:{planItemId}}
 *
 * <p>CaseHub owns the semantics of this opaque string — quarkus-work stores it unchanged on the
 * WorkItem and echoes it back in every WorkItemLifecycleEvent. Refs casehubio/quarkus-work#136.
 */
public record CallerRef(UUID caseId, String planItemId) {

  private static final Pattern PATTERN = Pattern.compile("^case:([0-9a-fA-F-]{36})/pi:(.+)$");

  public static String encode(UUID caseId, String planItemId) {
    return "case:" + caseId + "/pi:" + planItemId;
  }

  /** Returns {@code null} if the string is not a CaseHub callerRef. */
  public static CallerRef parse(String callerRef) {
    if (callerRef == null) return null;
    Matcher m = PATTERN.matcher(callerRef);
    if (!m.matches()) return null;
    try {
      return new CallerRef(UUID.fromString(m.group(1)), m.group(2));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
