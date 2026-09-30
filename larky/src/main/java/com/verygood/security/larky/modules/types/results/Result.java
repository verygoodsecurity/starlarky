package com.verygood.security.larky.modules.types.results;

import java.util.Objects;
import java.util.function.Function;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

import org.jetbrains.annotations.NotNull;


@StarlarkBuiltin(
  name = "_JResult" // only needed to satisfy Eval
)
public interface Result extends StarlarkValue, Comparable<Result> {

  @StarlarkMethod(name = "Error", parameters = {@Param(name = "error")}, useStarlarkThread = true)
  static Result error(Object error, StarlarkThread thread) {
    Objects.requireNonNull(error);
    if(error instanceof Result) {
      return (Result) error;
    }
    return Error.withThread(error, thread);
  }

  @StarlarkMethod(name = "Ok", parameters = {@Param(name = "value")})
  static Result ok(Object value) {
    Objects.requireNonNull(value);
    if(value instanceof Result) {
      return (Result) value;
    }
    return new Ok(value);
  }

  @StarlarkMethod(name = "of", parameters = {@Param(name = "o")}, useStarlarkThread = true)
  static Result of(Object o, StarlarkThread thread) {
    if(o instanceof Result) {
      return (Result) o;
    }
    else if (o instanceof Exception) {
      return error(o, thread);
    }
    return ok(o);
  }

  Object getValue();

  Error getError();

  @StarlarkMethod(name = "_val", structField = true, allowReturnNones = true)
  default Object Val() {
    return (isOk()) ? getValue() : getError().getValue();
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
  default <T> Result map(StarlarkCallable func, StarlarkThread thread) throws InterruptedException {
    if(!isOk()) {
      return this;
    }

    int stackSize = StarlarkEvalWrapper.callStackSize(thread);
    try {
      final Object res = Starlark.call(thread, func, Tuple.of(getValue()), Dict.empty());
      return of(res, thread);
    } catch (EvalException | Starlark.UncheckedEvalException e) {
      StarlarkEvalWrapper.unwindCallStack(thread, stackSize);
      return error(e, thread);
    }
  }

  @StarlarkMethod(name = "flatmap", parameters = {
    @Param(name = "func")
  }, allowReturnNones = true, useStarlarkThread = true)
  default <T> Result flatMap(StarlarkCallable func, StarlarkThread thread) throws InterruptedException {
    if(this.isOk()) {
      int stackSize = StarlarkEvalWrapper.callStackSize(thread);
      try {
        return ok(Starlark.call(thread, func, Tuple.of(getValue()), Dict.empty()));
      } catch (EvalException | Starlark.UncheckedEvalException e) {
        StarlarkEvalWrapper.unwindCallStack(thread, stackSize);
        return error(e, thread);
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
      final Object res = Starlark.call(thread, func, Tuple.of(getError().getValue()), Dict.empty());
      return error(res, thread);
    } catch (EvalException | Starlark.UncheckedEvalException | InterruptedException e) {
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
    return this.getError().getValue();
  }

  @StarlarkMethod(name = "unwrap", allowReturnNones = true)
  default Object unwrap() throws EvalException {
    if(isOk()) {
      return getValue();
    }
    throw getError().toEvalException();
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
    if(isOk()) {
      return getValue();
    }
    // we know we are an error instance here
    try {
      return Starlark.call(thread, func, Tuple.of(getError().getValue()), Dict.empty());
    } catch (InterruptedException e) {
      throw new EvalException(e.getMessage(), e);
    }
  }

  @StarlarkMethod(name = "unwrap_err", allowReturnNones = true)
  default Object unwrapErr() throws EvalException {
    return expectErr(String.valueOf(getValue()));
  }

  default <E2 extends EvalException> Object orElseRaiseAs(Function<EvalException, E2> emapper) throws E2 {
    if(isOk()) {
      return getValue();
    }
    throw emapper.apply(getError());
  }

  @Override
  default boolean truth() {
    return isOk();
  }

  /**
   * Orders results as the {@code option} library this module ports (and Rust's {@code Result}):
   * every {@code Ok} orders before every {@code Error}; two {@code Ok}s, or two {@code Error}s,
   * order by their wrapped values with Starlark's ordering. Values that cannot be ordered raise
   * Starlark's "unsupported comparison" error (a {@link ClassCastException}, which the comparison
   * operators report as an {@code EvalException}), as {@code Ok("1") <= Ok(1)} raises in Python.
   *
   * <p>Consistent with {@code equals}: equal results compare as 0, and results that compare as 0
   * hold values that Starlark considers equal.
   */
  @Override
  default int compareTo(@NotNull Result o) {
    if (isOk() != o.isOk()) {
      return isOk() ? -1 : 1;
    }
    Object x = Val();
    Object y = o.Val();
    return StarlarkEvalWrapper.compareUnchecked(
      x == null ? Starlark.NONE : x,
      y == null ? Starlark.NONE : y);
  }

}