package com.verygood.security.larky.py;

import com.google.common.collect.ImmutableList;

import com.verygood.security.larky.nativelib.LarkyProperty;
import com.verygood.security.larky.stdtypes.structs.Partial;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.HasBinary;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Structure;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.Location;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import lombok.SneakyThrows;

/**
 * An struct-like Info (LarkyValue instance) for providers defined in Starlark.
 */
public class LarkyObject implements LarkyValue, Structure, HasBinary {

  public Dict<String, Object> __dict__;


  private final LarkyType larkyType;

  @Nullable
  private final Location loc;
  // A format string with one %s placeholder for the missing field name.
  // If null, uses the default format specified by the LarkyValue.
  // TODO(adonovan): make the LarkyValue determine the error message
  // (but: this has implications for struct+struct, the equivalence
  // relation, and other observable behaviors).
  // Perhaps it should be a property of the LarkyObject instance, but
  // defined by a subclass?
  @Nullable
  private final String unknownFieldError;

  private LarkyObject(
      LarkyType type,
      @Nullable Location loc,
      @Nullable String unknownFieldError,
      @Nullable Dict<String, Object> fields) {
    this.larkyType = type;
    this.loc = loc;
    this.unknownFieldError = unknownFieldError;
    this.__dict__ = fields;
  }


  @Override
  public ImmutableList<String> getFieldNames() {
    return ImmutableList.copyOf(__dict__.keySet());
  }

  private Object getTransientValue(@Nonnull String name) throws EvalException {
    if (__dict__.containsKey(name)) {
      return __dict__.get(name);
    }

    if (getType().__dict__.containsKey(name)) {
      return getType().__dict__.get(name);
    }

    throw new EvalException(getErrorMessageForUnknownField(name));
  }

  @Override
  public Object getValue(String name) throws EvalException {
    if (name == null) {
      throw new EvalException(getErrorMessageForUnknownField(name));
    }

    Object field = getTransientValue(name);

    /* if the field is null, it's probably None? */
    if (field == null) {
      return Starlark.NONE;
    }

    /* if the field is a property, invoke its get() method! */
    if (LarkyProperty.class.isAssignableFrom(field.getClass())) {
      try {
        return ((LarkyProperty) field).call();
      } catch (NoSuchMethodException exception) {
        throw new EvalException(exception);
      }
    }

    /* if the field is a function, set it but add self! */
    if (StarlarkFunction.class.isAssignableFrom(field.getClass())) {
      field = bindMethod((StarlarkFunction) field);
    }
    return field;
  }

  @Override
  public void setField(String name, Object value) throws EvalException {
    Object field = this.__dict__.get(name);

    /* field does not exist? set it! */
    if (field == null) {
      /* if the field is a function, set it but add self! */
      if (StarlarkFunction.class.isAssignableFrom(value.getClass())) {
        value = bindMethod((StarlarkFunction) value);
      }
      this.__dict__.putEntry(name, value);
      return;
    }

    /* if the field is a property, let's invoke  the set() method it */
    if (LarkyProperty.class.isAssignableFrom(field.getClass())) {
      try {
        ((LarkyProperty) field).call(new Object[]{value, name}, null);
      } catch (NoSuchMethodException exception) {
        throw new RuntimeException(exception);
      }
    }

    /* if we're overwriting a field that is already a bound function */
    if (StarlarkFunction.class.isAssignableFrom(value.getClass())) {
      value = bindMethod((StarlarkFunction) value);
    }

    this.__dict__.putEntry(name, value);
  }

  private Object bindMethod(StarlarkFunction value) {
    return Partial.create(
        value,
        Tuple.of(),
        Dict.<String, Object>builder()
            .put("self", this)
            .buildImmutable());
  }

  @SneakyThrows
  static LarkyObject __new__(LarkyType type, Dict<String, Object> table, StarlarkThread starlarkThread) {

    // Check for duplicate keys, which are now adjacent.
//    throw Starlark.errorf(
//        "got multiple values for parameter %s in call to instantiate LarkyValue %s",
//        table[i], type.getPrintableName());


    // Check that schema is a superset of the table's keys.
//        throw Starlark.errorf(
//            "unexpected keyword%s %s in call to instantiate LarkyType %s",
//            unexpected.size() > 1 ? "s" : "",
//            Joiner.on(", ").join(unexpected),
//            type.getPrintableName());

    // 6 / 2 = 3 (cycles)
    // 0 -> 3
    // 1 -> 4
    // 2 -> 5
//    Dict.Builder<String, Object> builder = new Dict.Builder<>();
//    for (int i = 0; i < n - 1; i++) {
//      System.out.println(String.join(" ", (String) table[i], String.valueOf(StarlarkUtil.valueToStarlark(table[i+3]))));
//      builder.put((String) table[i], StarlarkUtil.valueToStarlark(table[i+3]));
//    }

    Location loc = starlarkThread.getCallerLocation();
    // __new__()
    LarkyObject obj = new LarkyObject(
        type,
        loc,
        /*unknownFieldError=*/ null,
        Dict.<String, Object>builder().build(starlarkThread.mutability()));
    // __init__()
    // TODO(mahmoudimus): What if no one passed __init__
    StarlarkFunction init = (StarlarkFunction) type.__dict__.get("__init__");
    if(init != null) {
      Starlark.call(starlarkThread, init, Tuple.of(obj), table);
    }
    return obj;
  }


  @Override
  public void repr(Printer printer) {
    printer.append("<types.");
    printer.append(getType().getPrintableName());
    printer.append(" object>");
  }

  @Override
  public LarkyType getType() {
    return larkyType;
  }

  @Override
  public Location getLocation() {
    return loc != null ? loc : getType().getLocation();
  }

  @Override
  public boolean isExported() {
    return getType().isExported();
  }

  @Override
  public void export(String extensionLabel, String exportedName) throws EvalException {
    getType().export(extensionLabel, exportedName);
  }

  @Override
  public Key getKey() {
    return getType().getKey();
  }

  @Override
  public String getPrintableName() {
    return getType().getPrintableName();
  }

  @Nullable
  @Override
  public String getErrorMessageForUnknownField(String field) {
    return getType().getErrorMessageForUnknownField(field);
  }

  @Override
  public boolean isImmutable() {
    // If the LarkyType is not yet exported, the hash code of the object is subject to change.
    if (!isExported()) {
      return false;
    }
    return Starlark.isImmutable(__dict__);
  }

  @Override
  public LarkyObject binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    if (op == TokenKind.PLUS && that instanceof LarkyObject) {
      return thisLeft
          ? plus(this, (LarkyObject) that) //
          : plus((LarkyObject) that, this);
    }
    return null;
  }

  private static LarkyObject plus(LarkyObject x, LarkyObject y) throws EvalException {
    LarkyType xprov = x.getType();
    LarkyType yprov = y.getType();
    if (!xprov.equals(yprov)) {
      throw Starlark.errorf(
          "Cannot use '+' operator on instances of different providers (%s and %s)",
          xprov.getPrintableName(), yprov.getPrintableName());
    }

    x.__dict__ = Dict.<String, Object>builder()
        .putAll(x.__dict__)
        .putAll(y.__dict__)
        .buildImmutable();
    return x;
  }
}

