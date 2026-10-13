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

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

/** The on_setattr hooks of {@code attrs.setters}: callables {@code (instance, attrib, new_value)}. */
final class Setters {

  private Setters() {}

  /** {@code setters.NO_OP}: disables class-wide on_setattr hooks for an attribute. */
  @StarlarkBuiltin(name = "object", documented = false)
  static final class NoOp implements StarlarkValue {
    static final NoOp INSTANCE = new NoOp();

    @Override
    public void repr(Printer printer, StarlarkSemantics semantics) {
      printer.append("<object object>");
    }

    @Override
    public boolean isImmutable() {
      return true;
    }
  }

  abstract static class Setter implements StarlarkCallable {
    private final String name;

    Setter(String name) {
      this.name = name;
    }

    abstract Object apply(StarlarkThread thread, Object instance, Object attrib, Object newValue)
        throws EvalException, InterruptedException;

    @Override
    public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs)
        throws EvalException, InterruptedException {
      if (args.size() != 3 || !kwargs.isEmpty()) {
        throw Starlark.errorf("TypeError: %s() takes 3 positional arguments (instance, attrib, new_value)", name);
      }
      return apply(thread, args.get(0), args.get(1), args.get(2));
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void repr(Printer printer, StarlarkSemantics semantics) {
      printer.append("<function ").append(name).append(">");
    }

    @Override
    public boolean isImmutable() {
      return true;
    }
  }

  static final Setter FROZEN = new Setter("frozen") {
    @Override
    Object apply(StarlarkThread thread, Object instance, Object attrib, Object newValue) throws EvalException {
      throw AttrsSupport.error("FrozenAttributeError", "can't set attribute");
    }
  };

  static final Setter VALIDATE = new Setter("validate") {
    @Override
    Object apply(StarlarkThread thread, Object instance, Object attrib, Object newValue)
        throws EvalException, InterruptedException {
      if (!AttrsSupport.runValidators(thread)) {
        return newValue;
      }
      Object v = AttrsSupport.getattr(thread, attrib, "validator");
      if (!Starlark.truth(v)) {
        return newValue;
      }
      Validators.run(thread, v, instance, attrib, newValue);
      return newValue;
    }
  };

  static final Setter CONVERT = new Setter("convert") {
    @Override
    Object apply(StarlarkThread thread, Object instance, Object attrib, Object newValue)
        throws EvalException, InterruptedException {
      Object c = AttrsSupport.getattr(thread, attrib, "converter");
      return Starlark.truth(c) ? AttrsSupport.call(thread, c, newValue) : newValue;
    }
  };

  /** {@code setters.pipe(*setters)}: runs the setters in turn, returning the last result. */
  static final class Pipe extends Setter {
    final Tuple setters;

    Pipe(Tuple setters) {
      super("wrapped_pipe");
      this.setters = setters;
    }

    @Override
    Object apply(StarlarkThread thread, Object instance, Object attrib, Object newValue)
        throws EvalException, InterruptedException {
      Object rv = newValue;
      for (Object setter : setters) {
        rv = setter instanceof Setter s
            ? s.apply(thread, instance, attrib, rv)
            : AttrsSupport.call(thread, setter, instance, attrib, rv);
      }
      return rv;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Pipe p && p.setters.equals(setters);
    }

    @Override
    public int hashCode() {
      return setters.hashCode();
    }
  }

  /** attrs' {@code _DEFAULT_ON_SETATTR}, used by {@code define} for mutable classes. */
  static final Pipe DEFAULT_ON_SETATTR = new Pipe(Tuple.of(CONVERT, VALIDATE));
}
