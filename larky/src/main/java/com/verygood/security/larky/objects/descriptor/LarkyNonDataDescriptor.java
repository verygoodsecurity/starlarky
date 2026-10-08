package com.verygood.security.larky.objects.descriptor;

import com.google.common.collect.ImmutableCollection;
import java.util.Map;

import com.verygood.security.larky.objects.PyObject;
import com.verygood.security.larky.objects.type.LarkyType;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Structure;

import org.jetbrains.annotations.Nullable;


public interface LarkyNonDataDescriptor {
  static boolean isNonDataDescriptor(Object obj) {
    boolean result = false;
    if (obj instanceof LarkyNonDataDescriptor) {
      result = true;
    } else if (obj instanceof Structure) {
      final ImmutableCollection<String> fieldNames = ((Structure) obj).getFieldNames();
      result = fieldNames.contains("__get__");
    } else if (obj instanceof Map) {
      final Map<?, ?> obj1 = (Map<?, ?>) obj;
      result = obj1.containsKey("__get__");
    }
    return result;
  }

  /** Reads a protocol method from any object accepted by descriptor detection. */
  static Object descriptorMethod(Object desc, String name, StarlarkThread thread) throws EvalException {
    if (desc instanceof PyObject) {
      return ((PyObject) desc).getField(name, thread);
    }
    if (desc instanceof Structure) {
      return ((Structure) desc).getValue(name);
    }
    if (desc instanceof Map) {
      return ((Map<?, ?>) desc).get(name);
    }
    throw Starlark.errorf("'%s' is not a descriptor", Starlark.type(desc));
  }

  // These are the prototypes for the descriptor protocol.
  Object __get__(Object obj, LarkyType type, @Nullable StarlarkThread thread)
    throws EvalException, InterruptedException;

  /**
   * The attribute may not be set or deleted.
   *
   * @return true if the attribute may not be set or deleted
   */
  boolean readonly();

  /**
   * The attribute may be deleted.
   *
   * @return true if the attribute may be deleted.
   */
  boolean optional();
}
