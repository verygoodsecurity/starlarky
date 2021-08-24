package com.verygood.security.larky.modules.types.structs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;

import com.verygood.security.larky.modules.types.LarkyObject;

import com.verygood.security.larky.modules.types.PyProtocols;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.spelling.SpellChecker;

// A trivial struct-like class with Starlark fields defined by a map.
public class SimpleStruct implements LarkyObject {

  final Map<String, Object> fields;
  final StarlarkThread currentThread;

  public static SimpleStruct create(Map<String, Object> kwargs) {
    return new SimpleStruct(kwargs, null);
  }

  public static SimpleStruct immutable(Dict<String, Object> kwargs, StarlarkThread thread) {
    return new ImmutableStruct(ImmutableMap.copyOf(kwargs), thread);
  }

  public static SimpleStruct mutable(Dict<String, Object> kwargs, StarlarkThread thread) {
    return new MutableStruct(kwargs, thread);
  }

  protected SimpleStruct(Map<String, Object> fields, StarlarkThread currentThread) {
    this.currentThread = currentThread;
    this.fields = fields;
  }

  @StarlarkMethod(name = PyProtocols.__DICT__, structField = true)
  public Dict<String, Object> dunderDict() throws EvalException {
    return composeAndFillDunderDictBuilder().build(Mutability.IMMUTABLE);
  }

  @Override
  public StarlarkThread getCurrentThread() {
    return currentThread;
  }

  @Override
  public ImmutableList<String> getFieldNames() {
    return ImmutableList.copyOf(fields.keySet());
  }

  @Override
  public Object getValue(String name) throws EvalException {
    if(name == null
        || !fields.containsKey(name)
        || fields.getOrDefault(name, null) == null) {
      return null;
    }

    return fields.get(name);
  }

  @Override
  public String getErrorMessageForUnknownField(String name) {
    String starlarkType = Starlark.type(this);
    String larkyType = LarkyObject.super.type();
    if(!larkyType.equals(starlarkType)) {
      starlarkType += String.format(" of class '%s'",larkyType);
    }

    return String.format(
      "%s has no field or method '%s'%s",
      starlarkType,
      name,
      SpellChecker.didYouMean(name,
        Starlark.dir(
          getCurrentThread().mutability(),
          getCurrentThread().getSemantics(), name)));
  }

  @Override
  public void repr(Printer p) {
    p.append("<class '").append(type()).append("'>");
  }


  @Override
  public void debugPrint(Printer p) {
    // This repr function prints only the fields.
    // Any methods are still accessible through dir/getattr/hasattr.
    p.append(type());
    p.append("(");
    String sep = "";
    for (Map.Entry<String, Object> e : fields.entrySet()) {
      p.append(sep).append(e.getKey()).append(" = ").debugPrint(e.getValue());
      sep = ", ";
    }
    p.append(")");
  }

  /**
   * Avoid un-necessary allocation if we need to override the immutability of the `__dict__` in a subclass for the caller.
   * */
  protected Dict.Builder<String, Object> composeAndFillDunderDictBuilder() throws EvalException {
    StarlarkThread thread = getCurrentThread();
    StarlarkList<String> keys = Starlark.dir(thread.mutability(), thread.getSemantics(), this);
    Dict.Builder<String, Object> builder = Dict.builder();
    for(String k : keys) {
      // obviously, ignore the actual __dict__ key since we're in this method already
      if(k.equals(PyProtocols.__DICT__)) {
        continue;
      }
      Object value = getValue(k);
      builder.put(k,  value != null ? value : Starlark.NONE);
    }

    return builder;
  }

}
