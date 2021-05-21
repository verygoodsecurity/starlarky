package com.verygood.security.larky.modules.types.results;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

public class Result implements StarlarkValue {

  private Object value;
  private EvalException error;

  Result(Object value, EvalException error) {
    this.value = value;
    this.error = error;
  }

  @StarlarkMethod(name = "failure", parameters = {@Param(name = "error")})
  public static Result failure(Object error) {
    Objects.requireNonNull(error);
    if (EvalException.class.isAssignableFrom(error.getClass())) {
      return new Result(null, (EvalException) error);
    }
    return new Result(null, new EvalException(String.valueOf(error)));
  }

  @StarlarkMethod(name = "success", parameters = {@Param(name = "value")})
  public static Result success(Object value) {
    Objects.requireNonNull(value);
    return new Result(value, null);
  }

  @StarlarkMethod(name = "of", parameters = {@Param(name = "o")})
  public static Result of(Object o) {
    if (o instanceof Exception) {
      return failure(o);
    }
    return success(o);
  }

  @StarlarkMethod(name = "either")
  public Object getEither() {
    return value != null ? value : error;
  }

  @StarlarkMethod(name = "value")
  public Object getValue() {
    return value;
  }

  @StarlarkMethod(name = "error")
  public EvalException getError() {
    return error;
  }

  // copy https://github.com/MaT1g3R/option/blob/master/option/result.py
  // decided against: https://github.com/dbrgn/result/blob/master/result/result.py
  @StarlarkMethod(name = "map", parameters = {
    @Param(name = "func")
  }, useStarlarkThread = true)
  public <T> Result map(StarlarkFunction func, StarlarkThread thread) {
    return of(
      Optional.ofNullable(getValue())
        .map((o) -> {
          try {
            return Starlark.call(thread, func, Tuple.of(o), Dict.empty());
          } catch (EvalException | InterruptedException e) {
            throw new RuntimeException(e);
          }
        })
        .orElse(error));
  }

  @StarlarkMethod(name = "is_ok", structField = true)
  public boolean isOk() {
    return this.value != null && this.error == null;
  }

  @StarlarkMethod(name = "is_err", structField = true)
  public boolean isErr() {
    return this.value == null && this.error != null;
  }

  @StarlarkMethod(name = "unwrap")
  public Object unwrap() throws EvalException {
    return orElseRaiseAs(e -> e);
  }

  @StarlarkMethod(name = "unwrap_or_else", parameters = {
    @Param(name = "func")
  }, useStarlarkThread = true)
  public Object unwrapOrElse(StarlarkFunction func, StarlarkThread thread) throws EvalException {
    return
      Optional.ofNullable(value)
        .orElseGet(() -> {
          try {
            return Starlark.call(thread, func, Tuple.of(), Dict.empty());
          } catch (EvalException | InterruptedException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @StarlarkMethod(name = "expect", parameters = {
    @Param(name = "s")
  })
  public Object expect(String s) throws EvalException {
    if (isOk()) {
      return value;
    }
    throw new EvalException(s);
  }

  @StarlarkMethod(name = "unwrap_err")
  public Object unwrapErr() throws EvalException {
    if (isOk()) {
      throw new EvalException(String.valueOf(this.value));
    }
    return this.value;
  }

  @StarlarkMethod(name = "expect_err", parameters = {
    @Param(name = "msg")
  })
  public Object expectErr(String msg) throws EvalException {
    if (isOk()) {
      throw new EvalException(msg);
    }
    return this.value;
  }

  public <E2 extends EvalException> Object orElseRaiseAs(Function<EvalException, E2> emapper) throws E2 {
    return Optional.ofNullable(value).orElseThrow(() -> emapper.apply(error));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Result result = (Result) o;
    return Objects.equals(value, result.value) && Objects.equals(error, result.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, error);
  }

  @Override
  public String toString() {
    return "Result{" + "value=" + value + ", error=" + error + '}';
  }

  @Override
  public void str(Printer printer) {
    printer.append(this.toString());
  }
}