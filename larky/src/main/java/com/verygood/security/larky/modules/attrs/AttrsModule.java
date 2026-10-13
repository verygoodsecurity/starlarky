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

import com.verygood.security.larky.objects.LarkyClassMethod;
import com.verygood.security.larky.objects.LarkyFunction;
import com.verygood.security.larky.objects.LarkyMethod;
import com.verygood.security.larky.objects.LarkyStaticMethod;
import com.verygood.security.larky.objects.type.LarkyType;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.TokenKind;

/**
 * The native part of {@code @vendor//attr} and {@code @vendor//attrs}, the Larky port of the
 * attrs package. attrs writes each class's methods as Python source and exec()s it; this module
 * provides those methods, the validators and the instance functions as Java callables instead.
 * Being Java, they can nest (an attrs instance inside another, a validator inside another of the
 * same kind) without tripping Starlark's ban on recursive calls.
 */
@StarlarkBuiltin(
    name = "jattrs",
    category = "BUILTIN",
    doc = "Native support for the Larky port of attrs (@vendor//attr). Use @vendor//attr instead.")
public final class AttrsModule implements StarlarkValue {

  public static final AttrsModule INSTANCE = new AttrsModule();

  @StarlarkMethod(name = "NOTHING", structField = true)
  public Object nothing() {
    return Nothing.NOTHING;
  }

  @StarlarkMethod(
      name = "Factory",
      parameters = {@Param(name = "factory", named = true), @Param(name = "takes_self", named = true, defaultValue = "False")})
  public Factory factory(Object factory, Object takesSelf) {
    return new Factory(factory, Starlark.truth(takesSelf));
  }

  @StarlarkMethod(name = "is_factory", parameters = {@Param(name = "x", named = true)})
  public boolean isFactory(Object x) {
    return x instanceof Factory;
  }

  @StarlarkMethod(
      name = "Attribute",
      parameters = {
        @Param(name = "name", named = true, allowedTypes = {@ParamType(type = String.class)}),
        @Param(name = "default", named = true),
        @Param(name = "validator", named = true),
        @Param(name = "repr", named = true),
        @Param(name = "cmp", named = true),
        @Param(name = "hash", named = true),
        @Param(name = "init", named = true),
        @Param(name = "inherited", named = true),
        @Param(name = "metadata", named = true, defaultValue = "None"),
        @Param(name = "type", named = true, defaultValue = "None"),
        @Param(name = "converter", named = true, defaultValue = "None"),
        @Param(name = "kw_only", named = true, defaultValue = "False"),
        @Param(name = "eq", named = true, defaultValue = "None"),
        @Param(name = "eq_key", named = true, defaultValue = "None"),
        @Param(name = "order", named = true, defaultValue = "None"),
        @Param(name = "order_key", named = true, defaultValue = "None"),
        @Param(name = "on_setattr", named = true, defaultValue = "None"),
        @Param(name = "alias", named = true, defaultValue = "None"),
        @Param(name = "alias_is_default", named = true, defaultValue = "None"),
      })
  public Attribute attribute(
      String name, Object dflt, Object validator, Object repr, Object cmp, Object hash, Object init,
      Object inherited, Object metadata, Object type, Object converter, Object kwOnly, Object eq,
      Object eqKey, Object order, Object orderKey, Object onSetattr, Object alias,
      Object aliasIsDefault) throws EvalException {
    return Attribute.create(name, dflt, validator, repr, cmp, hash, init, inherited, metadata, type,
        converter, kwOnly, eq, eqKey, order, orderKey, onSetattr, alias, aliasIsDefault);
  }

  @StarlarkMethod(name = "is_attribute", parameters = {@Param(name = "x", named = true)})
  public boolean isAttribute(Object x) {
    return x instanceof Attribute;
  }

  @StarlarkMethod(
      name = "determine_attrib_eq_order",
      parameters = {
        @Param(name = "cmp", named = true), @Param(name = "eq", named = true), @Param(name = "order", named = true), @Param(name = "default_eq", named = true)
      })
  public Tuple determineAttribEqOrder(Object cmp, Object eq, Object order, Object defaultEq)
      throws EvalException {
    return Attribute.determineAttribEqOrder(cmp, eq, order, Starlark.truth(defaultEq));
  }

  @StarlarkMethod(name = "default_init_alias_for", parameters = {@Param(name = "name", named = true)})
  public String defaultInitAliasFor(String name) {
    return Attribute.defaultInitAliasFor(name);
  }

  // Validators

  @StarlarkMethod(name = "instance_of", parameters = {@Param(name = "type", named = true)})
  public Object instanceOf(Object type) {
    return new Validators.InstanceOf(type);
  }

  @StarlarkMethod(
      name = "matches_re",
      parameters = {@Param(name = "pattern", named = true), @Param(name = "match_func", named = true)})
  public Object matchesRe(Object pattern, Object matchFunc) {
    return new Validators.MatchesRe(pattern, matchFunc);
  }

  @StarlarkMethod(name = "optional", parameters = {@Param(name = "validator", named = true)})
  public Object optional(Object validator) throws EvalException {
    if (validator instanceof Sequence) {
      return new Validators.Optional(new Validators.And(validator));
    }
    return new Validators.Optional(validator);
  }

  @StarlarkMethod(
      name = "in_",
      parameters = {@Param(name = "options", named = true), @Param(name = "original_options", named = true)})
  public Object in(Object options, Object originalOptions) {
    return new Validators.In(options, originalOptions);
  }

  @StarlarkMethod(name = "is_callable")
  public Object isCallable() {
    return new Validators.IsCallable();
  }

  @StarlarkMethod(
      name = "deep_iterable",
      parameters = {@Param(name = "member_validator", named = true), @Param(name = "iterable_validator", named = true)})
  public Object deepIterable(Object member, Object iterable) throws EvalException {
    return new Validators.DeepIterable(member, iterable);
  }

  @StarlarkMethod(
      name = "deep_mapping",
      parameters = {
        @Param(name = "key_validator", named = true),
        @Param(name = "value_validator", named = true),
        @Param(name = "mapping_validator", named = true)
      })
  public Object deepMapping(Object key, Object value, Object mapping) throws EvalException {
    return new Validators.DeepMapping(key, value, mapping);
  }

  @StarlarkMethod(
      name = "number",
      parameters = {
        @Param(name = "bound", named = true),
        @Param(name = "compare_op", named = true, allowedTypes = {@ParamType(type = String.class)})
      })
  public Object number(Object bound, String compareOp) {
    return new Validators.Number(bound, compareOp);
  }

  @StarlarkMethod(name = "max_len", parameters = {@Param(name = "length", named = true)})
  public Object maxLen(Object length) {
    return new Validators.Length(length, true);
  }

  @StarlarkMethod(name = "min_len", parameters = {@Param(name = "length", named = true)})
  public Object minLen(Object length) {
    return new Validators.Length(length, false);
  }

  @StarlarkMethod(name = "and_", extraPositionals = @Param(name = "validators"))
  public Object and(Tuple validators) throws EvalException {
    return Validators.and(validators);
  }

  @StarlarkMethod(name = "or_", extraPositionals = @Param(name = "validators"))
  public Object or(Tuple validators) throws EvalException {
    return Validators.or(validators);
  }

  @StarlarkMethod(
      name = "set_run_validators",
      parameters = {@Param(name = "run", named = true)},
      useStarlarkThread = true)
  public NoneType setRunValidators(Object run, StarlarkThread thread) throws EvalException {
    if (!(run instanceof Boolean)) {
      throw AttrsSupport.error("TypeError", "'run' must be bool.");
    }
    AttrsSupport.setRunValidators(thread, (Boolean) run);
    return Starlark.NONE;
  }

  @StarlarkMethod(name = "get_run_validators", useStarlarkThread = true)
  public boolean getRunValidators(StarlarkThread thread) {
    return AttrsSupport.runValidators(thread);
  }

  // Setters

  @StarlarkMethod(name = "setters_frozen", structField = true)
  public Object settersFrozen() {
    return Setters.FROZEN;
  }

  @StarlarkMethod(name = "setters_validate", structField = true)
  public Object settersValidate() {
    return Setters.VALIDATE;
  }

  @StarlarkMethod(name = "setters_convert", structField = true)
  public Object settersConvert() {
    return Setters.CONVERT;
  }

  @StarlarkMethod(name = "setters_NO_OP", structField = true)
  public Object settersNoOp() {
    return Setters.NoOp.INSTANCE;
  }

  @StarlarkMethod(name = "DEFAULT_ON_SETATTR", structField = true)
  public Object defaultOnSetattr() {
    return Setters.DEFAULT_ON_SETATTR;
  }

  @StarlarkMethod(name = "setters_pipe", extraPositionals = @Param(name = "setters"))
  public Object settersPipe(Tuple setters) {
    return new Setters.Pipe(setters);
  }

  // Classes

  @StarlarkMethod(name = "make_fields", parameters = {@Param(name = "attrs", named = true)})
  public AttrsFields makeFields(Sequence<?> attrs) throws EvalException {
    return new AttrsFields(Sequence.cast(attrs, Attribute.class, "attrs"));
  }

  @StarlarkMethod(
      name = "class_namespace",
      doc = "The generated methods and field descriptors of an attrs class.",
      parameters = {
        @Param(name = "class_name", named = true, allowedTypes = {@ParamType(type = String.class)}),
        @Param(name = "attrs", named = true),
        @Param(name = "init", named = true),
        @Param(name = "repr", named = true),
        @Param(name = "eq", named = true),
        @Param(name = "order", named = true),
        @Param(name = "str", named = true),
        @Param(name = "frozen", named = true),
        @Param(name = "on_setattr", named = true),
      },
      useStarlarkThread = true)
  public Dict<String, Object> classNamespace(
      String className, AttrsFields fields, Object init, Object repr, Object eq, Object order,
      Object str, Object frozen, Object onSetattr, StarlarkThread thread) throws EvalException {
    List<Attribute> attrs = new ArrayList<>(fields.attributes());
    boolean isFrozen = Starlark.truth(frozen);
    Object clsHook = AttrsSupport.isNone(onSetattr) || onSetattr == Setters.NoOp.INSTANCE ? null : onSetattr;
    if (isFrozen && clsHook != null) {
      throw AttrsSupport.error("ValueError", "Frozen classes can't use on_setattr.");
    }
    Dict<String, Object> ns = Dict.of(thread.mutability());
    ns.putEntry("__attrs_attrs__", fields);
    if (init instanceof String) {
      // "__init__", or "__attrs_init__" when the class keeps its own __init__.
      ns.putEntry((String) init, method(new ClassMethods.Init(className, attrs), thread));
    }
    if (Starlark.truth(repr)) {
      ns.putEntry("__repr__", method(new ClassMethods.Repr(className, "__repr__", attrs), thread));
    }
    if (Starlark.truth(str)) {
      ns.putEntry("__str__", method(new ClassMethods.Repr(className, "__str__", attrs), thread));
    }
    if (Starlark.truth(eq)) {
      ns.putEntry("__eq__", method(new ClassMethods.Eq(className, attrs, false), thread));
      ns.putEntry("__ne__", method(new ClassMethods.Eq(className, attrs, true), thread));
    }
    if (Starlark.truth(order)) {
      ns.putEntry("__lt__", method(new ClassMethods.Order(className, "__lt__", TokenKind.LESS, "<", attrs), thread));
      ns.putEntry("__le__", method(new ClassMethods.Order(className, "__le__", TokenKind.LESS_EQUALS, "<=", attrs), thread));
      ns.putEntry("__gt__", method(new ClassMethods.Order(className, "__gt__", TokenKind.GREATER, ">", attrs), thread));
      ns.putEntry("__ge__", method(new ClassMethods.Order(className, "__ge__", TokenKind.GREATER_EQUALS, ">=", attrs), thread));
    }
    for (Attribute a : attrs) {
      Object hook = a.onSetattr();
      if (isFrozen) {
        if (!AttrsSupport.isNone(hook) && hook != Setters.NoOp.INSTANCE) {
          throw AttrsSupport.error("ValueError", "Frozen classes can't use on_setattr.");
        }
        ns.putEntry(a.name(), new ClassMethods.Field(a, className, true, null));
        continue;
      }
      if (AttrsSupport.isNone(hook)) {
        hook = clsHook;
      }
      if (hook != null && hook != Setters.NoOp.INSTANCE) {
        ns.putEntry(a.name(), new ClassMethods.Field(a, className, false, hook));
      }
    }
    return ns;
  }

  @StarlarkMethod(
      name = "class_body",
      doc = "The own namespace of class cls, with its methods unbound so a new class can take them.",
      parameters = {@Param(name = "cls", named = true)},
      useStarlarkThread = true)
  public Dict<String, Object> classBody(Object cls, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (!(cls instanceof LarkyType)) {
      throw Starlark.errorf("TypeError: %s is not a class", Starlark.type(cls));
    }
    Dict<String, Object> body = Dict.of(thread.mutability());
    for (Map.Entry<String, Object> e : ((LarkyType) cls).getInternalDictUnsafe().entrySet()) {
      Object v = e.getValue();
      if (v instanceof LarkyClassMethod.LarkyClassMethodInstance cm) {
        v = Starlark.call(thread, LarkyClassMethod.getInstance(), Tuple.of(cm.wrapped()), Dict.empty());
      } else if (v instanceof LarkyStaticMethod.LarkyStaticMethodInstance sm) {
        v = Starlark.call(thread, LarkyStaticMethod.getInstance(), Tuple.of(sm.wrapped()), Dict.empty());
      } else if (v instanceof LarkyFunction f && !(v instanceof LarkyMethod)) {
        v = f.getFunction();
      }
      body.putEntry(e.getKey(), v);
    }
    return body;
  }

  @StarlarkMethod(
      name = "next_counter",
      doc = "The next value of the counter that orders attr.ib()s by creation.",
      useStarlarkThread = true)
  public StarlarkInt nextCounter(StarlarkThread thread) {
    return StarlarkInt.of(AttrsSupport.nextCounter(thread));
  }

  @StarlarkMethod(name = "callable", parameters = {@Param(name = "x", named = true)})
  public boolean callable(Object x) throws EvalException {
    return AttrsSupport.isCallable(x);
  }

  private static Object method(StarlarkCallable m, StarlarkThread thread) {
    return LarkyFunction.create(m, thread);
  }

  @StarlarkMethod(name = "fields", parameters = {@Param(name = "cls", named = true)}, useStarlarkThread = true)
  public Object fields(Object cls, StarlarkThread thread) throws EvalException, InterruptedException {
    return AttrsFunctions.fields(thread, cls);
  }

  @StarlarkMethod(name = "fields_dict", parameters = {@Param(name = "cls", named = true)}, useStarlarkThread = true)
  public Object fieldsDict(Object cls, StarlarkThread thread) throws EvalException, InterruptedException {
    return AttrsFunctions.fieldsDict(thread, cls);
  }

  @StarlarkMethod(name = "has", parameters = {@Param(name = "cls", named = true)}, useStarlarkThread = true)
  public boolean has(Object cls, StarlarkThread thread) throws EvalException, InterruptedException {
    return AttrsSupport.attrsOf(thread, cls) != null;
  }

  @StarlarkMethod(name = "is_class", parameters = {@Param(name = "x", named = true)})
  public boolean isClass(Object x) {
    return AttrsSupport.isClass(x);
  }

  @StarlarkMethod(name = "validate", parameters = {@Param(name = "inst", named = true)}, useStarlarkThread = true)
  public NoneType validate(Object inst, StarlarkThread thread) throws EvalException, InterruptedException {
    AttrsFunctions.validate(thread, inst);
    return Starlark.NONE;
  }

  @StarlarkMethod(
      name = "evolve",
      extraPositionals = @Param(name = "args"),
      extraKeywords = @Param(name = "changes"),
      useStarlarkThread = true)
  public Object evolve(Tuple args, Dict<String, Object> changes, StarlarkThread thread)
      throws EvalException, InterruptedException {
    return AttrsFunctions.evolve(thread, args, changes);
  }

  @StarlarkMethod(
      name = "asdict",
      parameters = {
        @Param(name = "inst", named = true),
        @Param(name = "recurse", named = true),
        @Param(name = "filter", named = true),
        @Param(name = "dict_factory", named = true),
        @Param(name = "retain_collection_types", named = true),
        @Param(name = "value_serializer", named = true),
      },
      useStarlarkThread = true)
  public Object asdict(Object inst, Object recurse, Object filter, Object dictFactory,
      Object retain, Object valueSerializer, StarlarkThread thread)
      throws EvalException, InterruptedException {
    return new AttrsFunctions.AsDict(thread, filter, dictFactory, Starlark.truth(retain), valueSerializer)
        .asdict(inst, Starlark.truth(recurse));
  }

  @StarlarkMethod(
      name = "astuple",
      parameters = {
        @Param(name = "inst", named = true),
        @Param(name = "recurse", named = true),
        @Param(name = "filter", named = true),
        @Param(name = "tuple_factory", named = true),
        @Param(name = "retain_collection_types", named = true),
      },
      useStarlarkThread = true)
  public Object astuple(Object inst, Object recurse, Object filter, Object tupleFactory, Object retain,
      StarlarkThread thread) throws EvalException, InterruptedException {
    return new AttrsFunctions.AsTuple(thread, filter, tupleFactory, Starlark.truth(retain))
        .astuple(inst, Starlark.truth(recurse));
  }
}
