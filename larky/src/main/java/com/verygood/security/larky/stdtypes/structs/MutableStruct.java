package com.verygood.security.larky.stdtypes.structs;

import com.verygood.security.larky.nativelib.LarkyDescriptor;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;

class MutableStruct extends SimpleStruct {
  MutableStruct(Dict<String, Object> fields) {
    super(fields);
  }

  @Override
  public Object getValue(String name) {
    Object field = super.getValue(name);
    /* if we have assigned a field that is a descriptor, we can invoke it */
    if (field == null
        || !LarkyDescriptor.class.isAssignableFrom(field.getClass())) {
      return field;
    }

    try {
      return ((LarkyDescriptor) field).call();
    } catch (
        NoSuchMethodException
            | EvalException
            | InterruptedException exception) {
      throw new RuntimeException(exception);
    }
  }

  @Override
  public void setField(String name, Object value) throws EvalException {
    Object field = this.fields.get(name);
    /* if we have assigned a field that is a descriptor, we can invoke it */
    if (field == null
        || !LarkyDescriptor.class.isAssignableFrom(field.getClass())) {
      ((Dict<String, Object>) fields).putEntry(name, value);
      return;
    }

    try {
      ((LarkyDescriptor) field).call(new Object[]{value}, null);
    } catch (
        NoSuchMethodException
            | EvalException
            | InterruptedException exception) {
      throw new RuntimeException(exception);
    }
  }

}

/*

c = mutablestruct()
c.foo = foo
c.foo()
 */