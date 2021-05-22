package com.verygood.security.larky.modules.types.results;

import java.util.Objects;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;

public class Ok extends Result {

  private Object value;

  Ok(Object value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Ok result = (Ok) o;
    return Objects.equals(value, result.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "Ok{" + value + '}';
  }

  @Override
  public void str(Printer printer) {
    printer.append(this.toString());
  }

  @Override
  public Object getValue() {
    return this.value;
  }

  @Override
  public EvalException getError() {
    return null;
  }

  @Override
  public boolean isOk() {
    return true;
  }

  @Override
  public boolean isError() {
    return false;
  }
}
