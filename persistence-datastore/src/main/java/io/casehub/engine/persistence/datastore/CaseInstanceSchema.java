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
package io.casehub.engine.persistence.datastore;

import io.casehub.platform.api.data.CollectionDescriptor;
import io.casehub.platform.api.data.FieldDescriptor;
import io.casehub.platform.api.data.FieldType;
import io.casehub.platform.api.data.IndexDescriptor;
import io.casehub.platform.api.data.IndexType;
import io.casehub.platform.api.data.StorageHint;
import java.util.List;

public final class CaseInstanceSchema {

  public static final String COLLECTION = "case_instances";

  private CaseInstanceSchema() {}

  public static CollectionDescriptor descriptor() {
    return new CollectionDescriptor(
        COLLECTION,
        List.of(
            new FieldDescriptor("uuid", FieldType.STRING, true, null),
            new FieldDescriptor("state", FieldType.STRING, true, null),
            new FieldDescriptor("namespace", FieldType.STRING, true, null),
            new FieldDescriptor("name", FieldType.STRING, true, null),
            new FieldDescriptor("actorId", FieldType.STRING, false, null),
            new FieldDescriptor("createdAt", FieldType.TIMESTAMP, true, null)),
        List.of(
            new IndexDescriptor("idx_state", List.of("state"), IndexType.STANDARD),
            new IndexDescriptor("idx_ns_name", List.of("namespace", "name"), IndexType.STANDARD)),
        List.of(),
        StorageHint.HYBRID);
  }
}
