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
package io.casehub.engine.internal.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContextUtils {

  private static final ObjectMapper mapper = new ObjectMapper();

  private ContextUtils() {
    // Utility class, prevent instantiation
  }

  public static Map<String, Object> evalObjectTemplate(Map<String, Object> data, String template) {
    if (template == null) return Map.of();
    String s = template.trim();
    if (s.isEmpty()) return Map.of();

    if (!(s.startsWith("{") && s.endsWith("}"))) {
      throw new IllegalArgumentException("Object template must be wrapped with { }: " + template);
    }

    String body = s.substring(1, s.length() - 1).trim();
    if (body.isEmpty()) return Map.of();

    LinkedHashMap<String, Object> out = new LinkedHashMap<>();
    for (String entry : splitTopLevel(body, ',')) {
      String e = entry.trim();
      if (e.isEmpty()) continue;

      int colon = indexOfTopLevel(e, ':');
      if (colon < 0) {
        throw new IllegalArgumentException("Invalid entry (missing ':'): " + e);
      }

      String rawKey = e.substring(0, colon).trim();
      String rawVal = e.substring(colon + 1).trim();

      String key = parseKey(rawKey);
      Object val = evalValue(data, rawVal);

      out.put(key, val);
    }
    return out;
  }

  /** Supports: .path, ".", JSON literals ("s", 1, 1.2, true/false, null) */
  public static Object evalValue(Map<String, Object> data, String expr) {
    if (expr == null) return null;
    String v = expr.trim();
    if (v.isEmpty()) return null;

    // JQ-like path: .a.b.c
    if (v.startsWith(".")) {
      if (v.equals(".")) {
        return new LinkedHashMap<>(data); // whole context snapshot (shallow)
      }
      String path = v.substring(1); // remove leading dot
      return getPathInternal(data, path);
    }

    // JSON literal (string/number/bool/null)
    // Let Jackson parse it when possible (e.g. "x", 1, true, null)
    try {
      // readValue expects valid JSON token; this works for "str", 123, true, null, 12.3
      return mapper.readValue(v, Object.class);
    } catch (Exception ignore) {
      // fallback: treat as bare string
      return v;
    }
  }

  public static Object getPathInternal(Map<String, Object> data, String path) {
    String[] parts = path.split("\\.");
    Object current = data;

    for (String part : parts) {
      if (current instanceof Map<?, ?> map) {
        current = map.get(part);
      } else {
        return null;
      }
      if (current == null) return null;
    }
    return current;
  }

  public static String parseKey(String rawKey) {
    String k = rawKey.trim();
    if (k.isEmpty()) throw new IllegalArgumentException("Empty key in object template");

    // allow quoted keys: "documentId": .documentId
    if ((k.startsWith("\"") && k.endsWith("\"")) || (k.startsWith("'") && k.endsWith("'"))) {
      // normalize to JSON double quotes for parsing
      String json =
          k.startsWith("'")
              ? "\"" + k.substring(1, k.length() - 1).replace("\"", "\\\"") + "\""
              : k;
      try {
        return mapper.readValue(json, String.class);
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid quoted key: " + rawKey, e);
      }
    }

    // unquoted identifier
    return k;
  }

  /** Split by delimiter at top-level, respecting quotes and nested {}[]() */
  public static List<String> splitTopLevel(String s, char delimiter) {
    List<String> parts = new ArrayList<>();
    StringBuilder cur = new StringBuilder();

    int depth = 0;
    boolean inStr = false;
    char strQuote = 0;
    boolean esc = false;

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);

      if (inStr) {
        cur.append(c);
        if (esc) {
          esc = false;
        } else if (c == '\\') {
          esc = true;
        } else if (c == strQuote) {
          inStr = false;
          strQuote = 0;
        }
        continue;
      }

      if (c == '"' || c == '\'') {
        inStr = true;
        strQuote = c;
        cur.append(c);
        continue;
      }

      if (c == '{' || c == '[' || c == '(') depth++;
      else if (c == '}' || c == ']' || c == ')') depth = Math.max(0, depth - 1);

      if (depth == 0 && c == delimiter) {
        parts.add(cur.toString());
        cur.setLength(0);
      } else {
        cur.append(c);
      }
    }

    parts.add(cur.toString());
    return parts;
  }

  /** Find first char at top-level (not inside quotes/nesting) */
  public static int indexOfTopLevel(String s, char ch) {
    int depth = 0;
    boolean inStr = false;
    char strQuote = 0;
    boolean esc = false;

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);

      if (inStr) {
        if (esc) esc = false;
        else if (c == '\\') esc = true;
        else if (c == strQuote) {
          inStr = false;
          strQuote = 0;
        }
        continue;
      }

      if (c == '"' || c == '\'') {
        inStr = true;
        strQuote = c;
        continue;
      }

      if (c == '{' || c == '[' || c == '(') depth++;
      else if (c == '}' || c == ']' || c == ')') depth = Math.max(0, depth - 1);

      if (depth == 0 && c == ch) return i;
    }
    return -1;
  }
}
