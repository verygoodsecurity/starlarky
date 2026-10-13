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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.verygood.security.larky.objects.PyObject;
import com.verygood.security.larky.objects.type.LarkyType;

import net.starlark.java.eval.BuiltinFunction;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSet;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

/** attrs' module-level functions that work on instances: fields, validate, evolve, asdict, astuple. */
final class AttrsFunctions {

  private AttrsFunctions() {}

  /** attrs' {@code fields(cls)}; also accepts an attrs instance, as attrs 26.1 does. */
  static AttrsFields fields(StarlarkThread thread, Object cls) throws EvalException, InterruptedException {
    if (!AttrsSupport.isClass(cls)) {
      AttrsFields attrs = AttrsSupport.attrsOfInstance(thread, cls);
      if (attrs == null) {
        throw AttrsSupport.error("TypeError", "Passed object must be a class or attrs instance.");
      }
      return attrs;
    }
    AttrsFields attrs = AttrsSupport.attrsOf(thread, cls);
    if (attrs == null) {
      throw AttrsSupport.error("NotAnAttrsClassError",
          AttrsSupport.typeRepr(cls) + " is not an attrs-decorated class.");
    }
    return attrs;
  }

  static Dict<String, Object> fieldsDict(StarlarkThread thread, Object cls) throws EvalException, InterruptedException {
    if (!AttrsSupport.isClass(cls)) {
      throw AttrsSupport.error("TypeError", "Passed object must be a class.");
    }
    Dict<String, Object> rv = Dict.of(thread.mutability());
    for (Attribute a : fields(thread, cls).attributes()) {
      rv.putEntry(a.name(), a);
    }
    return rv;
  }

  /**
   * attrs' {@code fields(inst.__class__)}: a class or any other non-attrs value is rejected by
   * its class, e.g. {@code <class 'type'> is not an attrs-decorated class.}
   */
  static AttrsFields fieldsOfInstance(StarlarkThread thread, Object inst) throws EvalException, InterruptedException {
    AttrsFields attrs = AttrsSupport.attrsOfInstance(thread, inst);
    if (attrs == null) {
      throw AttrsSupport.error("NotAnAttrsClassError",
          AttrsSupport.classRepr(inst) + " is not an attrs-decorated class.");
    }
    return attrs;
  }

  static void validate(StarlarkThread thread, Object inst) throws EvalException, InterruptedException {
    if (!AttrsSupport.runValidators(thread)) {
      return;
    }
    for (Attribute a : fieldsOfInstance(thread, inst).attributes()) {
      Object v = a.validator();
      if (!AttrsSupport.isNone(v)) {
        Validators.run(thread, v, inst, a, AttrsSupport.getField(thread, inst, a.name()));
      }
    }
  }

  static Object evolve(StarlarkThread thread, Tuple args, Dict<String, Object> changes)
      throws EvalException, InterruptedException {
    if (args.size() != 1) {
      throw AttrsSupport.error("TypeError",
          "evolve() takes 1 positional argument, but " + args.size() + " were given");
    }
    Object inst = args.get(0);
    Dict<String, Object> kwargs = Dict.of(thread.mutability());
    kwargs.putEntries(changes);
    for (Attribute a : fieldsOfInstance(thread, inst).attributes()) {
      if (!a.init()) {
        continue;
      }
      if (!kwargs.containsKey(a.alias())) {
        kwargs.putEntry(a.alias(), AttrsSupport.getField(thread, inst, a.name()));
      }
    }
    return Starlark.call(thread, ((PyObject) inst).typeClass(), Tuple.empty(), kwargs);
  }

  /** The types attrs copies as is when recursing. */
  private static boolean isAtomic(Object v) {
    if (AttrsSupport.isNone(v) || v instanceof LarkyType || AttrsSupport.isBuiltinType(v)) {
      return true;
    }
    switch (Starlark.type(v)) {
      case "bool", "int", "float", "string", "bytes", "range":
        return true;
      default:
        return false;
    }
  }

  private static boolean isCollection(Object v) {
    return v instanceof StarlarkList || v instanceof Tuple || v instanceof StarlarkSet;
  }

  private static boolean isNamed(Object fn, String name) {
    return fn instanceof BuiltinFunction && ((BuiltinFunction) fn).getName().equals(name);
  }

  /** Builds a collection of {@code items} of the same kind as {@code like}, or a list/tuple. */
  private static Object collection(Mutability mu, Object like, List<Object> items) throws EvalException {
    if (like instanceof Tuple) {
      return Tuple.copyOf(items);
    }
    if (like instanceof StarlarkSet) {
      return StarlarkSet.copyOf(mu, items);
    }
    return StarlarkList.copyOf(mu, items);
  }

  static final class AsDict {
    final StarlarkThread thread;
    final Object filter;
    final Object dictFactory;
    final boolean retain;
    final Object valueSerializer;

    AsDict(StarlarkThread thread, Object filter, Object dictFactory, boolean retain, Object valueSerializer) {
      this.thread = thread;
      this.filter = AttrsSupport.isNone(filter) ? null : filter;
      this.dictFactory = dictFactory;
      this.retain = retain;
      this.valueSerializer = AttrsSupport.isNone(valueSerializer) ? null : valueSerializer;
    }

    /** {@code dict_factory()} filled with {@code pairs}. */
    private Object makeDict(List<Object[]> pairs) throws EvalException, InterruptedException {
      if (isNamed(dictFactory, "dict")) {
        Dict<Object, Object> d = Dict.of(thread.mutability());
        for (Object[] p : pairs) {
          d.putEntry(p[0], p[1]);
        }
        return d;
      }
      Object d = AttrsSupport.call(thread, dictFactory);
      for (Object[] p : pairs) {
        if (d instanceof Dict) {
          @SuppressWarnings("unchecked")
          Dict<Object, Object> dd = (Dict<Object, Object>) d;
          dd.putEntry(p[0], p[1]);
        } else {
          AttrsSupport.call(thread, AttrsSupport.getattr(thread, d, "__setitem__"), p[0], p[1]);
        }
      }
      return d;
    }

    Object asdict(Object inst, boolean recurse) throws EvalException, InterruptedException {
      List<Object[]> rv = new ArrayList<>();
      for (Attribute a : fieldsOfInstance(thread, inst).attributes()) {
        Object v = AttrsSupport.getField(thread, inst, a.name());
        if (filter != null && !Starlark.truth(AttrsSupport.call(thread, filter, a, v))) {
          continue;
        }
        if (valueSerializer != null) {
          v = AttrsSupport.call(thread, valueSerializer, inst, a, v);
        }
        if (recurse) {
          if (isAtomic(v)) {
            // as is
          } else if (AttrsSupport.attrsOfInstance(thread, v) != null) {
            v = asdict(v, true);
          } else if (isCollection(v)) {
            List<Object> items = new ArrayList<>();
            for (Object i : Starlark.toIterable(v)) {
              items.add(anything(i, false));
            }
            v = retain ? collection(thread.mutability(), v, items) : StarlarkList.copyOf(thread.mutability(), items);
          } else if (v instanceof Dict) {
            v = dictOf((Dict<?, ?>) v);
          }
        }
        rv.add(new Object[] {a.name(), v});
      }
      return makeDict(rv);
    }

    private Object dictOf(Dict<?, ?> d) throws EvalException, InterruptedException {
      List<Object[]> pairs = new ArrayList<>();
      for (Map.Entry<?, ?> e : d.entrySet()) {
        pairs.add(new Object[] {anything(e.getKey(), true), anything(e.getValue(), false)});
      }
      return makeDict(pairs);
    }

    /** attrs' {@code _asdict_anything}. */
    Object anything(Object val, boolean isKey) throws EvalException, InterruptedException {
      if (isAtomic(val)) {
        return valueSerializer != null ? AttrsSupport.call(thread, valueSerializer, Starlark.NONE, Starlark.NONE, val) : val;
      }
      if (AttrsSupport.attrsOfInstance(thread, val) != null) {
        return asdict(val, true);
      }
      if (isCollection(val)) {
        List<Object> items = new ArrayList<>();
        for (Object i : Starlark.toIterable(val)) {
          items.add(anything(i, false));
        }
        if (retain) {
          return collection(thread.mutability(), val, items);
        }
        return isKey ? Tuple.copyOf(items) : StarlarkList.copyOf(thread.mutability(), items);
      }
      if (val instanceof Dict) {
        return dictOf((Dict<?, ?>) val);
      }
      return valueSerializer != null ? AttrsSupport.call(thread, valueSerializer, Starlark.NONE, Starlark.NONE, val) : val;
    }
  }

  static final class AsTuple {
    final StarlarkThread thread;
    final Object filter;
    final Object tupleFactory;
    final boolean retain;

    AsTuple(StarlarkThread thread, Object filter, Object tupleFactory, boolean retain) {
      this.thread = thread;
      this.filter = AttrsSupport.isNone(filter) ? null : filter;
      this.tupleFactory = tupleFactory;
      this.retain = retain;
    }

    private Object nested(Object v) throws EvalException, InterruptedException {
      return AttrsSupport.attrsOfInstance(thread, v) != null ? astuple(v, true) : v;
    }

    Object astuple(Object inst, boolean recurse) throws EvalException, InterruptedException {
      List<Object> rv = new ArrayList<>();
      for (Attribute a : fieldsOfInstance(thread, inst).attributes()) {
        Object v = AttrsSupport.getField(thread, inst, a.name());
        if (filter != null && !Starlark.truth(AttrsSupport.call(thread, filter, a, v))) {
          continue;
        }
        if (recurse) {
          if (isAtomic(v)) {
            // as is
          } else if (AttrsSupport.attrsOfInstance(thread, v) != null) {
            v = astuple(v, true);
          } else if (isCollection(v)) {
            List<Object> items = new ArrayList<>();
            for (Object j : Starlark.toIterable(v)) {
              items.add(nested(j));
            }
            v = retain ? collection(thread.mutability(), v, items) : StarlarkList.copyOf(thread.mutability(), items);
          } else if (v instanceof Dict) {
            Dict<Object, Object> d = Dict.of(thread.mutability());
            for (Map.Entry<?, ?> e : ((Dict<?, ?>) v).entrySet()) {
              d.putEntry(nested(e.getKey()), nested(e.getValue()));
            }
            v = d;
          }
        }
        rv.add(v);
      }
      if (isNamed(tupleFactory, "list")) {
        return StarlarkList.copyOf(thread.mutability(), rv);
      }
      if (isNamed(tupleFactory, "tuple")) {
        return Tuple.copyOf(rv);
      }
      return AttrsSupport.call(thread, tupleFactory, StarlarkList.copyOf(thread.mutability(), rv));
    }
  }
}
