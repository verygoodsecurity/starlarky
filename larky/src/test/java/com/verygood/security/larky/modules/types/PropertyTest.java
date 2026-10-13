package com.verygood.security.larky.modules.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.verygood.security.larky.objects.descriptor.LarkyDataDescriptor;

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
      Property property = Property.builder().thread(thread).build();
      LarkyDataDescriptor descriptor = (LarkyDataDescriptor) property;
      assertSame(property, descriptor.__get__(null, null, thread));
      assertSame(property, descriptor.__get__(Starlark.NONE, null, null));
    }
  }

  @Test
  void descriptorAccessUsesActiveThreadOrFallsBackToCreatingThread() throws Exception {
    try (Mutability creating = Mutability.create("creating");
         Mutability active = Mutability.create("active")) {
      StarlarkThread creatingThread = StarlarkThread.createTransient(creating, StarlarkSemantics.DEFAULT);
      StarlarkThread activeThread = StarlarkThread.createTransient(active, StarlarkSemantics.DEFAULT);
      for (StarlarkThread suppliedThread : new StarlarkThread[]{activeThread, null}) {
        StarlarkThread expectedThread = suppliedThread == null ? creatingThread : activeThread;
        Property property = Property.builder()
          .thread(creatingThread)
          .fget(accessor(expectedThread, Tuple.of("instance")))
          .fset(accessor(expectedThread, Tuple.of("instance", "assigned")))
          .build();
        LarkyDataDescriptor descriptor = (LarkyDataDescriptor) property;
        assertEquals("value", descriptor.__get__("instance", null, suppliedThread));
        descriptor.__set__("instance", "assigned", suppliedThread);
      }
    }
  }

  @Test
  void writeOnlyPropertyReadRaisesAttributeErrorAndSetterStillWorks() throws Exception {
    try (Mutability mu = Mutability.create("write-only property")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Property property = Property.builder()
        .thread(thread)
        .fset(accessor(thread, Tuple.of("instance", "assigned")))
        .build();
      for (StarlarkThread suppliedThread : new StarlarkThread[]{thread, null}) {
        assertSame(property, property.__get__(null, null, suppliedThread));
        assertSame(property, property.__get__(Starlark.NONE, null, suppliedThread));
        assertEquals("AttributeError: unreadable attribute",
          assertThrows(EvalException.class,
            () -> property.__get__("instance", null, suppliedThread)).getMessage());
        property.__set__("instance", "assigned", suppliedThread);
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
