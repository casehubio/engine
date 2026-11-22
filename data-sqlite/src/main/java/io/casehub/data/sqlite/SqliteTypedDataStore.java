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
package io.casehub.data.sqlite;

import com.zaxxer.hikari.HikariDataSource;
import io.casehub.platform.api.data.CollectionDescriptor;
import io.casehub.platform.api.data.DataCollection;
import io.casehub.platform.api.data.DataStore;
import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

public class SqliteTypedDataStore implements DataStore, Closeable {

  private final DataSource dataSource;
  private final Map<String, CollectionDescriptor> descriptors = new ConcurrentHashMap<>();

  public SqliteTypedDataStore(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void register(CollectionDescriptor descriptor) {
    descriptors.put(descriptor.name(), descriptor);
  }

  @Override
  public DataCollection collection(String name) {
    CollectionDescriptor desc = descriptors.get(name);
    if (desc != null && !desc.fields().isEmpty()) {
      return new SqliteTypedDataCollection(dataSource, name, desc);
    }
    return new SqliteDocumentDataCollection(dataSource, name);
  }

  @Override
  public void close() {
    if (dataSource instanceof HikariDataSource hds) {
      hds.close();
    }
  }
}
