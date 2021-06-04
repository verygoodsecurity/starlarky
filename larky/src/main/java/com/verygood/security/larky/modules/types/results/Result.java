package com.verygood.security.larky.modules.types.results;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.verygood.security.larky.modules.types.PyProtocols;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

import org.jetbrains.annotations.NotNull;


@StarlarkBuiltin(
  name = "_JResult" // only needed to satisfy Eval
)
public interface Result extends StarlarkValue, Comparable<Result> {

  @StarlarkMethod(name = "Error", parameters = {@Param(name = "error")})
  static Result error(Object error) {
    Objects.requireNonNull(error);
    if(error instanceof Result) {
      return (Result) error;
    }
    if (EvalException.class.isAssignableFrom(error.getClass())) {
      return new Error((EvalException) error);
    }
    return new Error(Starlark.str(error));
  }

  @StarlarkMethod(name = "Ok", parameters = {@Param(name = "value")})
  static Result ok(Object value) {
    Objects.requireNonNull(value);
    if(value instanceof Result) {
      return (Result) value;
    }
    return new Ok(value);
  }

  @StarlarkMethod(name = "of", parameters = {@Param(name = "o")})
  static Result of(Object o) {
    if(o instanceof Result) {
      return (Result) o;
    }
    else if (o instanceof Exception) {
      return error(o);
    }
    return ok(o);
  }

  Object getValue();

  EvalException getError();

  @StarlarkMethod(name = "_val", structField = true, allowReturnNones = true)
  default Object Val() {
    return (isOk()) ? getValue() : getError().getMessage();
  }

  @StarlarkMethod(name = "is_ok", structField = true)
  boolean isOk();

  @StarlarkMethod(name = "is_err", structField = true)
  boolean isError();

  // copy https://github.com/MaT1g3R/option/blob/master/option/result.py
  // decided against: https://github.com/dbrgn/result/blob/master/result/result.py
  @StarlarkMethod(name = "map", parameters = {
    @Param(name = "func")
  }, allowReturnNones = true, useStarlarkThread = true)
  default <T> Result map(StarlarkCallable func, StarlarkThread thread) {
    return
      Optional.ofNullable(getValue())
        .map((o) -> {
          try {
            return of(Starlark.call(thread, func, Tuple.of(o), Dict.empty()));
          } catch (EvalException | InterruptedException e) {
            throw new RuntimeException(e);
          }
        })
        .orElse(this);
  }

  @StarlarkMethod(name = "flatmap", parameters = {
    @Param(name = "func")
  }, allowReturnNones = true, useStarlarkThread = true)
  default <T> Result flatMap(StarlarkCallable func, StarlarkThread thread) {
    if(this.isOk()) {
      try {
        return ok(Starlark.call(thread, func, Tuple.of(getValue()), Dict.empty()));
      } catch (EvalException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    return this;
  }

  @StarlarkMethod(name = "map_err", parameters = {
    @Param(name = "func")
  }, allowReturnNones = true, useStarlarkThread = true)
  default <T> Result mapError(StarlarkCallable func, StarlarkThread thread) {
    if(this.isOk()) {
      return this;
    }
    try {
      return error(Starlark.call(thread, func, Tuple.of(getError()), Dict.empty()));
    } catch (EvalException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Express the expectation that this object is an Ok value. If it's an Error value instead, throw
   * a EvalException with the given message.
   *
   * @param msg the message to pass to a potential EvalException
   * @throws EvalException if unwrap() is called on an Error value
   */
  @StarlarkMethod(name = "expect",
    doc = "Express the expectation that this object is an Ok value. If it's an Error value " +
            "instead, throw a EvalException with the given message." +
            "Returns the success value in the :class:`Result` or raises\n" +
            "a ``ValueError`` with a provided message.\n" +
            "Args:\n" +
            "    msg: The error message.\n" +
            "Returns:\n" +
            "    The success value in the :class:`Result` if it is\n" +
            "    a :meth:`Result.Ok` value.\n" +
            "Raises:\n" +
            "    ``ValueError`` with ``msg`` as the message if the\n" +
            "    :class:`Result` is a :meth:`Result.Err` value.\n" +
            "Examples:\n" +
            "    >>> Ok(1).expect('no')\n" +
            "    1\n" +
            "    >>> try:\n" +
            "    ...     Err(1).expect('no')\n" +
            "    ... except ValueError as e:\n" +
            "    ...     print(e)\n" +
            "    no",
    parameters = {
      @Param(name = "msg", doc = "The error message.")
  }, allowReturnNones = true)
  default Object expect(String msg) throws EvalException {
    if (isOk()) {
      return getValue();
    }
    throw new EvalException(msg);
  }

  @StarlarkMethod(name = "expect_err", parameters = {
    @Param(name = "msg")
  }, allowReturnNones = true)
  default Object expectErr(String msg) throws EvalException {
    if (isOk()) {
      throw new EvalException(msg);
    }
    return this.getError();
  }

  @StarlarkMethod(name = "unwrap", allowReturnNones = true)
  default Object unwrap() throws EvalException {
    Object o = orElseRaiseAs(e -> e);
    return o;
  }

  @StarlarkMethod(name = "unwrap_or",
    doc ="" +
       "Returns the success value in the :class:`Result` or ``optb``.\n" +
       "Args:\n" +
       "    optb: The default return value.\n" +
       "\n" +
       "\n" +
       "Returns:\n" +
       "    The success value in the :class:`Result` if it is a\n" +
       "    :meth:`Result.Ok` value, otherwise ``optb``.\n" +
       "\n" +
       "\n" +
       "Notes:\n" +
       "    If you wish to use a result of a function call as the default,\n" +
       "    it is recommnded to use :meth:`unwrap_or_else` instead.\n" +
       "\n" +
       "\n" +
       "Examples:\n" +
       "    >>> Ok(1).unwrap_or(2)\n" +
       "    1\n" +
       "    >>> Err(1).unwrap_or(2)\n" +
       "    2",
    parameters = {@Param(name = "optb",
      doc = "The default return value")
  }, allowReturnNones = true)
  default Object unwrapOr(Object defaultValue) throws EvalException {
    return (isOk()) ? getValue() : defaultValue;
  }

  @StarlarkMethod(name = "unwrap_or_else", parameters = {
    @Param(name = "func")
  }, useStarlarkThread = true, allowReturnNones = true)
  default Object unwrapOrElse(StarlarkCallable func, StarlarkThread thread) throws EvalException {
    Object value = getValue();
    if(value != null) {
      return value;
    }
    // we know we are an error instance here since getValue() is null here.
    try {
      // TODO(mahmoudimus): getError().getMessage() <- we should probably make this a value
      //  instead of extracting the message itself from the error. This *could* be a
      //  potential bug.
      return Starlark.call(thread, func, Tuple.of(getError().getMessage()), Dict.empty());
    } catch (InterruptedException e) {
      throw new EvalException(e.getMessage(), e);
    }
  }

  @StarlarkMethod(name = "unwrap_err", allowReturnNones = true)
  default Object unwrapErr() throws EvalException {
    return expectErr(String.valueOf(getValue()));
  }

  default <E2 extends EvalException> Object orElseRaiseAs(Function<EvalException, E2> emapper) throws E2 {
    return Optional.ofNullable(getValue())
             .orElseThrow(() -> emapper.apply(getError()));
  }

  @Override
  default boolean truth() {
    return isOk();
  }

  @StarlarkMethod(name = PyProtocols.__BOOL__)
  default boolean __bool__() {
    return truth();
  }

  @StarlarkMethod(name = PyProtocols.__EQ__)
  default boolean __eq__(Result o) {
    return compareTo(o) == 0;
  }

  @StarlarkMethod(name = PyProtocols.__NE__)
  default boolean __ne__(Result o) {
    return compareTo(o) != 0;
  }

  @StarlarkMethod(name = PyProtocols.__LT__)
  default boolean __lt__(Result o) {
    return compareTo(o) != 0;
  }

  @StarlarkMethod(name = PyProtocols.__LE__)
  default boolean __le__(Result o) {
    return compareTo(o) <= 0;
  }

  @StarlarkMethod(name = PyProtocols.__GT__)
  default boolean __gt__(Result o) {
    return compareTo(o) > 0;
  }

  @StarlarkMethod(name = PyProtocols.__GE__)
  default boolean __ge__(Result o) {
    return compareTo(o) >= 0;
  }

  @Override
  default int compareTo(@NotNull Result o) {
    if(isOk() && o.isError()) {
      return -1; // error is > ok according to my understanding of test_result.py
    }
    else if(isError() && o.isOk()) {
      return 1;
    }
    else if(equals(o)) {
      return 0;
    }
    else if(isOk() == o.isOk()) {
      if(Objects.equals(Val(), o.Val())) {
        return 0;
      }
      return String.valueOf(Val()).compareTo(String.valueOf(o.Val()));
    }
    return -1;
  }

}