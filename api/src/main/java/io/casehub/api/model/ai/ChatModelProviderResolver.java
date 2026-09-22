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

import com.fasterxml.jackson.databind.JsonNode;

/**
 * SPI for constructing {@link ChatModelProvider} instances from a provider type and YAML
 * configuration. The default {@link InlineChatModelProviderResolver} builds LangChain4j providers
 * inline. Alternative implementations (e.g. in blocks engine-adapter) can route through the
 * platform's {@code RoutingAgentProvider} for pool management and rate limiting.
 */
public interface ChatModelProviderResolver {
  ChatModelProvider resolve(String providerType, JsonNode config);
}
