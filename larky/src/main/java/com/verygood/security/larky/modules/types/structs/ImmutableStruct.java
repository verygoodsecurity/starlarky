package com.verygood.security.larky.modules.types.structs;

import com.google.common.collect.ImmutableMap;

import net.starlark.java.eval.StarlarkThread;

class ImmutableStruct extends SimpleStruct {
  ImmutableStruct(ImmutableMap<String, Object> fields, StarlarkThread currentThread) {
    super(fields, currentThread);
  }
}
