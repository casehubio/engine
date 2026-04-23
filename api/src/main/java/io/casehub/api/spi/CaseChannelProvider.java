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
package io.casehub.api.spi;

import io.casehub.api.model.CaseChannel;
import java.util.List;
import java.util.UUID;

/**
 * Creates and manages communication channels for workers on a case.
 *
 * <p>The {@link CaseChannel} record carries a {@code backendType} field and extensible {@code
 * properties} map so implementations can attach backend-specific metadata without coupling the SPI
 * to any particular channel system.
 */
public interface CaseChannelProvider {

  /**
   * Open a new channel for the given case.
   *
   * @param caseId the case instance ID
   * @param purpose human-readable description of the channel's purpose
   * @return the opened channel reference
   */
  CaseChannel openChannel(UUID caseId, String purpose);

  /**
   * Post a message to a channel.
   *
   * @param channel the channel reference returned by {@link #openChannel}
   * @param from sender identity (worker ID or "human")
   * @param content message content
   */
  void postToChannel(CaseChannel channel, String from, String content);

  /**
   * Close a channel. No-op if the channel is unknown or already closed.
   *
   * @param channel the channel to close
   */
  void closeChannel(CaseChannel channel);

  /**
   * List all channels currently open for the given case.
   *
   * @param caseId the case instance ID
   * @return list of open channels, empty if none
   */
  List<CaseChannel> listChannels(UUID caseId);
}
