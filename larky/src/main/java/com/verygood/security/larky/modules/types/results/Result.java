package com.verygood.security.larky.modules.types.results;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

import org.jetbrains.annotations.Nullable;


public abstract class Result implements StarlarkValue {

  static class StarlarkException extends EvalException implements StarlarkValue {

    public StarlarkException(String message) {
      super(message);
    }

    public StarlarkException(String message, @Nullable Throwable cause) {
      super(message, cause);
    }

    public StarlarkException(Throwable cause) {
      super(cause);
    }
  }

  @StarlarkMethod(name = "Error", parameters = {@Param(name = "error")})
  public static Result error(Object error) {
    Objects.requireNonNull(error);
    if (EvalException.class.isAssignableFrom(error.getClass())) {
      return new Error((EvalException) error);
    }
    return new Error(new EvalException(Starlark.str(error)));
  }

  @StarlarkMethod(name = "Ok", parameters = {@Param(name = "value")})
  public static Result ok(Object value) {
    Objects.requireNonNull(value);
    return new Ok(value);
  }

  @StarlarkMethod(name = "of", parameters = {@Param(name = "o")})
  public static Result of(Object o) {
    if (o instanceof Exception) {
      return error(o);
    }
    return ok(o);
  }

  @StarlarkMethod(name = "value")
  abstract Object getValue();

  @StarlarkMethod(name = "error")
  abstract EvalException getError();

  @StarlarkMethod(name = "is_ok", structField = true)
  abstract boolean isOk();

  @StarlarkMethod(name = "is_err", structField = true)
  abstract boolean isError();

  // copy https://github.com/MaT1g3R/option/blob/master/option/result.py
  // decided against: https://github.com/dbrgn/result/blob/master/result/result.py
  @StarlarkMethod(name = "map", parameters = {
    @Param(name = "func")
  }, useStarlarkThread = true)
  public <T> Result map(StarlarkCallable func, StarlarkThread thread) {
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

  @StarlarkMethod(name = "map_err", parameters = {
    @Param(name = "func")
  }, useStarlarkThread = true)
  public <T> Result mapError(StarlarkCallable func, StarlarkThread thread) {
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
   * @param s the message to pass to a potential EvalException
   * @throws EvalException if unwrap() is called on an Error value
   */
  @StarlarkMethod(name = "expect",
    doc = "Express the expectation that this object is an Ok value. If it's an Error value " +
            "instead, throw a EvalException with the given message.",
    parameters = {
      @Param(name = "s")
    }
  )
  public Object expect(String s) throws EvalException {
    if (isOk()) {
      return getValue();
    }
    throw new EvalException(s);
  }

  @StarlarkMethod(name = "expect_err", parameters = {
    @Param(name = "msg")
  })
  public Object expectErr(String msg) throws EvalException {
    if (isOk()) {
      throw new EvalException(msg);
    }
    return this.getValue();
  }

  @StarlarkMethod(name = "unwrap")
  public Object unwrap() throws EvalException {
    return orElseRaiseAs(e -> e);
  }

  @StarlarkMethod(name = "unwrap_or_else", parameters = {
    @Param(name = "func")
  }, useStarlarkThread = true)
  public Object unwrapOrElse(StarlarkFunction func, StarlarkThread thread) throws EvalException {
    Object value = getValue();
    if(value != null) {
      return value;
    }
    try {
      return Starlark.call(thread, func, Tuple.of(), Dict.empty());
    } catch (InterruptedException e) {
      throw new EvalException(e.getMessage(), e);
    }
  }

  @StarlarkMethod(name = "unwrap_err")
  public Object unwrapErr() throws EvalException {
    return expectErr(String.valueOf(getValue()));
  }

  <E2 extends EvalException> Object orElseRaiseAs(Function<EvalException, E2> emapper) throws E2 {
    return Optional.ofNullable(getValue())
             .orElseThrow(() -> emapper.apply(getError()));
  }

}