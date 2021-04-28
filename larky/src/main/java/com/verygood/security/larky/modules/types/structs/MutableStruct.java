package com.verygood.security.larky.modules.types.structs;

import com.google.common.base.Joiner;

import com.verygood.security.larky.modules.types.Property;
import com.verygood.security.larky.modules.types.PyProtocols;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;

import lombok.SneakyThrows;

class MutableStruct extends SimpleStruct {
  MutableStruct(Dict<String, Object> fields, StarlarkThread currentThread) {
    super(fields, currentThread);
  }

  @StarlarkMethod(name = PyProtocols.__DICT__, useStarlarkThread = true)
  public Dict<String, Object> dunderDict(StarlarkThread thread) throws EvalException {
    StarlarkList<String> keys = Starlark.dir(thread.mutability(), thread.getSemantics(), this);
    Dict.Builder<String, Object> builder = Dict.builder();
    for(String k : keys) {
      // obviously, ignore the actual __dict__ key since we're in this method already
      if(k.equals(PyProtocols.__DICT__)) {
        continue;
      }
      Object value = super.getValue(k);
      builder.put(k,  value != null ? value : Starlark.NONE);
    }
    return builder.build(thread.mutability());
  }


  @Override
  public Object getValue(String name) throws EvalException {
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
    if(this.currentThread.mutability().isFrozen()) {
      throw new EvalException("Attempting to update a frozen structure");
    }
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
