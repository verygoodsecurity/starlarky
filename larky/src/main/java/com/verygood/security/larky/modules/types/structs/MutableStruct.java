package com.verygood.security.larky.modules.types.structs;

import com.verygood.security.larky.modules.types.Property;
import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.parser.CurrentEvaluation;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkSemantics;

import org.jetbrains.annotations.Nullable;

import lombok.SneakyThrows;

public class MutableStruct extends SimpleStruct {
  // The struct's own freeze state: the Mutability of the thread that created it. Not the thread.
  private final Mutability mutability;

  protected MutableStruct(Dict<String, Object> fields, Mutability mutability) {
    super(fields);
    this.mutability = mutability;
  }

  /**
   * A struct that shares {@code shared}'s fields: a change made through either is a change to both. For a
   * decorator, such as a short-circuit response decorating the response it stops with.
   */
  protected MutableStruct(MutableStruct shared, Mutability mutability) {
    super(shared.fields);
    this.mutability = mutability;
  }

  /** The field as stored, without resolving a property or {@code __getattr__} ({@link #getField} does). */
  protected final Object rawField(String name) {
    return fields.get(name);
  }

  /** Whether the fields are frozen (their dict's mutability). */
  protected final boolean fieldsFrozen() {
    return fields instanceof Dict<?, ?> dict && dict.mutability().isFrozen();
  }

  @Override
  @StarlarkMethod(name = PyProtocols.__DICT__, structField = true)
  public Dict<String, Object> dunderDict() throws EvalException {
    return composeAndFillDunderDictBuilder()
        .build(this.mutability);
  }

  @Override
  public Object getField(String name, @Nullable StarlarkThread thread) {
    Object field = super.getField(name, thread);
    /* if we have assigned a field that is a descriptor, we can invoke it */
    if (field == null
        || !Property.class.isAssignableFrom(field.getClass())) {
      return field;
    }
    final Property field1 = (Property) field;
    try {
      // on the caller's thread (the current evaluation's when the caller has none)
      return field1.get(thread);
    } catch (EvalException exception) {
      String s = String.format(
        "Exception encountered for property '%s' pointing to: '%s': %s",
        name, field, exception
      );
      throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(s, exception, thread);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(e, thread);
    }
  }

  @Override
  public void setField(String name, Object value) throws EvalException {
    if (this.mutability.isFrozen()) {
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
      ((Property) field).set(value, name, CurrentEvaluation.requireThread());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new EvalException(e);
    }
  }

  @SneakyThrows
  @Override
  public String toString() {
    String result;
    if (this.fields.containsKey("data")) {
      result = this.fields.get("data").toString();
    } else {
      Dict<String, Object> d = Dict.cast(this.fields, String.class, Object.class, "cast");
      result = Starlark.repr(d, StarlarkSemantics.DEFAULT);
    }
    return result;
  }

}
