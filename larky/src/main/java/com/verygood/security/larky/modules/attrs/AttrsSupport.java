// Copyright 2026 Very Good Security Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.verygood.security.larky.modules.attrs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.verygood.security.larky.objects.PyObject;
import com.verygood.security.larky.objects.type.LarkyType;
import com.verygood.security.larky.parser.StarlarkUtil;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.starlark.java.eval.BuiltinFunction;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

/**
 * Helpers shared by the Larky port of attrs: Python-style type checks and type names, calls of
 * user callables, and per-thread state.
 *
 * <p>The generated methods and validators are Java so that nesting them (an attrs instance whose
 * field holds another attrs instance, a validator wrapping another validator of the same kind)
 * does not trip Starlark's ban on recursion, which compares the code of the functions on the call
 * stack.
 */
final class AttrsSupport {

  private AttrsSupport() {}

  /** Builtin type constructors and the Starlark type names of their instances. */
  private static final ImmutableMap<String, ImmutableSet<String>> BUILTIN_TYPES =
      ImmutableMap.<String, ImmutableSet<String>>builder()
          .put("int", ImmutableSet.of("int", "bool"))
          .put("bool", ImmutableSet.of("bool"))
          .put("float", ImmutableSet.of("float"))
          .put("str", ImmutableSet.of("string"))
          .put("bytes", ImmutableSet.of("bytes"))
          .put("bytearray", ImmutableSet.of("bytearray"))
          .put("list", ImmutableSet.of("list"))
          .put("tuple", ImmutableSet.of("tuple"))
          .put("dict", ImmutableSet.of("dict"))
          .put("set", ImmutableSet.of("set"))
          .build();

  /** Python's names for the Starlark types whose names differ. */
  private static final ImmutableMap<String, String> PYTHON_TYPE_NAMES =
      ImmutableMap.of("string", "str", "builtin_function_or_method", "builtin_function_or_method");

  /** Whether {@code x} is one of the builtin type constructors, e.g. {@code int} or {@code str}. */
  static boolean isBuiltinType(Object x) {
    return x instanceof BuiltinFunction && BUILTIN_TYPES.containsKey(((BuiltinFunction) x).getName());
  }

  /** Whether {@code x} can be the second argument of {@code isinstance}. */
  static boolean isClass(Object x) {
    return x instanceof LarkyType || isBuiltinType(x);
  }

  /** Python's {@code isinstance(value, spec)} for classes, builtin types and tuples of them. */
  static boolean isInstance(Object value, Object spec) throws EvalException {
    if (spec instanceof Tuple) {
      for (Object s : (Tuple) spec) {
        if (isInstance(value, s)) {
          return true;
        }
      }
      return false;
    }
    if (isBuiltinType(spec)) {
      return BUILTIN_TYPES.get(((BuiltinFunction) spec).getName()).contains(Starlark.type(value));
    }
    if (spec instanceof LarkyType) {
      LarkyType cls = (LarkyType) spec;
      if ("object".equals(cls.__name__()) && cls.getBases().isEmpty()) {
        return true;
      }
      if (value instanceof PyObject) {
        return isSubclass(((PyObject) value).typeClass(), cls);
      }
      return false;
    }
    throw Starlark.errorf(
        "TypeError: isinstance() arg 2 must be a type, a tuple of types, or a union");
  }

  static boolean isSubclass(LarkyType sub, LarkyType sup) {
    if (sub == sup || sub.equals(sup)) {
      return true;
    }
    for (Object base : sub.getMRO()) {
      if (base == sup || base.equals(sup)) {
        return true;
      }
    }
    return false;
  }

  /** {@code repr(t)} as Python prints a type: {@code <class 'int'>} for the builtin {@code int}. */
  static String typeRepr(Object t) {
    if (isBuiltinType(t)) {
      return "<class '" + ((BuiltinFunction) t).getName() + "'>";
    }
    if (t instanceof Tuple) {
      StringBuilder sb = new StringBuilder("(");
      Tuple tuple = (Tuple) t;
      for (int i = 0; i < tuple.size(); i++) {
        if (i > 0) {
          sb.append(", ");
        }
        sb.append(typeRepr(tuple.get(i)));
      }
      return sb.append(tuple.size() == 1 ? ",)" : ")").toString();
    }
    return repr(t);
  }

  /** {@code repr(value.__class__)} as Python prints it, e.g. {@code <class 'str'>}. */
  static String classRepr(Object value) {
    if (value instanceof PyObject) {
      return repr(((PyObject) value).typeClass());
    }
    if (value instanceof LarkyType) {
      return "<class 'type'>";
    }
    String name = Starlark.type(value);
    return "<class '" + PYTHON_TYPE_NAMES.getOrDefault(name, name) + "'>";
  }

  /** Python's {@code type(x).__name__}. */
  static String pythonTypeName(Object x) {
    if (x instanceof PyObject) {
      return ((PyObject) x).typeName();
    }
    if (x == null) {
      return "NoneType";
    }
    String name = Starlark.type(x);
    return PYTHON_TYPE_NAMES.getOrDefault(name, name);
  }

  static String repr(Object x) {
    return Starlark.repr(x, StarlarkSemantics.DEFAULT);
  }

  static String str(Object x) {
    return Starlark.str(x, StarlarkSemantics.DEFAULT);
  }

  /** Calls {@code fn} with positional {@code args}; {@code fn} may be an object with {@code __call__}. */
  static Object call(StarlarkThread thread, Object fn, Object... args)
      throws EvalException, InterruptedException {
    StarlarkCallable callable = fn instanceof StarlarkCallable ? (StarlarkCallable) fn : StarlarkUtil.toCallable(fn);
    return Starlark.call(thread, callable, Tuple.of(args), Dict.empty());
  }

  static boolean isCallable(Object x) throws EvalException {
    return StarlarkUtil.isCallable(x);
  }

  static boolean isNone(Object x) {
    return x == null || x == Starlark.NONE;
  }

  static Object binaryOp(net.starlark.java.syntax.TokenKind op, Object x, Object y, StarlarkThread thread)
      throws EvalException {
    return StarlarkEvalWrapper.binaryOp(op, x, y, thread);
  }

  /** Python's {@code getattr(x, name)}. */
  static Object getattr(StarlarkThread thread, Object x, String name) throws EvalException, InterruptedException {
    return Starlark.getattr(thread.mutability(), thread.getSemantics(), x, name, null);
  }

  /** Python's {@code getattr(x, name, None)}, returning Java null for a missing attribute. */
  static Object getattrOrNull(StarlarkThread thread, Object x, String name)
      throws EvalException, InterruptedException {
    Object v = Starlark.getattr(thread.mutability(), thread.getSemantics(), x, name, Starlark.NONE);
    return v == Starlark.NONE ? null : v;
  }

  /** The attrs attributes of class {@code cls}, or null if it is not an attrs class. */
  static AttrsFields attrsOf(StarlarkThread thread, Object cls) throws EvalException, InterruptedException {
    if (!(cls instanceof LarkyType)) {
      return null;
    }
    Object attrs = ((LarkyType) cls).lookup("__attrs_attrs__");
    return attrs instanceof AttrsFields ? (AttrsFields) attrs : null;
  }

  /** The attrs attributes of {@code inst}'s class, or null if it is not an attrs instance. */
  static AttrsFields attrsOfInstance(StarlarkThread thread, Object inst) throws EvalException, InterruptedException {
    return inst instanceof PyObject ? attrsOf(thread, ((PyObject) inst).typeClass()) : null;
  }

  /** Reads attribute {@code name} of an attrs instance. */
  static Object getField(StarlarkThread thread, Object inst, String name) throws EvalException, InterruptedException {
    return getattr(thread, inst, name);
  }

  /** Stores {@code value} in the instance dictionary, bypassing descriptors and on_setattr hooks. */
  static void storeField(Object inst, String name, Object value) throws EvalException {
    ((PyObject) inst).setItemUnsafe(name, value);
  }

  /** Whether validators run (attrs' {@code _config._run_validators}), per Starlark thread. */
  static final class RunValidators {
    boolean value = true;
  }

  static boolean runValidators(StarlarkThread thread) {
    RunValidators state = thread.getThreadLocal(RunValidators.class);
    return state == null || state.value;
  }

  static void setRunValidators(StarlarkThread thread, boolean run) {
    RunValidators state = thread.getThreadLocal(RunValidators.class);
    if (state == null) {
      state = new RunValidators();
      thread.setThreadLocal(RunValidators.class, state);
    }
    state.value = run;
  }

  /** attrs' {@code _CountingAttr.cls_counter}, per Starlark thread rather than per process. */
  static final class Counter {
    long value;
  }

  static long nextCounter(StarlarkThread thread) {
    Counter counter = thread.getThreadLocal(Counter.class);
    if (counter == null) {
      counter = new Counter();
      thread.setThreadLocal(Counter.class, counter);
    }
    return ++counter.value;
  }

  /** Instances whose repr is being computed on a thread, so a cycle prints {@code ...}. */
  static final class ReprGuard {
    final Set<Object> active = Collections.newSetFromMap(new IdentityHashMap<>());
  }

  static Set<Object> reprGuard(StarlarkThread thread) {
    ReprGuard guard = thread.getThreadLocal(ReprGuard.class);
    if (guard == null) {
      guard = new ReprGuard();
      thread.setThreadLocal(ReprGuard.class, guard);
    }
    return guard.active;
  }

  /** Joins {@code 'a'}, {@code 'a' and 'b'}, {@code 'a', 'b', and 'c'} as CPython's argument errors do. */
  static String joinNames(List<String> names) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < names.size(); i++) {
      if (i > 0) {
        if (names.size() == 2) {
          sb.append(" and ");
        } else if (i == names.size() - 1) {
          sb.append(", and ");
        } else {
          sb.append(", ");
        }
      }
      sb.append('\'').append(names.get(i)).append('\'');
    }
    return sb.toString();
  }

  static EvalException error(String type, String msg) {
    return new EvalException(type + ": " + msg);
  }
}
