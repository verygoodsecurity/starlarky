package com.verygood.security.larky.modules.types.structs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.Structure;

import java.util.Map;

// A trivial struct-like class with Starlark fields defined by a map.
public class SimpleStruct implements Structure {

  final Map<String, Object> fields;

  @SuppressWarnings("CdiInjectionPointsInspection")
  SimpleStruct(Map<String, Object> fields) {
    this.fields = fields;
  }

  public static SimpleStruct create(Map<String, Object> kwargs) {
    return new SimpleStruct(kwargs);
  }

  public static SimpleStruct immutable(Dict<String, Object> kwargs, StarlarkSemantics semantics) {
    return new ImmutableStruct(ImmutableMap.copyOf(kwargs));
  }

  public static SimpleStruct mutable(Dict<String, Object> kwargs, StarlarkSemantics semantics) {
    return new MutableStruct(kwargs);
  }

  @Override
  public ImmutableList<String> getFieldNames() {
    return ImmutableList.copyOf(fields.keySet());
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public Object getValue(String name) {
    if(name == null
        || !fields.containsKey(name)
        || fields.getOrDefault(name, null) == null) {
      return null;
    }

    return fields.get(name);
  }

  @Override
  public String getErrorMessageForUnknownField(String name) {
    return null;
  }

  @Override
  public void repr(Printer p) {
    // This repr function prints only the fields.
    // Any methods are still accessible through dir/getattr/hasattr.
    p.append(Starlark.type(this));
    p.append("(");
    String sep = "";
    for (Map.Entry<String, Object> e : fields.entrySet()) {
      p.append(sep).append(e.getKey()).append(" = ").repr(e.getValue());
      sep = ", ";
    }
    p.append(")");
  }

}
