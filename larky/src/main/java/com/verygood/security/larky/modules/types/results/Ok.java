package com.verygood.security.larky.modules.types.results;

import java.util.Objects;

import net.starlark.java.eval.Printer;
import net.starlark.java.eval.StarlarkSemantics;

public class Ok implements Result {

  private final Object value;

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
    return Objects.hash(this.getClass(), this.isOk(), value);
  }

  @Override
  public String toString() {
    return "Ok{" + value + '}';
  }

  @Override
  public void repr(Printer printer, StarlarkSemantics semantics) {
    printer.append(this.toString());
  }

  @Override
  public void str(Printer printer, StarlarkSemantics semantics) {
    printer.append(String.valueOf(value));
  }

  @Override
  public Object getValue() {
    return this.value;
  }

  @Override
  public Error getError() {
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
