package com.verygood.security.larky.stdtypes.structs;

import com.google.common.collect.ImmutableMap;

class ImmutableStruct extends SimpleStruct {
  ImmutableStruct(ImmutableMap<String, Object> fields) {
    super(fields);
  }
}
