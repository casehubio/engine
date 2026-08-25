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
package io.casehub.api.model;

import io.casehub.platform.api.expression.ExpressionEvaluator;
import java.util.Objects;

/**
 * CloudEvent-based trigger. Fires when a matching CloudEvent is received.
 *
 * <p>Supports exact match on {@code type} (required), and optional exact match on {@code source}
 * and {@code subject}. An optional {@code filter} expression provides predicate-based filtering
 * over the event and context.
 */
public class CloudEventTrigger implements Trigger {

  private final String type;
  private final String source;
  private final String subject;
  private final ExpressionEvaluator filter;

  public CloudEventTrigger(String type) {
    this(type, null, null, null);
  }

  public CloudEventTrigger(String type, String source, String subject, ExpressionEvaluator filter) {
    this.type = Objects.requireNonNull(type, "CloudEvent type must not be null");
    this.source = source;
    this.subject = subject;
    this.filter = filter;
  }

  public String getType() {
    return type;
  }

  public String getSource() {
    return source;
  }

  public String getSubject() {
    return subject;
  }

  public ExpressionEvaluator getFilter() {
    return filter;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("CloudEventTrigger{type='").append(type).append('\'');
    if (source != null) sb.append(", source='").append(source).append('\'');
    if (subject != null) sb.append(", subject='").append(subject).append('\'');
    if (filter != null) sb.append(", filter=").append(filter);
    sb.append('}');
    return sb.toString();
  }
}
