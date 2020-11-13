package com.verygood.security.larky.stdtypes.structs;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;

class MutableStruct extends SimpleStruct {
  MutableStruct(Dict<String, Object> fields) {
    super(fields);
  }

  @Override
  public void setField(String field, Object value) throws EvalException {
    ((Dict<String, Object>) fields).putEntry(field, value);
  }

}
