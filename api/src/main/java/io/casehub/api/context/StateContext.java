package io.casehub.api.context;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public interface StateContext {

    Map<String, Object> getData();

    StateContext set(String key, Object value);

    Object get(String key);

    <T> T getAs(String key, Class<T> type);

    <T> T getOrDefault(String key, T defaultValue);

    Object computeIfAbsent(String key, Function<String, Object> mappingFunction);

    Object putIfAbsent(String key, Object value);

    boolean compareAndSet(String key, Object expected, Object newValue);

    StateContext update(String key, Function<Object, Object> updateFunction);

    String getString(String key);

    Integer getInt(String key);

    Long getLong(String key);

    Double getDouble(String key);

    Boolean getBoolean(String key);

    <T> List<T> getList(String key, Class<T> elementType);

    Object getPath(String path);

    String getPathAsString(String path);

    StateContext setPath(String path, Object value);

    StateContext setAll(Map<String, Object> values);

    Map<String, Object> getAll(String... keys);

    boolean contains(String key);

    StateContext remove(String key);

    StateContext clear();

    Set<String> getKeys();

    boolean isEmpty();

    int size();

    JsonNode asJsonNode();

    StateContext merge(StateContext other);

    StateContext snapshot();

    JsonNode diff(StateContext other);

    void applyDiff(JsonNode diff);

    long getVersion();

    Map<String, Object> evalObjectTemplate(String template);
}
