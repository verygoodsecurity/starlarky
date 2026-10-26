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

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.verygood.security.larky.objects.PyObject;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Structure;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.TokenKind;

/** The validators of {@code attrs.validators}, each a callable {@code (inst, attr, value)}. */
final class Validators {

  private Validators() {}

  /** A validator: a callable with the fields that attrs exposes on it, equal by those fields. */
  abstract static class Validator implements StarlarkCallable, Structure {
    final Map<String, Object> fields = new LinkedHashMap<>();
    private final String className;

    Validator(String className) {
      this.className = className;
    }

    abstract void validate(StarlarkThread thread, Object inst, Object attr, Object value)
        throws EvalException, InterruptedException;

    abstract String reprString();

    @Override
    public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs)
        throws EvalException, InterruptedException {
      if (args.size() != 3 || !kwargs.isEmpty()) {
        throw Starlark.errorf(
            "TypeError: %s.__call__() takes 4 positional arguments (self, inst, attr, value)", className);
      }
      validate(thread, args.get(0), args.get(1), args.get(2));
      return Starlark.NONE;
    }

    @Override
    public String getName() {
      return className;
    }

    @Nullable
    @Override
    public Object getValue(String name) {
      return fields.get(name);
    }

    @Override
    public ImmutableList<String> getFieldNames() {
      return ImmutableList.copyOf(fields.keySet());
    }

    @Nullable
    @Override
    public String getErrorMessageForUnknownField(String field) {
      return "'" + className + "' object has no attribute '" + field + "'";
    }

    @Override
    public void setField(String field, Object value) throws EvalException {
      throw AttrsSupport.error("FrozenInstanceError", "can't set attribute");
    }

    @Override
    public void repr(Printer printer, StarlarkSemantics semantics) {
      printer.append(reprString());
    }

    @Override
    public boolean equals(Object o) {
      return o != null && o.getClass() == getClass() && fields.equals(((Validator) o).fields);
    }

    @Override
    public int hashCode() {
      return Objects.hash(getClass(), fields.keySet());
    }

    @Override
    public boolean isImmutable() {
      for (Object v : fields.values()) {
        if (!Starlark.isImmutable(v)) {
          return false;
        }
      }
      return true;
    }
  }

  static String reprOf(Object x) {
    return AttrsSupport.repr(x);
  }

  static String attrName(StarlarkThread thread, Object attr) throws EvalException, InterruptedException {
    return AttrsSupport.str(AttrsSupport.getattr(thread, attr, "name"));
  }

  /** Runs {@code validator}, which may be one of ours or any callable. */
  static void run(StarlarkThread thread, Object validator, Object inst, Object attr, Object value)
      throws EvalException, InterruptedException {
    if (validator instanceof Validator) {
      ((Validator) validator).validate(thread, inst, attr, value);
    } else {
      AttrsSupport.call(thread, validator, inst, attr, value);
    }
  }

  private static String typeRepr(Object t) {
    return AttrsSupport.typeRepr(t);
  }

  private static String classRepr(Object value) {
    return AttrsSupport.classRepr(value);
  }

  // instance_of

  static final class InstanceOf extends Validator {
    InstanceOf(Object type) {
      super("_InstanceOfValidator");
      fields.put("type", type);
    }

    @Override
    void validate(StarlarkThread thread, Object inst, Object attr, Object value)
        throws EvalException, InterruptedException {
      Object type = fields.get("type");
      if (!AttrsSupport.isInstance(value, type)) {
        throw AttrsSupport.error("TypeError", String.format(
            "'%s' must be %s (got %s that is a %s).",
            attrName(thread, attr), typeRepr(type), reprOf(value), classRepr(value)));
      }
    }

    @Override
    String reprString() {
      return "<instance_of validator for type " + typeRepr(fields.get("type")) + ">";
    }
  }

  // matches_re

  static final class MatchesRe extends Validator {
    MatchesRe(Object pattern, Object matchFunc) {
      super("_MatchesReValidator");
      fields.put("pattern", pattern);
      fields.put("match_func", matchFunc);
    }

    @Override
    void validate(StarlarkThread thread, Object inst, Object attr, Object value)
        throws EvalException, InterruptedException {
      if (!Starlark.truth(AttrsSupport.call(thread, fields.get("match_func"), value))) {
        Object pattern = AttrsSupport.getattr(thread, fields.get("pattern"), "pattern");
        throw AttrsSupport.error("ValueError", String.format(
            "'%s' must match regex %s (%s doesn't)", attrName(thread, attr), reprOf(pattern), reprOf(value)));
      }
    }

    @Override
    String reprString() {
      return "<matches_re validator for pattern " + reprOf(fields.get("pattern")) + ">";
    }
  }

  // optional

  static final class Optional extends Validator {
    Optional(Object validator) {
      super("_OptionalValidator");
      fields.put("validator", validator);
    }

    @Override
    void validate(StarlarkThread thread, Object inst, Object attr, Object value)
        throws EvalException, InterruptedException {
      if (AttrsSupport.isNone(value)) {
        return;
      }
      run(thread, fields.get("validator"), inst, attr, value);
    }

    @Override
    String reprString() {
      return "<optional validator for " + reprOf(fields.get("validator")) + " or None>";
    }
  }

  // in_

  static final class In extends Validator {
    In(Object options, Object originalOptions) {
      super("_InValidator");
      fields.put("options", options);
      fields.put("_original_options", originalOptions);
    }

    @Override
    void validate(StarlarkThread thread, Object inst, Object attr, Object value)
        throws EvalException, InterruptedException {
      boolean in;
      int stackSize = StarlarkEvalWrapper.callStackSize(thread);
      try {
        in = Starlark.truth(AttrsSupport.binaryOp(TokenKind.IN, value, fields.get("options"), thread));
      } catch (EvalException e) { // e.g. 1 in "abc", as attrs catches TypeError
        StarlarkEvalWrapper.unwindCallStack(thread, stackSize);
        in = false;
      }
      if (!in) {
        throw AttrsSupport.error("ValueError", String.format(
            "'%s' must be in %s (got %s)",
            attrName(thread, attr), reprOf(fields.get("_original_options")), reprOf(value)));
      }
    }

    @Override
    String reprString() {
      return "<in_ validator with options " + reprOf(fields.get("_original_options")) + ">";
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof In && Objects.equals(((In) o).fields.get("options"), fields.get("options"));
    }
  }

  // is_callable

  static final class IsCallable extends Validator {
    IsCallable() {
      super("_IsCallableValidator");
    }

    @Override
    void validate(StarlarkThread thread, Object inst, Object attr, Object value)
        throws EvalException, InterruptedException {
      if (!AttrsSupport.isCallable(value)) {
        throw AttrsSupport.error("NotCallableError", String.format(
            "'%s' must be callable (got %s that is a %s).",
            attrName(thread, attr), reprOf(value), classRepr(value)));
      }
    }

    @Override
    String reprString() {
      return "<is_callable validator>";
    }
  }

  /** Checks a validator's own fields as attrs' validator classes do in their __init__. */
  private static void checkCallable(String field, Object value, boolean optional) throws EvalException {
    if (optional && AttrsSupport.isNone(value)) {
      return;
    }
    if (!AttrsSupport.isCallable(value)) {
      throw AttrsSupport.error("NotCallableError", String.format(
          "'%s' must be callable (got %s that is a %s).", field, reprOf(value), classRepr(value)));
    }
  }

  // deep_iterable

  static final class DeepIterable extends Validator {
    DeepIterable(Object memberValidator, Object iterableValidator) throws EvalException {
      super("_DeepIterable");
      checkCallable("member_validator", memberValidator, false);
      checkCallable("iterable_validator", iterableValidator, true);
      fields.put("member_validator", memberValidator);
      fields.put("iterable_validator", iterableValidator);
    }

    @Override
    void validate(StarlarkThread thread, Object inst, Object attr, Object value)
        throws EvalException, InterruptedException {
      Object iterableValidator = fields.get("iterable_validator");
      if (!AttrsSupport.isNone(iterableValidator)) {
        run(thread, iterableValidator, inst, attr, value);
      }
      for (Object member : Starlark.toIterable(value)) {
        run(thread, fields.get("member_validator"), inst, attr, member);
      }
    }

    @Override
    String reprString() {
      Object iterableValidator = fields.get("iterable_validator");
      String iterableIdentifier = AttrsSupport.isNone(iterableValidator) ? "" : " " + reprOf(iterableValidator);
      return "<deep_iterable validator for" + iterableIdentifier
          + " iterables of " + reprOf(fields.get("member_validator")) + ">";
    }
  }

  // deep_mapping

  static final class DeepMapping extends Validator {
    DeepMapping(Object keyValidator, Object valueValidator, Object mappingValidator) throws EvalException {
      super("_DeepMapping");
      checkCallable("key_validator", keyValidator, true);
      checkCallable("value_validator", valueValidator, true);
      checkCallable("mapping_validator", mappingValidator, true);
      fields.put("key_validator", keyValidator);
      fields.put("value_validator", valueValidator);
      fields.put("mapping_validator", mappingValidator);
    }

    @Override
    void validate(StarlarkThread thread, Object inst, Object attr, Object value)
        throws EvalException, InterruptedException {
      Object mappingValidator = fields.get("mapping_validator");
      Object keyValidator = fields.get("key_validator");
      Object valueValidator = fields.get("value_validator");
      if (!AttrsSupport.isNone(mappingValidator)) {
        run(thread, mappingValidator, inst, attr, value);
      }
      for (Object key : Starlark.toIterable(value)) {
        if (!AttrsSupport.isNone(keyValidator)) {
          run(thread, keyValidator, inst, attr, key);
        }
        if (!AttrsSupport.isNone(valueValidator)) {
          run(thread, valueValidator, inst, attr, StarlarkEvalWrapper.index(thread, value, key));
        }
      }
    }

    @Override
    String reprString() {
      return "<deep_mapping validator for objects mapping " + reprOf(fields.get("key_validator"))
          + " to " + reprOf(fields.get("value_validator")) + ">";
    }
  }

  // lt, le, ge, gt

  static final class Number extends Validator {
    private final TokenKind op;

    Number(Object bound, String compareOp) {
      super("_NumberValidator");
      fields.put("bound", bound);
      fields.put("compare_op", compareOp);
      this.op = switch (compareOp) {
        case "<" -> TokenKind.LESS;
        case "<=" -> TokenKind.LESS_EQUALS;
        case ">=" -> TokenKind.GREATER_EQUALS;
        case ">" -> TokenKind.GREATER;
        default -> throw new IllegalArgumentException(compareOp);
      };
    }

    @Override
    void validate(StarlarkThread thread, Object inst, Object attr, Object value)
        throws EvalException, InterruptedException {
      Object bound = fields.get("bound");
      if (!Starlark.truth(AttrsSupport.binaryOp(op, value, bound, thread))) {
        throw AttrsSupport.error("ValueError", String.format(
            "'%s' must be %s %s: %s", attrName(thread, attr), fields.get("compare_op"),
            AttrsSupport.str(bound), AttrsSupport.str(value)));
      }
    }

    @Override
    String reprString() {
      return "<Validator for x " + fields.get("compare_op") + " "
          + AttrsSupport.str(fields.get("bound")) + ">";
    }
  }

  /** {@code len(value)}, including objects that define {@code __len__}. */
  static int len(StarlarkThread thread, Object value) throws EvalException, InterruptedException {
    int n = Starlark.len(value);
    if (n >= 0) {
      return n;
    }
    if (value instanceof PyObject) {
      Object lenMethod = ((PyObject) value).getField("__len__", thread);
      if (lenMethod != null) {
        return Starlark.toInt(AttrsSupport.call(thread, lenMethod), "__len__");
      }
    }
    throw Starlark.errorf("TypeError: object of type '%s' has no len()", Starlark.type(value));
  }

  // max_len, min_len

  static final class Length extends Validator {
    private final boolean max;

    Length(Object length, boolean max) {
      super(max ? "_MaxLengthValidator" : "_MinLengthValidator");
      this.max = max;
      fields.put(max ? "max_length" : "min_length", length);
    }

    @Override
    void validate(StarlarkThread thread, Object inst, Object attr, Object value)
        throws EvalException, InterruptedException {
      Object limit = fields.get(max ? "max_length" : "min_length");
      StarlarkInt n = StarlarkInt.of(len(thread, value));
      boolean ok = Starlark.truth(AttrsSupport.binaryOp(
          max ? TokenKind.LESS_EQUALS : TokenKind.GREATER_EQUALS, n, limit, thread));
      if (!ok) {
        throw AttrsSupport.error("ValueError", String.format(
            "Length of '%s' must be %s %s: %s", attrName(thread, attr), max ? "<=" : ">=",
            AttrsSupport.str(limit), n));
      }
    }

    @Override
    String reprString() {
      return "<" + (max ? "max_len" : "min_len") + " validator for "
          + AttrsSupport.str(fields.get(max ? "max_length" : "min_length")) + ">";
    }
  }

  // and_

  static final class And extends Validator {
    And(Object validators) {
      super("_AndValidator");
      fields.put("_validators", validators);
    }

    @Override
    void validate(StarlarkThread thread, Object inst, Object attr, Object value)
        throws EvalException, InterruptedException {
      for (Object v : Starlark.toIterable(fields.get("_validators"))) {
        run(thread, v, inst, attr, value);
      }
    }

    @Override
    String reprString() {
      return "_AndValidator(_validators=" + reprOf(fields.get("_validators")) + ")";
    }
  }

  /** attrs' {@code and_}: flattens nested {@code and_} validators. */
  static And and(Iterable<?> validators) throws EvalException {
    List<Object> vals = new ArrayList<>();
    for (Object v : validators) {
      if (v instanceof And) {
        for (Object inner : Starlark.toIterable(((And) v).fields.get("_validators"))) {
          vals.add(inner);
        }
      } else {
        vals.add(v);
      }
    }
    return new And(Tuple.copyOf(vals));
  }

  // or_

  static final class Or extends Validator {
    Or(Tuple validators) {
      super("_OrValidator");
      fields.put("validators", validators);
    }

    @Override
    void validate(StarlarkThread thread, Object inst, Object attr, Object value)
        throws EvalException, InterruptedException {
      Tuple validators = (Tuple) fields.get("validators");
      for (Object v : validators) {
        int stackSize = StarlarkEvalWrapper.callStackSize(thread);
        try {
          run(thread, v, inst, attr, value);
          return;
        } catch (EvalException e) {
          // attrs catches any Exception here; InterruptedException still stops the evaluation.
          StarlarkEvalWrapper.unwindCallStack(thread, stackSize);
        }
      }
      throw AttrsSupport.error("ValueError", String.format(
          "None of %s satisfied for value %s", reprOf(validators), reprOf(value)));
    }

    @Override
    String reprString() {
      return "<or validator wrapping " + reprOf(fields.get("validators")) + ">";
    }
  }

  /** attrs' {@code or_}: flattens nested {@code or_} validators. */
  static Or or(Iterable<?> validators) throws EvalException {
    List<Object> vals = new ArrayList<>();
    for (Object v : validators) {
      if (v instanceof Or) {
        vals.addAll((Tuple) ((Or) v).fields.get("validators"));
      } else {
        vals.add(v);
      }
    }
    return new Or(Tuple.copyOf(vals));
  }
}
