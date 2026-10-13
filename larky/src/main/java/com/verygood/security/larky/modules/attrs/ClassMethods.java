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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.verygood.security.larky.objects.PyObject;
import com.verygood.security.larky.objects.descriptor.LarkyDataDescriptor;
import com.verygood.security.larky.objects.type.LarkyType;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.TokenKind;

/**
 * The methods attrs writes for a class (attrs generates and exec()s their source; Larky has no
 * exec, so each is a Java callable configured with the class's attributes).
 */
final class ClassMethods {

  private ClassMethods() {}

  /** A generated method; its first argument is the instance. */
  abstract static class Method implements StarlarkCallable {
    final String qualname;

    Method(String qualname) {
      this.qualname = qualname;
    }

    @Override
    public String getName() {
      return qualname.substring(qualname.lastIndexOf('.') + 1);
    }

    @Override
    public void repr(Printer printer, StarlarkSemantics semantics) {
      printer.append("<function ").append(qualname).append(">");
    }

    @Override
    public boolean isImmutable() {
      return true;
    }

    static PyObject self(Tuple args, String qualname) throws EvalException {
      if (args.isEmpty() || !(args.get(0) instanceof PyObject)) {
        throw Starlark.errorf("TypeError: %s() needs an instance as its first argument", qualname);
      }
      return (PyObject) args.get(0);
    }
  }

  static Object instanceValue(PyObject self, String name) {
    return self.getInternalDictUnsafe().get(name);
  }

  /** {@code self.name} as the generated methods read it; null if the instance has no such attribute. */
  static Object fieldValue(PyObject self, String name, StarlarkThread thread) {
    Object v = instanceValue(self, name);
    if (v != null || self.typeClass().lookup(name) instanceof Field) {
      return v;
    }
    return self.getField(name, thread);
  }

  // __init__

  static final class Init extends Method {
    private final List<Attribute> attrs;

    Init(String className, List<Attribute> attrs) {
      super(className + ".__init__");
      this.attrs = attrs;
    }

    private static boolean hasDefault(Attribute a) {
      return a.dflt() != Nothing.NOTHING;
    }

    private static String plural(int n, String word) {
      return n + " " + word + (n == 1 ? "" : "s");
    }

    @Override
    public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs)
        throws EvalException, InterruptedException {
      PyObject self = self(args, qualname);
      List<Attribute> positional = new ArrayList<>();
      List<Attribute> kwOnly = new ArrayList<>();
      Map<String, Attribute> byAlias = new HashMap<>();
      for (Attribute a : attrs) {
        if (!a.init()) {
          continue;
        }
        (a.kwOnly() ? kwOnly : positional).add(a);
        byAlias.put(a.alias(), a);
      }

      int given = args.size() - 1;
      if (given > positional.size()) {
        int required = 0;
        for (Attribute a : positional) {
          if (!hasDefault(a)) {
            required++;
          }
        }
        String takes = required == positional.size()
            ? plural(positional.size() + 1, "positional argument")
            : "from " + (required + 1) + " to " + (positional.size() + 1) + " positional arguments";
        int kwGiven = 0;
        for (Attribute a : kwOnly) {
          if (kwargs.containsKey(a.alias())) {
            kwGiven++;
          }
        }
        String givenStr = kwGiven > 0
            ? plural(given + 1, "positional argument") + " (and " + plural(kwGiven, "keyword-only argument") + ")"
            : String.valueOf(given + 1);
        throw Starlark.errorf("TypeError: %s() takes %s but %s were given", qualname, takes, givenStr);
      }

      Map<String, Object> values = new HashMap<>();
      for (int i = 0; i < given; i++) {
        values.put(positional.get(i).alias(), args.get(i + 1));
      }
      for (Map.Entry<String, Object> e : kwargs.entrySet()) {
        Attribute a = byAlias.get(e.getKey());
        if (a == null) {
          throw Starlark.errorf(
              "TypeError: %s() got an unexpected keyword argument '%s'", qualname, e.getKey());
        }
        if (values.containsKey(e.getKey())) {
          throw Starlark.errorf(
              "TypeError: %s() got multiple values for argument '%s'", qualname, e.getKey());
        }
        values.put(e.getKey(), e.getValue());
      }
      checkMissing(positional, values, "positional");
      checkMissing(kwOnly, values, "keyword-only");

      if (self.typeClass().lookup("__attrs_pre_init__") != null) {
        AttrsSupport.call(thread, self.getField("__attrs_pre_init__", thread));
      }

      List<Attribute> assigned = new ArrayList<>();
      for (Attribute a : attrs) {
        if (!a.init() && !hasDefault(a)) {
          continue;
        }
        Object dflt = a.dflt();
        Object val = a.init() ? values.get(a.alias()) : null;
        if (val == null || (val == Nothing.NOTHING && dflt instanceof Factory)) {
          if (dflt instanceof Factory) {
            Factory f = (Factory) dflt;
            val = f.takesSelf
                ? AttrsSupport.call(thread, f.factory, self)
                : AttrsSupport.call(thread, f.factory);
          } else {
            val = dflt;
          }
        }
        Object converter = a.converter();
        if (!AttrsSupport.isNone(converter)) {
          val = AttrsSupport.call(thread, converter, val);
        }
        AttrsSupport.storeField(self, a.name(), val);
        assigned.add(a);
      }

      if (AttrsSupport.runValidators(thread)) {
        for (Attribute a : assigned) {
          Object v = a.validator();
          if (Starlark.truth(v)) { // attrs: `if a.validator`, so an empty list is no validator
            Validators.run(thread, v, self, a, instanceValue(self, a.name()));
          }
        }
      }

      Object postInit = self.typeClass().lookup("__attrs_post_init__");
      if (postInit != null) {
        AttrsSupport.call(thread, self.getField("__attrs_post_init__", thread));
      }
      return Starlark.NONE;
    }

    private void checkMissing(List<Attribute> params, Map<String, Object> values, String kind)
        throws EvalException {
      List<String> missing = new ArrayList<>();
      for (Attribute a : params) {
        if (!hasDefault(a) && !values.containsKey(a.alias())) {
          missing.add(a.alias());
        }
      }
      if (!missing.isEmpty()) {
        throw Starlark.errorf("TypeError: %s() missing %d required %s argument%s: %s",
            qualname, missing.size(), kind, missing.size() == 1 ? "" : "s",
            AttrsSupport.joinNames(missing));
      }
    }
  }

  // __repr__ / __str__

  static final class Repr extends Method {
    private final List<Attribute> attrs;

    Repr(String className, String name, List<Attribute> attrs) {
      super(className + "." + name);
      this.attrs = attrs;
    }

    @Override
    public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs)
        throws EvalException, InterruptedException {
      PyObject self = self(args, qualname);
      Set<Object> active = AttrsSupport.reprGuard(thread);
      if (active.contains(self)) {
        return "...";
      }
      active.add(self);
      try {
        StringBuilder sb = new StringBuilder(self.typeName()).append('(');
        String sep = "";
        for (Attribute a : attrs) {
          Object flag = a.reprFlag();
          if (Boolean.FALSE.equals(flag)) {
            continue;
          }
          Object value = fieldValue(self, a.name(), thread);
          if (value == null) {
            value = Nothing.NOTHING;
          }
          String rendered = Boolean.TRUE.equals(flag)
              ? AttrsSupport.repr(value)
              : AttrsSupport.str(AttrsSupport.call(thread, flag, value));
          sb.append(sep).append(a.name()).append('=').append(rendered);
          sep = ", ";
        }
        return sb.append(')').toString();
      } finally {
        active.remove(self);
      }
    }
  }

  // __eq__ / __ne__

  static final class Eq extends Method {
    private final List<Attribute> attrs;
    private final boolean negate;

    Eq(String className, List<Attribute> attrs, boolean negate) {
      super(className + (negate ? ".__ne__" : ".__eq__"));
      this.attrs = attrs;
      this.negate = negate;
    }

    @Override
    public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs)
        throws EvalException, InterruptedException {
      PyObject self = self(args, qualname);
      if (args.size() != 2) {
        throw Starlark.errorf("TypeError: %s() takes 2 positional arguments", qualname);
      }
      return negate != equal(thread, self, args.get(1));
    }

    private boolean equal(StarlarkThread thread, PyObject self, Object other)
        throws EvalException, InterruptedException {
      // attrs returns NotImplemented for another class; Python then compares identity.
      if (!(other instanceof PyObject) || ((PyObject) other).typeClass() != self.typeClass()) {
        return self == other;
      }
      for (Attribute a : attrs) {
        if (!a.eq()) {
          continue;
        }
        Object x = fieldValue(self, a.name(), thread);
        Object y = fieldValue((PyObject) other, a.name(), thread);
        Object key = a.eqKey();
        if (!AttrsSupport.isNone(key)) {
          x = AttrsSupport.call(thread, key, x);
          y = AttrsSupport.call(thread, key, y);
        }
        if (!Starlark.truth(AttrsSupport.binaryOp(TokenKind.EQUALS_EQUALS, x, y, thread))) {
          return false;
        }
      }
      return true;
    }
  }

  // __lt__, __le__, __gt__, __ge__

  static final class Order extends Method {
    private final List<Attribute> attrs;
    private final TokenKind op;
    private final String symbol;

    Order(String className, String name, TokenKind op, String symbol, List<Attribute> attrs) {
      super(className + "." + name);
      this.attrs = attrs;
      this.op = op;
      this.symbol = symbol;
    }

    private Tuple key(StarlarkThread thread, PyObject obj) throws EvalException, InterruptedException {
      List<Object> values = new ArrayList<>();
      for (Attribute a : attrs) {
        if (!a.order()) {
          continue;
        }
        Object v = fieldValue(obj, a.name(), thread);
        Object key = a.orderKey();
        values.add(AttrsSupport.isNone(key) ? v : AttrsSupport.call(thread, key, v));
      }
      return Tuple.copyOf(values);
    }

    @Override
    public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs)
        throws EvalException, InterruptedException {
      PyObject self = self(args, qualname);
      Object other = args.size() == 2 ? args.get(1) : null;
      if (!(other instanceof PyObject) || ((PyObject) other).typeClass() != self.typeClass()) {
        throw Starlark.errorf("TypeError: '%s' not supported between instances of '%s' and '%s'",
            symbol, self.typeName(), AttrsSupport.pythonTypeName(other));
      }
      return AttrsSupport.binaryOp(op, key(thread, self), key(thread, (PyObject) other), thread);
    }
  }

  // Field descriptors: frozen classes and on_setattr hooks

  /**
   * A data descriptor for one field of a frozen class or of a field with an on_setattr hook,
   * standing in for the {@code __setattr__} that attrs writes. The value lives in the instance
   * dictionary. Reading it from the class returns the descriptor, like a slotted class's member.
   */
  @StarlarkBuiltin(name = "member_descriptor", documented = false)
  static final class Field implements LarkyDataDescriptor, StarlarkValue {
    private final Attribute attr;
    private final String className;
    private final boolean frozen;
    @Nullable private final Object hook;

    Field(Attribute attr, String className, boolean frozen, @Nullable Object hook) {
      this.attr = attr;
      this.className = className;
      this.frozen = frozen;
      this.hook = hook;
    }

    @Override
    public Object __get__(Object obj, LarkyType type, @Nullable StarlarkThread thread) throws EvalException {
      if (obj == null || obj == Starlark.NONE) {
        return this;
      }
      Object value = instanceValue((PyObject) obj, attr.name());
      if (value == null) {
        // Unchecked, so that the attribute lookup does not report it as a broken descriptor.
        throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(
            String.format("AttributeError: '%s' object has no attribute '%s'",
                ((PyObject) obj).typeName(), attr.name()),
            new EvalException("AttributeError"), thread);
      }
      return value;
    }

    @Override
    public void __set__(Object obj, Object value, @Nullable StarlarkThread thread)
        throws EvalException, InterruptedException {
      if (frozen) {
        throw AttrsSupport.error("FrozenInstanceError", "can't set attribute");
      }
      PyObject self = (PyObject) obj;
      StarlarkThread t = thread != null ? thread : self.getCurrentThread();
      Object nval = hook instanceof Setters.Setter s
          ? s.apply(t, self, attr, value)
          : AttrsSupport.call(t, hook, self, attr, value);
      AttrsSupport.storeField(self, attr.name(), nval);
    }

    @Override
    public void __delete__(Object obj, @Nullable StarlarkThread thread) throws EvalException {
      if (frozen) {
        throw AttrsSupport.error("FrozenInstanceError", "can't set attribute");
      }
      PyObject self = (PyObject) obj;
      ((Dict<?, ?>) self.getInternalDictUnsafe())
          .pop(attr.name(), Starlark.NONE, thread != null ? thread : self.getCurrentThread());
    }

    @Override
    public boolean readonly() {
      return false;
    }

    @Override
    public boolean optional() {
      return true;
    }

    @Override
    public void repr(Printer printer, StarlarkSemantics semantics) {
      printer.append("<member '").append(attr.name()).append("' of '").append(className).append("' objects>");
    }

    @Override
    public boolean isImmutable() {
      return true;
    }
  }
}
