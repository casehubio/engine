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
package io.casehub.api.model.converter.yaml;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class YamlRecordDriftTest {

  private static final Path GENERATED_DIR =
      Path.of("target/generated-sources/yaml-records/io/casehub/api/model/converter/yaml");
  private static final Path HAND_WRITTEN_DIR =
      Path.of("src/main/java/io/casehub/api/model/converter/yaml");
  private static final Path ALLOW_LIST = Path.of("src/main/resources/hand-written-exceptions.txt");

  @Test
  void handWrittenTypes_mustBeGeneratedOrAllowListed() throws IOException {
    Set<String> generated = scanJavaTypes(GENERATED_DIR);
    Set<String> handWritten = scanJavaTypes(HAND_WRITTEN_DIR);
    Set<String> allowed = loadAllowList();

    Set<String> drift = new TreeSet<>(handWritten);
    drift.removeAll(generated);
    drift.removeAll(allowed);

    assertTrue(
        drift.isEmpty(),
        () ->
            "Codegen drift detected — hand-written types not in generated output or allow-list:\n"
                + drift.stream().map(t -> "  - " + t).collect(Collectors.joining("\n"))
                + "\n\nEither generate from the schema or add to "
                + ALLOW_LIST
                + " with a justification comment.");
  }

  private Set<String> scanJavaTypes(Path dir) throws IOException {
    if (!Files.isDirectory(dir)) {
      return Set.of();
    }
    try (Stream<Path> files = Files.list(dir)) {
      return files
          .filter(p -> p.toString().endsWith(".java"))
          .map(p -> p.getFileName().toString())
          .map(name -> name.substring(0, name.length() - 5))
          .filter(name -> !name.equals("package-info"))
          .collect(Collectors.toCollection(TreeSet::new));
    }
  }

  private Set<String> loadAllowList() throws IOException {
    if (!Files.exists(ALLOW_LIST)) {
      return Set.of();
    }
    return Files.readAllLines(ALLOW_LIST).stream()
        .map(String::trim)
        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
