package com.verygood.security.larky.core;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.syntax.Location;

class MutableStruct extends SimpleStruct {
  MutableStruct(Dict<String, Object> fields) {
    super(fields);
  }

  @Override
  public void setField(String field, Object value) throws EvalException {
    ((Dict<String, Object>) fields).put(field, value, (Location) null);
  }

}
