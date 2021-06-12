package com.verygood.security.larky.modules.types.results;

import java.util.Objects;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;


public class Error extends EvalException implements Result {

  private final Object errValue;
  private final EvalException exc;

  private Error(EvalException error) {
    super(error.getMessage(), error.getCause());
    this.errValue = error.getMessage();
    this.exc = error;
  }

  private Error(Error errorObj) {
    super(errorObj.exc.getMessage(), errorObj.exc.getCause());
    this.errValue = errorObj.exc.getMessage();
    this.exc = errorObj.exc;
  }

  private Error(Object errorObj) {
    super(Starlark.str(errorObj));
    this.errValue = errorObj;
    this.exc = new EvalException(Starlark.str(errorObj));
  }

  public static Error of(Object e) {
    if(Error.class.isAssignableFrom(e.getClass())) {
      return new Error(e);
    }
    else if(EvalException.class.isAssignableFrom(e.getClass())) {
      return new Error((EvalException) e);
    }
    return new Error(e);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Error result = (Error) o;
    return Objects.equals(errValue, result.errValue);
  }

  @Override
  public int hashCode() {
    // return hash((self._type, self._is_ok, self._val))
    return Objects.hash(this.getClass(), this.isOk(), errValue);
    //return Objects.hash(errValue);
  }

  @Override
  public String toString() {
    return "Error{" + errValue + '}';
  }

  @Override
  public void repr(Printer printer) {
    printer.append(this.toString());
  }

  @Override
  public void str(Printer printer) {
    printer.append(String.valueOf(errValue));
  }

  @Override
  public Object getValue() {
    return this.errValue;
  }

  @Override
  public Error getError() {
    return this;
  }

  @Override
  public boolean isOk() {
    return false;
  }

  @Override
  public boolean isError() {
    return true;
  }

  public EvalException toEvalException() {
    return this.exc;
  }
}
