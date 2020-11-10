package com.verygood.security.larky.core;

import com.google.common.collect.ImmutableMap;

class ImmutableStruct extends SimpleStruct {
  ImmutableStruct(ImmutableMap<String, Object> fields) {
    super(fields);
  }
}
