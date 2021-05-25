package com.verygood.security.larky.modules.types.results;

import java.util.Objects;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;


public class Error implements Result {

  private final Object errValue;
  private final EvalException exc;

  Error(Object error) {
    if(EvalException.class.isAssignableFrom(error.getClass())) {
      this.errValue = ((EvalException) error).getMessage();
      this.exc = ((EvalException) error);
    }
    else {
      this.errValue = error;
      this.exc = new EvalException(Starlark.str(error));
    }
  }

  Error(EvalException error) {
    this.errValue = error.getMessage();
    this.exc = error;
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
  public void str(Printer printer) {
    printer.append(this.toString());
  }

  @Override
  public Object getValue() {
    return null;
  }

  @Override
  public EvalException getError() {
    return this.exc;
  }

  @Override
  public boolean isOk() {
    return false;
  }

  @Override
  public boolean isError() {
    return true;
  }
}
