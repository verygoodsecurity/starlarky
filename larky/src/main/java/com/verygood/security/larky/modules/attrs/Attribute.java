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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Structure;
import net.starlark.java.eval.Tuple;

/**
 * attrs' {@code Attribute}: the read-only description of one field of an attrs class, as returned
 * by {@code fields()} and passed to validators.
 */
@StarlarkBuiltin(name = "Attribute", documented = false)
public final class Attribute implements Structure {

  /** The fields in attrs' order (Attribute.__slots__). */
  static final ImmutableList<String> FIELDS = ImmutableList.of(
      "name", "default", "validator", "repr", "eq", "eq_key", "order", "order_key", "hash",
      "init", "metadata", "type", "converter", "kw_only", "inherited", "on_setattr", "alias",
      "alias_is_default");

  final Map<String, Object> values;

  private Attribute(Map<String, Object> values) {
    this.values = values;
  }

  /** As attrs' {@code Attribute.__init__}: derives eq/order and their keys, and freezes metadata. */
  static Attribute create(
      String name, Object dflt, Object validator, Object repr, Object cmp, Object hash, Object init,
      Object inherited, Object metadata, Object type, Object converter, Object kwOnly, Object eq,
      Object eqKey, Object order, Object orderKey, Object onSetattr, Object alias,
      Object aliasIsDefault) throws EvalException {
    Tuple eqOrder = determineAttribEqOrder(
        cmp, Starlark.truth(eqKey) ? eqKey : eq, Starlark.truth(orderKey) ? orderKey : order, true);
    Map<String, Object> v = new LinkedHashMap<>();
    v.put("name", name);
    v.put("default", dflt);
    v.put("validator", validator);
    v.put("repr", repr);
    v.put("eq", eqOrder.get(0));
    v.put("eq_key", eqOrder.get(1));
    v.put("order", eqOrder.get(2));
    v.put("order_key", eqOrder.get(3));
    v.put("hash", hash);
    v.put("init", init);
    v.put("metadata", freezeMetadata(metadata));
    v.put("type", type);
    v.put("converter", converter);
    v.put("kw_only", kwOnly);
    v.put("inherited", inherited);
    v.put("on_setattr", onSetattr);
    v.put("alias", alias);
    v.put("alias_is_default",
        AttrsSupport.isNone(aliasIsDefault) ? AttrsSupport.isNone(alias) : aliasIsDefault);
    return new Attribute(v);
  }

  private static Dict<Object, Object> freezeMetadata(Object metadata) throws EvalException {
    if (AttrsSupport.isNone(metadata) || !Starlark.truth(metadata)) {
      return Dict.empty();
    }
    if (!(metadata instanceof Map)) {
      throw Starlark.errorf("TypeError: metadata must be a dict, not %s", Starlark.type(metadata));
    }
    Dict.Builder<Object, Object> b = Dict.builder();
    for (Map.Entry<?, ?> e : ((Map<?, ?>) metadata).entrySet()) {
      b.put(e.getKey(), e.getValue());
    }
    return b.buildImmutable();
  }

  /** attrs' {@code _determine_attrib_eq_order}: returns {@code (eq, eq_key, order, order_key)}. */
  static Tuple determineAttribEqOrder(Object cmp, Object eq, Object order, boolean defaultEq)
      throws EvalException {
    if (!AttrsSupport.isNone(cmp) && (!AttrsSupport.isNone(eq) || !AttrsSupport.isNone(order))) {
      throw AttrsSupport.error("ValueError", "Don't mix `cmp` with `eq' and `order`.");
    }
    if (!AttrsSupport.isNone(cmp)) {
      Object[] c = decideCallableOrBoolean(cmp);
      return Tuple.of(c[0], c[1], c[0], c[1]);
    }
    Object[] e = AttrsSupport.isNone(eq) ? new Object[] {defaultEq, Starlark.NONE} : decideCallableOrBoolean(eq);
    Object[] o = AttrsSupport.isNone(order) ? e : decideCallableOrBoolean(order);
    if (Boolean.FALSE.equals(e[0]) && Boolean.TRUE.equals(o[0])) {
      throw AttrsSupport.error("ValueError", "`order` can only be True if `eq` is True too.");
    }
    return Tuple.of(e[0], e[1], o[0], o[1]);
  }

  private static Object[] decideCallableOrBoolean(Object value) throws EvalException {
    if (AttrsSupport.isCallable(value)) {
      return new Object[] {true, value};
    }
    return new Object[] {value, Starlark.NONE};
  }

  String name() {
    return (String) values.get("name");
  }

  Object dflt() {
    return values.get("default");
  }

  Object validator() {
    return values.get("validator");
  }

  Object converter() {
    return values.get("converter");
  }

  Object onSetattr() {
    return values.get("on_setattr");
  }

  Object reprFlag() {
    return values.get("repr");
  }

  boolean eq() {
    return Starlark.truth(values.get("eq"));
  }

  Object eqKey() {
    return values.get("eq_key");
  }

  boolean order() {
    return Starlark.truth(values.get("order"));
  }

  Object orderKey() {
    return values.get("order_key");
  }

  boolean init() {
    return Starlark.truth(values.get("init"));
  }

  boolean kwOnly() {
    return Starlark.truth(values.get("kw_only"));
  }

  /** The __init__ parameter name. */
  String alias() {
    Object alias = values.get("alias");
    return AttrsSupport.isNone(alias) ? name() : (String) alias;
  }

  @Nullable
  @Override
  public Object getValue(String name) {
    return values.get(name);
  }

  @Override
  public ImmutableList<String> getFieldNames() {
    return FIELDS;
  }

  @Nullable
  @Override
  public String getErrorMessageForUnknownField(String field) {
    return "'Attribute' object has no attribute '" + field + "'";
  }

  @Override
  public void setField(String field, Object value) throws EvalException {
    throw AttrsSupport.error("FrozenInstanceError", "can't set attribute");
  }

  @StarlarkMethod(
      name = "evolve",
      doc = "Copy *self* and apply *changes*.",
      extraKeywords = @Param(name = "changes"),
      useStarlarkThread = true)
  public Attribute evolve(Dict<String, Object> changes, StarlarkThread thread) throws EvalException {
    Map<String, Object> v = new LinkedHashMap<>(values);
    for (Map.Entry<String, Object> e : changes.entrySet()) {
      if (!values.containsKey(e.getKey())) {
        throw Starlark.errorf("AttributeError: 'Attribute' object has no attribute '%s'", e.getKey());
      }
      v.put(e.getKey(), e.getKey().equals("metadata") ? freezeMetadata(e.getValue()) : e.getValue());
    }
    if (changes.containsKey("alias") && !changes.containsKey("alias_is_default")) {
      v.put("alias_is_default", false);
    } else if (changes.containsKey("name") && !changes.containsKey("alias")
        && Starlark.truth(values.get("alias_is_default"))) {
      v.put("alias", defaultInitAliasFor((String) v.get("name")));
    }
    return new Attribute(v);
  }

  /** attrs' {@code _default_init_alias_for}: the name without leading underscores. */
  static String defaultInitAliasFor(String name) {
    int i = 0;
    while (i < name.length() && name.charAt(i) == '_') {
      i++;
    }
    return name.substring(i);
  }

  @Override
  public void repr(Printer p, StarlarkSemantics semantics) {
    p.append("Attribute(");
    String sep = "";
    for (String f : FIELDS) {
      if (f.equals("alias_is_default")) {
        continue; // repr=False upstream
      }
      p.append(sep).append(f).append("=");
      if (f.equals("metadata")) {
        p.append("mappingproxy(").repr(values.get(f), semantics).append(")");
      } else {
        p.repr(values.get(f), semantics);
      }
      sep = ", ";
    }
    p.append(")");
  }

  /** Equal when every field but {@code inherited} is equal, as attrs' generated __eq__. */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Attribute)) {
      return false;
    }
    Attribute other = (Attribute) o;
    for (String f : FIELDS) {
      if (!f.equals("inherited") && !Objects.equals(values.get(f), other.values.get(f))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(values.get("name"), values.get("alias"));
  }

  @Override
  public boolean isImmutable() {
    for (Object v : values.values()) {
      if (!Starlark.isImmutable(v)) {
        return false;
      }
    }
    return true;
  }
}
