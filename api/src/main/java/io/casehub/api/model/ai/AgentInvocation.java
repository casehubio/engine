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
package io.casehub.api.model.ai;

import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import java.time.Duration;

/** A completed synchronous invocation of a streaming agent. */
public record AgentInvocation(String text, AgentEvent.InvocationComplete completion) {
  public static AgentInvocation invoke(
      AgentProvider provider, AgentSessionConfig config, Duration timeout) {
    var events = provider.invoke(config).collect().asList().await().atMost(timeout);
    var text = new StringBuilder();
    AgentEvent.InvocationComplete completion = null;
    for (var event : events) {
      if (event instanceof AgentEvent.TextDelta delta) {
        text.append(delta.text());
      } else if (event instanceof AgentEvent.InvocationComplete complete) {
        completion = complete;
      }
    }
    if (completion != null && completion.isError()) {
      throw new AgentException("Agent invocation reported an error: " + text);
    }
    return new AgentInvocation(text.toString(), completion);
  }
}
