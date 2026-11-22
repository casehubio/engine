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
package io.casehub.platform.api.data;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public interface DataCollection {
  String insert(ObjectNode record, DataContext ctx);

  String insert(String id, ObjectNode record, DataContext ctx);

  Optional<ObjectNode> get(String id, DataContext ctx);

  void update(
      String id,
      ObjectNode changes,
      OptionalInt expectedVersion,
      MergeStrategy mergeStrategy,
      DataContext ctx);

  void delete(String id, DataContext ctx);

  QueryResult query(Query query, DataContext ctx);

  long count(Query query, DataContext ctx);

  List<ObjectNode> search(String text, DataContext ctx);
}
