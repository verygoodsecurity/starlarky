package com.verygood.security.mode.grpc;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ContextMap<K, V> extends HashMap<K, V> {

  public ContextMap(Map<V, V> contextMap) {
    super((Map<? extends K, ? extends V>) contextMap);
  }

  @Override
  public String toString() {
    return this.entrySet()
        .stream()
        .map(entry -> String.format("\"%s\": \"%s\"", entry.getKey(), entry.getValue()))
        .collect(Collectors.joining(", ", "{", "}"));
  }
}
