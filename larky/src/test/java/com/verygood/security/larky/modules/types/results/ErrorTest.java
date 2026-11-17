package com.verygood.security.larky.modules.types.results;

import static com.google.common.truth.Truth.assertThat;

import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import org.junit.Test;

/** How {@link Error#withThread} turns an unchecked exception into an error message. */
public class ErrorTest {

  private static Starlark.UncheckedEvalException unchecked(RuntimeException cause) {
    StarlarkThread thread =
        StarlarkThread.createTransient(Mutability.create("test"), StarlarkSemantics.DEFAULT);
    return new Starlark.UncheckedEvalException(cause, thread) {};
  }

  @Test
  public void usesTheCausesMessage() {
    Error error = Error.withThread(unchecked(new IllegalStateException("bad state")), null);
    assertThat(error.getMessage()).isEqualTo("bad state");
  }

  @Test
  public void causeWithoutMessageNamesItsClass() {
    Error error = Error.withThread(unchecked(new NullPointerException()), null);
    assertThat(error.getMessage())
        .startsWith("NullPointerException thrown during Starlark evaluation");
    assertThat(error.getCause()).isInstanceOf(NullPointerException.class);
  }
}
