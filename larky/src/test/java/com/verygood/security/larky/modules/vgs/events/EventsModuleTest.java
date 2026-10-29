package com.verygood.security.larky.modules.vgs.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import org.junit.jupiter.api.Test;

class EventsModuleTest {

  @Test
  void recordWithoutThreadLocalIsNoOp() throws Exception {
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = new StarlarkThread(mu, StarlarkSemantics.DEFAULT);
      Object result =
          EventsModule.INSTANCE.record("evt", Starlark.NONE, thread);
      assertEquals(Starlark.NONE, result);
    }
  }
}
