package com.verygood.security.larky.modules.types.structs;

import com.google.common.base.Joiner;

import com.verygood.security.larky.modules.types.Property;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;

import lombok.SneakyThrows;

class MutableStruct extends SimpleStruct {
  MutableStruct(Dict<String, Object> fields) {
    super(fields);
  }

  @Override
  public Object getValue(String name) {
    Object field = super.getValue(name);
    /* if we have assigned a field that is a descriptor, we can invoke it */
    if (field == null
        || !Property.class.isAssignableFrom(field.getClass())) {
      return field;
    }

    try {
      return ((Property) field).call();
    } catch (
        NoSuchMethodException
            | EvalException exception) {
      throw new RuntimeException(exception);
    }
  }

  @Override
  public void setField(String name, Object value) throws EvalException {
    Object field = this.fields.get(name);
    /* if we have assigned a field that is a descriptor, we can invoke it */
    if (field == null
        || !Property.class.isAssignableFrom(field.getClass())) {
      ((Dict<String, Object>) fields).putEntry(name, value);
      return;
    }

    try {
      ((Property) field).call(new Object[]{value, name}, null);
    } catch (NoSuchMethodException exception) {
      throw new RuntimeException(exception);
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
