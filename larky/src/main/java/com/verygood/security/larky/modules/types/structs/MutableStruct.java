package com.verygood.security.larky.modules.types.structs;

import com.google.common.base.Joiner;

import com.verygood.security.larky.modules.types.Property;
import com.verygood.security.larky.modules.types.PyProtocols;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkThread;

import lombok.SneakyThrows;

class MutableStruct extends SimpleStruct {
  MutableStruct(Dict<String, Object> fields, StarlarkThread currentThread) {
    super(fields, currentThread);
  }

  @Override
  @StarlarkMethod(name = PyProtocols.__DICT__, structField = true)
  public Dict<String, Object> dunderDict() throws EvalException {
    return composeAndFillDunderDictBuilder()
        .build(this.currentThread.mutability());
  }

  @Override
  public Object getValue(String name) throws EvalException {
    Object field = super.getValue(name);
    /* if we have assigned a field that is a descriptor, we can invoke it */
    if (field == null
        || !Property.class.isAssignableFrom(field.getClass())) {
      return field;
    }
    final Property field1 = (Property) field;
    try {
      return field1.call();
    } catch (NoSuchMethodException | NullPointerException | EvalException exception) {
      String s = String.format(
        "Exception encountered for property '%s' pointing to: '%s': %s",
        name, field, exception
      );
      throw new EvalException(s, exception);
    }
  }

  @Override
  public void setField(String name, Object value) throws EvalException {
    if(this.currentThread.mutability().isFrozen()) {
      throw new EvalException("Attempting to update a frozen structure");
    }
    Object field = this.fields.get(name);
    /* if we have assigned a field that is a descriptor, we can invoke it */
    if (field == null
        || !Property.class.isAssignableFrom(field.getClass())
         || Property.class.isAssignableFrom(value.getClass())) {
      ((Dict<String, Object>) fields).putEntry(name, value);
      return;
    }

    try {
      ((Property) field).call(new Object[]{value, name}, null);
    } catch (NoSuchMethodException exception) {
      throw new EvalException(exception);
    }
  }

  @SneakyThrows
  @Override
  public String toString() {
    return this.fields.containsKey("data") ?
        this.fields.get("data").toString():
        Joiner.on(",").withKeyValueSeparator(":").join(this.fields);
  }

}
