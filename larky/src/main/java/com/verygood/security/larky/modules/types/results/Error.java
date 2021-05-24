package com.verygood.security.larky.modules.types.results;

import java.util.Objects;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;

public class Error extends Result {

  private EvalException error;

  Error(EvalException error) {
    this.error = error;
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
    return Objects.equals(error.getMessage(), result.error.getMessage());
  }

  @Override
  public int hashCode() {
    return Objects.hash(error);
  }

  @Override
  public String toString() {
    return "Error{" + error + '}';
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
    return this.error;
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
