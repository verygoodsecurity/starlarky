/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky.wasm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Validates and caches unbound method handles; only explicitly supplied module classes are scanned. */
final class WasiHostRegistry {
  private WasiHostRegistry() {}

  /** Declared methods only: the module owns its entire host surface without inherited exports. */
  static Map<String, Descriptor> discover(Class<?> moduleClass) {
    Map<String, Descriptor> descriptors = new LinkedHashMap<>();
    Method[] methods = moduleClass.getDeclaredMethods();
    Arrays.sort(methods, Comparator.comparing(Method::toGenericString));
    for (Method method : methods) {
      WasiHostFunction function = method.getAnnotation(WasiHostFunction.class);
      if (function == null || method.isSynthetic()) {
        continue;
      }
      validate(method, function);
      if (descriptors.putIfAbsent(function.name(), new Descriptor(method)) != null) {
        throw new IllegalArgumentException("duplicate WASI function: " + function.name());
      }
    }
    return Map.copyOf(descriptors);
  }

  private static void validate(Method method, WasiHostFunction function) {
    String signature = function.signature();
    if (!function.name().matches("[a-z][a-z0-9_]*")
        || !signature.matches("[iI]*:i?")
        || (!function.implemented() && signature.endsWith(":"))) {
      throw new IllegalArgumentException("invalid WASI signature or name: " + function.name());
    }
    int count = signature.indexOf(':');
    Class<?>[] types = method.getParameterTypes();
    boolean valid = !Modifier.isStatic(method.getModifiers())
        && !Modifier.isAbstract(method.getModifiers())
        && types.length == count + 1 && types[0] == WasiHost.GuestMemory.class
        && method.getReturnType() == (signature.endsWith(":") ? void.class : int.class);
    for (int i = 0; valid && i < count; i++) {
      valid = types[i + 1] == (signature.charAt(i) == 'i' ? int.class : long.class);
    }
    if (!valid) {
      throw new IllegalArgumentException("Java method does not match WASI signature: " + method);
    }
  }

  static final class Descriptor {
    private final MethodHandle handle;
    private final String signature;
    private final boolean implemented;

    Descriptor(Method method) {
      WasiHostFunction function = method.getAnnotation(WasiHostFunction.class);
      signature = function.signature();
      implemented = function.implemented();
      try {
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(method.getDeclaringClass(),
            MethodHandles.lookup());
        MethodHandle target = lookup.unreflect(method);
        // Adapters store both i32 and i64 in longs. Narrow i32 without losing its unsigned bits.
        Class<?>[] arguments = new Class<?>[method.getParameterCount() + 1];
        arguments[0] = Object.class;
        arguments[1] = WasiHost.GuestMemory.class;
        Arrays.fill(arguments, 2, arguments.length, long.class);
        target = MethodHandles.explicitCastArguments(target, MethodType.methodType(int.class, arguments));
        handle = target.asSpreader(long[].class, arguments.length - 2);
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("cannot bind WASI method: " + method, e);
      }
    }

    String signature() {
      return signature;
    }

    boolean implemented() {
      return implemented;
    }

    WasiHost.Function bind(Object module) {
      MethodHandle bound = handle.bindTo(module);
      return (args, memory) -> {
        try {
          return (int) bound.invokeExact(memory, args);
        } catch (RuntimeException | Error e) {
          // Exit, memory traps, deadline stops and output limits must reach the runtime unchanged.
          throw e;
        } catch (Throwable e) {
          throw new IllegalStateException("WASI function threw a checked exception", e);
        }
      };
    }
  }
}
