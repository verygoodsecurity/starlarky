package com.verygood.security.larky.modules.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.verygood.security.larky.objects.descriptor.LarkyDataDescriptor;
import com.verygood.security.larky.parser.CurrentEvaluation;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import org.junit.jupiter.api.Test;

class PropertyTest {

  private static StarlarkCallable accessor(StarlarkThread expectedThread, Tuple expectedArgs) {
    return new StarlarkCallable() {
      @Override
      public String getName() {
        return "accessor";
      }

      @Override
      public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) {
        assertSame(expectedThread, thread);
        assertEquals(expectedArgs, args);
        assertEquals(Dict.empty(), kwargs);
        return "value";
      }
    };
  }

  @Test
  void classAccessReturnsPropertyWithoutCallingGetter() throws Exception {
    try (Mutability mu = Mutability.create("property")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Property property = Property.builder().build();
      LarkyDataDescriptor descriptor = (LarkyDataDescriptor) property;
      assertSame(property, descriptor.__get__(null, null, thread));
      assertSame(property, descriptor.__get__(Starlark.NONE, null, null));
    }
  }

  @Test
  void getWithoutAGetterIsAnEvalException() throws Exception {
    try (Mutability mu = Mutability.create("property")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Property property = Property.builder().build();
      EvalException e = assertThrows(EvalException.class, () -> property.get(thread));
      assertEquals("AttributeError: unreadable attribute", e.getMessage());
      assertEquals("null", property.toString());
    }
  }

  @Test
  void descriptorAccessUsesSuppliedThreadOrFallsBackToCurrentEvaluation() throws Exception {
    try (Mutability current = Mutability.create("current");
         Mutability active = Mutability.create("active")) {
      StarlarkThread currentThread = StarlarkThread.createTransient(current, StarlarkSemantics.DEFAULT);
      StarlarkThread activeThread = StarlarkThread.createTransient(active, StarlarkSemantics.DEFAULT);
      for (StarlarkThread suppliedThread : new StarlarkThread[]{activeThread, null}) {
        StarlarkThread expectedThread = suppliedThread == null ? currentThread : activeThread;
        Property property = Property.builder()
          .fget(accessor(expectedThread, Tuple.of("instance")))
          .fset(accessor(expectedThread, Tuple.of("instance", "assigned")))
          .build();
        LarkyDataDescriptor descriptor = (LarkyDataDescriptor) property;
        try (CurrentEvaluation ignored = CurrentEvaluation.enter(currentThread)) {
          assertEquals("value", descriptor.__get__("instance", null, suppliedThread));
          descriptor.__set__("instance", "assigned", suppliedThread);
        }
      }
    }
  }

  @Test
  void getAndSetUseTheCallersThreadAndNoThreadOutsideAnEvaluationFailsClosed() throws Exception {
    try (Mutability mu = Mutability.create("caller")) {
      StarlarkThread caller = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Property property = Property.builder()
        .fget(accessor(caller, Tuple.empty()))
        .fset(accessor(caller, Tuple.of("assigned")))
        .build();
      assertEquals("value", property.get(caller));
      property.set("assigned", "name", caller);
      IllegalStateException e = assertThrows(IllegalStateException.class, () -> property.get(null));
      assertTrue(e.getMessage().contains("no Starlark evaluation is active"));
      assertThrows(IllegalStateException.class, () -> property.set("assigned", "name", null));
      assertThrows(IllegalStateException.class,
        () -> ((LarkyDataDescriptor) property).__get__("instance", null, null));
    }
  }

  @Test
  void writeOnlyPropertyReadRaisesAttributeErrorAndSetterStillWorks() throws Exception {
    try (Mutability mu = Mutability.create("write-only property")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Property property = Property.builder()
        .fset(accessor(thread, Tuple.of("instance", "assigned")))
        .build();
      for (StarlarkThread suppliedThread : new StarlarkThread[]{thread, null}) {
        assertSame(property, property.__get__(null, null, suppliedThread));
        assertSame(property, property.__get__(Starlark.NONE, null, suppliedThread));
        assertEquals("AttributeError: unreadable attribute",
          assertThrows(EvalException.class,
            () -> property.__get__("instance", null, suppliedThread)).getMessage());
        try (CurrentEvaluation ignored = CurrentEvaluation.enter(thread)) {
          property.__set__("instance", "assigned", suppliedThread);
        }
      }
    }
  }

  @Test
  void unsupportedWritesAndDeletesRaisePropertyErrors() {
    Property property = Property.builder().build();
    LarkyDataDescriptor descriptor = (LarkyDataDescriptor) property;
    // Let the descriptor methods supply the precise errors, including getter-only properties.
    assertFalse(descriptor.readonly());
    assertFalse(descriptor.optional());
    assertEquals("AttributeError: can't set attribute",
      assertThrows(EvalException.class, () -> descriptor.__set__("instance", "value", null)).getMessage());
    assertEquals("AttributeError: can't delete attribute",
      assertThrows(EvalException.class, () -> descriptor.__delete__("instance", null)).getMessage());
  }
}
