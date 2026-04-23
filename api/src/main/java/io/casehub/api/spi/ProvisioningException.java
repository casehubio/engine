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

/**
 * Thrown by {@code WorkerProvisioner.provision()} when a worker cannot be started.
 *
 * <p>Unchecked — callers decide whether to catch and retry or propagate. Common causes include
 * resource exhaustion (no available tmux sessions, no Docker capacity), network failures when
 * reaching a remote provisioner, or configuration errors.
 */
public class ProvisioningException extends RuntimeException {

  public ProvisioningException(String message) {
    super(message);
  }

  public ProvisioningException(String message, Throwable cause) {
    super(message, cause);
  }
}
