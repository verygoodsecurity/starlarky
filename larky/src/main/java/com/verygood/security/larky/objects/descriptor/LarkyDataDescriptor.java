package com.verygood.security.larky.objects.descriptor;

import com.google.common.collect.ImmutableCollection;
import java.util.Map;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Structure;

import org.jetbrains.annotations.Nullable;

public interface LarkyDataDescriptor extends LarkyNonDataDescriptor {
  static boolean isDataDescriptor(Object obj) {
    boolean result = false;
    if (obj instanceof LarkyDataDescriptor) {
      result = true;
    } else if (obj instanceof Structure) {
      final ImmutableCollection<String> fieldNames = ((Structure) obj).getFieldNames();
      result = fieldNames.contains("__set__") || fieldNames.contains("__delete__");
    } else if (obj instanceof Map) {
      final Map<?, ?> obj1 = (Map<?, ?>) obj;
      result = obj1.containsKey("__set__") || obj1.containsKey("__delete__");
    }
    return result;
  }

  void __set__(Object obj, Object value, @Nullable StarlarkThread thread)
    throws EvalException, InterruptedException;

  void __delete__(Object obj, @Nullable StarlarkThread thread)
    throws EvalException, InterruptedException;
}
