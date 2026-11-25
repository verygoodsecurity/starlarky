// Copyright 2026 Very Good Security Authors
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

package com.verygood.security.larky.modules.multidict;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.modules.types.structs.SimpleStruct;
import com.verygood.security.larky.runtime.CurrentEvaluation;
import com.verygood.security.larky.parser.StarlarkUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkStrings;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

/**
 * The multidicts of {@code @vendor//multidict} ({@code MultiDict}, {@code CIMultiDict} and their
 * read-only proxies), implemented natively behind the same Larky facade: an instance is a struct
 * whose {@code __name__} and {@code __class__} name the facade function that made it, whose
 * attributes may be set (as {@code VGSCIMultiDict} does), and whose methods behave as the
 * {@code .star} implementation's did (ported from multidict 5.1.0's {@code _multidict_py.py}),
 * including its error messages and per-instance version counter.
 *
 * <p>Methods are looked up after the instance's own attributes, so an attribute set on an instance
 * overrides the method of that name, as it did when the methods were attributes.
 */
public final class LarkyMultiDict extends SimpleStruct implements Mutability.Freezable {

  /** {@code (identity, key, value)}: the identity is the key, or {@code key.title()} if case-insensitive. */
  private record Entry(Object identity, Object key, Object value) {}

  /** The entries and version a multidict shares with its proxies. */
  private static final class Store {
    private final ArrayList<Entry> entries = new ArrayList<>();
    private final Mutability mutability;
    private int version = 1;

    private Store(Mutability mutability) {
      this.mutability = mutability;
    }
  }

  private enum Method {
    GETALL("getall", false),
    GETONE("getone", false),
    GET("get", false),
    GETITEM("__getitem__", false),
    ITER("__iter__", false),
    LEN("__len__", false),
    KEYS("keys", false),
    ITEMS("items", false),
    VALUES("values", false),
    EQ("__eq__", false),
    NE("__ne__", false),
    CONTAINS("__contains__", false),
    REPR("__repr__", false),
    COPY("copy", false),
    REDUCE("__reduce__", false),
    DUNDER_COPY("__copy__", true),
    ADD("add", true),
    EXTEND("extend", true),
    CLEAR("clear", true),
    SETITEM("__setitem__", true),
    DELITEM("__delitem__", true),
    SETDEFAULT("setdefault", true),
    POPONE("popone", true),
    POP("pop", true),
    POPALL("popall", true),
    POPITEM("popitem", true),
    UPDATE("update", true);

    private final String starlarkName;
    /** Only a multidict has it; a proxy does not. */
    private final boolean mutableOnly;

    Method(String starlarkName, boolean mutableOnly) {
      this.starlarkName = starlarkName;
      this.mutableOnly = mutableOnly;
    }
  }

  private static final ImmutableMap<String, Method> METHODS;

  static {
    ImmutableMap.Builder<String, Method> methods = ImmutableMap.builder();
    for (Method m : Method.values()) {
      methods.put(m.starlarkName, m);
    }
    METHODS = methods.buildOrThrow();
  }

  /** The instance's attributes, which SimpleStruct reads as its fields. */
  private final Map<String, Object> attrs;
  private final Store store;
  private final boolean caseInsensitive;
  private final boolean proxy;
  /** What a proxy's copy() calls with its items: the facade's MultiDict or CIMultiDict. */
  @Nullable private final Object proxyCopyClass;

  private LarkyMultiDict(
      Map<String, Object> attrs,
      Store store,
      boolean caseInsensitive,
      boolean proxy,
      @Nullable Object proxyCopyClass) {
    super(attrs);
    this.attrs = attrs;
    this.store = store;
    this.caseInsensitive = caseInsensitive;
    this.proxy = proxy;
    this.proxyCopyClass = proxyCopyClass;
  }

  private static Map<String, Object> attrs(Object cls, String name) {
    Map<String, Object> attrs = new LinkedHashMap<>(4);
    attrs.put(PyProtocols.__NAME__, name);
    attrs.put(PyProtocols.__CLASS__, cls);
    return attrs;
  }

  /**
   * {@code MultiDict(*args, **kwargs)} or {@code CIMultiDict(*args, **kwargs)}: {@code cls} is the
   * facade function, the instance's {@code __class__}.
   */
  static LarkyMultiDict create(
      Object cls,
      String name,
      boolean caseInsensitive,
      Tuple args,
      Dict<String, Object> kwargs,
      StarlarkThread thread)
      throws EvalException, InterruptedException {
    LarkyMultiDict md =
        new LarkyMultiDict(
            attrs(cls, name), new Store(thread.mutability()), caseInsensitive, false, null);
    md.extend(args, kwargs, name, false, thread);
    return md;
  }

  /** {@code MultiDictProxy(md)} or {@code CIMultiDictProxy(md)}: a read-only view of {@code md}. */
  static LarkyMultiDict proxy(
      Object cls, String name, boolean caseInsensitive, Object copyClass, Object arg)
      throws EvalException {
    if (!(arg instanceof LarkyMultiDict md)) {
      throw Starlark.errorf(
          "TypeError: %s requires a multidict, not %s", name, StarlarkUtil.richType(arg));
    }
    return new LarkyMultiDict(attrs(cls, name), md.store, caseInsensitive, true, copyClass);
  }

  /** The Mutability of the evaluation that created the multidict (a proxy: its multidict's). */
  @Override
  public Mutability mutability() {
    return store.mutability;
  }

  /** The multidict's change counter: 1 when created empty, plus one per change. */
  int version() {
    return store.version;
  }

  // Attributes

  @Nullable
  @Override
  public Object getField(String name, @Nullable StarlarkThread thread) {
    Object attr = attrs.get(name);
    if (attr != null) {
      return attr;
    }
    Method method = METHODS.get(name);
    if (method != null && has(method)) {
      return new BoundMethod(this, method);
    }
    return super.getField(name, thread);
  }

  private boolean has(Method method) {
    return !(proxy && method.mutableOnly);
  }

  @Override
  public ImmutableList<String> getFieldNames() {
    ImmutableList.Builder<String> names = ImmutableList.<String>builder().addAll(attrs.keySet());
    for (Method m : Method.values()) {
      if (has(m) && !attrs.containsKey(m.starlarkName)) {
        names.add(m.starlarkName);
      }
    }
    return names.build();
  }

  @Override
  public void setField(String name, Object value) throws EvalException {
    if (store.mutability.isFrozen()) {
      throw new EvalException("Attempting to update a frozen structure");
    }
    attrs.put(name, value);
  }

  private boolean overridden(String name) {
    return attrs.containsKey(name);
  }

  // Operators: the native methods unless the instance overrides them

  @Override
  public Object getIndex(StarlarkThread thread, StarlarkSemantics semantics, Object key)
      throws EvalException {
    if (overridden(PyProtocols.__GETITEM__)) {
      return super.getIndex(thread, semantics, key);
    }
    return getone(thread, key, null);
  }

  @Override
  public void setIndex(StarlarkThread thread, StarlarkSemantics semantics, Object key, Object value)
      throws EvalException {
    if (proxy || overridden(PyProtocols.__SETITEM__)) {
      super.setIndex(thread, semantics, key, value);
      return;
    }
    Starlark.checkHashable(key);
    replace(thread, key, value);
  }

  @Override
  public boolean containsKey(StarlarkThread thread, StarlarkSemantics semantics, Object key)
      throws EvalException {
    if (overridden(PyProtocols.__CONTAINS__)) {
      return super.containsKey(thread, semantics, key);
    }
    try {
      return contains(thread, key);
    } catch (EvalException e) {
      // As for any struct: when __contains__ fails, `in` compares the iterated keys instead.
      for (Entry entry : store.entries) {
        if (Starlark.checkedEquals(entry.key(), key)) {
          return true;
        }
      }
      return false;
    }
  }

  @Override
  public Iterator<Object> iterator() {
    if (overridden(PyProtocols.__ITER__)) {
      return super.iterator();
    }
    // iterates a copy of the keys, as the .star implementation did (iter(self.keys()))
    charge(CurrentEvaluation.currentOrNull(), store.entries.size());
    List<Object> keys = new ArrayList<>(store.entries.size());
    for (Entry e : store.entries) {
      keys.add(e.key());
    }
    return keys.iterator();
  }

  @Override
  public boolean truth() {
    if (overridden(PyProtocols.__BOOL__) || overridden(PyProtocols.__LEN__)) {
      return super.truth();
    }
    return !store.entries.isEmpty();
  }

  @Override
  public void repr(Printer printer, StarlarkSemantics semantics) {
    if (overridden(PyProtocols.__REPR__)) {
      super.repr(printer, semantics);
      return;
    }
    charge(CurrentEvaluation.currentOrNull(), store.entries.size());
    printer.append(reprString(semantics));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (overridden(PyProtocols.__EQ__)) {
      return super.equals(obj);
    }
    // As for any struct, == consults __eq__ only when the other side is a struct too.
    if (!(obj instanceof SimpleStruct)) {
      return false;
    }
    StarlarkThread thread = CurrentEvaluation.currentOrNull();
    try {
      return eq(thread, obj);
    } catch (EvalException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int hashCode() {
    if (overridden(PyProtocols.__HASH__)) {
      return super.hashCode();
    }
    return System.identityHashCode(this);
  }

  // Methods

  private Object call(Method method, StarlarkThread thread, Tuple args, Dict<String, Object> kwargs)
      throws EvalException, InterruptedException {
    String fn = method.starlarkName;
    switch (method) {
      case GETALL -> {
        Object[] a = Args.bind(fn, args, kwargs, 1, "key", "default");
        return getall(thread, a[0], a[1]);
      }
      case GETONE -> {
        Object[] a = Args.bind(fn, args, kwargs, 1, "key", "default");
        return getone(thread, a[0], a[1]);
      }
      case GET -> {
        Object[] a = Args.bind(fn, args, kwargs, 1, "key", "default");
        return getone(thread, a[0], a[1] == null ? Starlark.NONE : a[1]);
      }
      case GETITEM -> {
        return getone(thread, Args.bind(fn, args, kwargs, 1, "key")[0], null);
      }
      case ITER -> {
        Args.bind(fn, args, kwargs, 0);
        return com.verygood.security.larky.modules.types.LarkyIterator.from(keys(thread), thread);
      }
      case LEN -> {
        Args.bind(fn, args, kwargs, 0);
        return StarlarkInt.of(store.entries.size());
      }
      case KEYS -> {
        Args.bind(fn, args, kwargs, 0);
        return keys(thread);
      }
      case ITEMS -> {
        Args.bind(fn, args, kwargs, 0);
        return items(thread);
      }
      case VALUES -> {
        Args.bind(fn, args, kwargs, 0);
        charge(thread, store.entries.size());
        List<Object> values = new ArrayList<>(store.entries.size());
        for (Entry e : store.entries) {
          values.add(e.value());
        }
        return StarlarkList.copyOf(thread.mutability(), values);
      }
      case EQ -> {
        return eq(thread, Args.bind(fn, args, kwargs, 1, "other")[0]);
      }
      case NE -> {
        return !eq(thread, Args.bind(fn, args, kwargs, 1, "other")[0]);
      }
      case CONTAINS -> {
        return contains(thread, Args.bind(fn, args, kwargs, 1, "key")[0]);
      }
      case REPR -> {
        Args.bind(fn, args, kwargs, 0);
        charge(thread, store.entries.size());
        return reprString(thread.getSemantics());
      }
      case COPY, DUNDER_COPY -> {
        Args.bind(fn, args, kwargs, 0);
        Object cls = proxy ? proxyCopyClass : attrs.get(PyProtocols.__CLASS__);
        return Starlark.call(thread, cls, ImmutableList.of(itemsAttr(thread)), ImmutableMap.of());
      }
      case REDUCE -> {
        Args.bind(fn, args, kwargs, 0);
        if (proxy) {
          throw Starlark.errorf("TypeError: can't pickle %s objects", typeName());
        }
        return Tuple.pair(attrs.get(PyProtocols.__CLASS__), Tuple.of(itemsAttr(thread)));
      }
      case ADD -> {
        Object[] a = Args.bind(fn, args, kwargs, 2, "key", "value");
        add(thread, a[0], a[1]);
        return Starlark.NONE;
      }
      case EXTEND -> {
        extend(args, kwargs, "extend", false, thread);
        return Starlark.NONE;
      }
      case CLEAR -> {
        Args.bind(fn, args, kwargs, 0);
        checkMutable();
        store.entries.clear();
        store.version++;
        return Starlark.NONE;
      }
      case SETITEM -> {
        Object[] a = Args.bind(fn, args, kwargs, 2, "key", "value");
        replace(thread, a[0], a[1]);
        return Starlark.NONE;
      }
      case DELITEM -> {
        delitem(thread, Args.bind(fn, args, kwargs, 1, "key")[0]);
        return Starlark.NONE;
      }
      case SETDEFAULT -> {
        Object[] a = Args.bind(fn, args, kwargs, 1, "key", "default");
        return setdefault(thread, a[0], a[1] == null ? Starlark.NONE : a[1]);
      }
      case POPONE, POP -> {
        Object[] a = Args.bind(fn, args, kwargs, 1, "key", "default");
        return popone(thread, a[0], a[1]);
      }
      case POPALL -> {
        Object[] a = Args.bind(fn, args, kwargs, 1, "key", "default");
        return popall(thread, a[0], a[1]);
      }
      case POPITEM -> {
        Args.bind(fn, args, kwargs, 0);
        if (store.entries.isEmpty()) {
          throw Starlark.errorf("KeyError: empty multidict");
        }
        checkMutable();
        Entry e = store.entries.remove(0);
        store.version++;
        return Tuple.pair(e.key(), e.value());
      }
      case UPDATE -> {
        extend(args, kwargs, "update", true, thread);
        return Starlark.NONE;
      }
    }
    throw new IllegalStateException(method.name());
  }

  /**
   * Charges the evaluation one step per entry an operation visits, so that the step budget bounds
   * the work done here as it bounded the .star implementation's loops.
   */
  private static void charge(@Nullable StarlarkThread thread, long entries) {
    if (thread != null && entries > 0) {
      thread.incrementExecutedSteps(entries);
    }
  }

  private void checkMutable() throws EvalException {
    Starlark.checkMutable(this);
  }

  /** The identity of {@code key}: {@code key.title()} if case-insensitive, else {@code key}. */
  private Object identity(StarlarkThread thread, Object key)
      throws EvalException, InterruptedException {
    if (!caseInsensitive) {
      return key;
    }
    if (key instanceof String s) {
      return StarlarkStrings.title(s, thread);
    }
    // an istr, or a value without title(), which fails here as key.title() did
    Object title = Starlark.getattr(thread.mutability(), thread.getSemantics(), key, "title", null);
    return Starlark.call(thread, title, ImmutableList.of(), ImmutableMap.of());
  }

  /** Checks that {@code key} may be a key: a string, or an istr. */
  private static Object checkKey(StarlarkThread thread, Object key) throws EvalException {
    if (key instanceof String || Starlark.hasattr(thread.getSemantics(), key, "__is_istr__")) {
      return key;
    }
    throw Starlark.errorf(
        "TypeError: MultiDict keys should be str. Received %s", StarlarkUtil.richType(key));
  }

  private static boolean same(Object storedIdentity, Object identity) throws EvalException {
    return Starlark.checkedEquals(storedIdentity, identity);
  }

  /**
   * {@code operator.eq(lhs, rhs)}, which getall() compares with (getone() and the rest use ==): the
   * __eq__ of whichever side defines one (the right side's first when their types are the same),
   * else ==. So an istr key matches a str one in getall() only, as before.
   */
  private static boolean richEq(StarlarkThread thread, Object lhs, Object rhs)
      throws EvalException, InterruptedException {
    if (lhs instanceof String && rhs instanceof String) {
      return lhs.equals(rhs);
    }
    Object lhsEq = Starlark.getattr(thread.mutability(), thread.getSemantics(), lhs, "__eq__", NO_EQ);
    Object rhsEq = Starlark.getattr(thread.mutability(), thread.getSemantics(), rhs, "__eq__", NO_EQ);
    boolean sameType = StarlarkUtil.richType(lhs).equals(StarlarkUtil.richType(rhs));
    Object[][] calls = sameType
        ? new Object[][] {{rhsEq, lhs}, {lhsEq, rhs}}
        : new Object[][] {{lhsEq, rhs}, {rhsEq, lhs}};
    for (Object[] call : calls) {
      if (call[0] != NO_EQ) {
        return Starlark.truth(
            Starlark.call(thread, call[0], ImmutableList.of(call[1]), ImmutableMap.of()));
      }
    }
    return Starlark.checkedEquals(lhs, rhs);
  }

  /** The default of richEq()'s attribute lookups: a value no attribute holds. */
  private static final Object NO_EQ = new MissingValue();

  private Object getall(StarlarkThread thread, Object key, @Nullable Object dflt)
      throws EvalException {
    Object identity = identityUnchecked(thread, key);
    charge(thread, store.entries.size());
    List<Object> res = new ArrayList<>();
    try {
      for (Entry e : store.entries) {
        if (richEq(thread, e.identity(), identity)) {
          res.add(e.value());
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new EvalException(e);
    }
    if (!res.isEmpty()) {
      return StarlarkList.copyOf(thread.mutability(), res);
    }
    if (dflt != null) {
      return dflt;
    }
    throw keyNotFound(thread, key);
  }

  private Object getone(StarlarkThread thread, Object key, @Nullable Object dflt)
      throws EvalException {
    Object identity = identityUnchecked(thread, key);
    charge(thread, store.entries.size());
    for (Entry e : store.entries) {
      if (same(e.identity(), identity)) {
        return e.value();
      }
    }
    if (dflt != null) {
      return dflt;
    }
    throw keyNotFound(thread, key);
  }

  private EvalException keyNotFound(StarlarkThread thread, Object key) throws EvalException {
    return Starlark.errorf(
        "KeyError: %s", StarlarkStrings.percent(thread, "Key not found: %r", key));
  }

  private EvalException keyError(StarlarkThread thread, Object key) throws EvalException {
    return Starlark.errorf("%s", StarlarkStrings.percent(thread, "KeyError: %s", key));
  }

  /** {@link #identity} for callers that cannot throw InterruptedException (an istr's title()). */
  private Object identityUnchecked(StarlarkThread thread, Object key) throws EvalException {
    try {
      return identity(thread, key);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new EvalException(e);
    }
  }

  private boolean contains(StarlarkThread thread, Object key) throws EvalException {
    Object identity = identityUnchecked(thread, key);
    charge(thread, store.entries.size());
    for (Entry e : store.entries) {
      if (same(e.identity(), identity)) {
        return true;
      }
    }
    return false;
  }

  private StarlarkList<Object> keys(StarlarkThread thread) {
    charge(thread, store.entries.size());
    List<Object> keys = new ArrayList<>(store.entries.size());
    for (Entry e : store.entries) {
      keys.add(e.key());
    }
    return StarlarkList.copyOf(thread.mutability(), keys);
  }

  /** {@code self.items()}: an items attribute set on the instance if there is one, as before. */
  private Object itemsAttr(StarlarkThread thread) throws EvalException, InterruptedException {
    Object override = attrs.get("items");
    if (override == null) {
      return items(thread);
    }
    return Starlark.call(thread, override, ImmutableList.of(), ImmutableMap.of());
  }

  private StarlarkList<Object> items(StarlarkThread thread) {
    charge(thread, store.entries.size());
    List<Object> items = new ArrayList<>(store.entries.size());
    for (Entry e : store.entries) {
      items.add(Tuple.pair(e.key(), e.value()));
    }
    return StarlarkList.copyOf(thread.mutability(), items);
  }

  private String reprString(StarlarkSemantics semantics) {
    StringBuilder body = new StringBuilder();
    for (Entry e : store.entries) {
      if (body.length() > 0) {
        body.append(", ");
      }
      body.append('\'').append(Starlark.str(e.key(), semantics)).append("': ")
          .append(Starlark.repr(e.value(), semantics));
    }
    return "<" + Starlark.str(attrs.get(PyProtocols.__NAME__), semantics) + "(" + body + ")>";
  }

  private boolean eq(@Nullable StarlarkThread thread, Object other)
      throws EvalException, InterruptedException {
    if (other instanceof LarkyMultiDict md
        && Starlark.checkedEquals(
            md.attrs.get(PyProtocols.__CLASS__), attrs.get(PyProtocols.__CLASS__))) {
      List<Entry> lft = store.entries;
      List<Entry> rht = md.store.entries;
      charge(thread != null ? thread : CurrentEvaluation.currentOrNull(), lft.size());
      if (lft.size() != rht.size()) {
        return false;
      }
      for (int i = 0; i < lft.size(); i++) {
        if (!Starlark.checkedEquals(lft.get(i).identity(), rht.get(i).identity())
            || !Starlark.checkedEquals(lft.get(i).value(), rht.get(i).value())) {
          return false;
        }
      }
      return true;
    }
    if (thread == null) {
      thread = CurrentEvaluation.requireThread();
    }
    if (store.entries.size() != length(thread, other)) {
      return false;
    }
    charge(thread, store.entries.size());
    Object get = other instanceof Dict<?, ?> ? null
        : Starlark.getattr(thread.mutability(), thread.getSemantics(), other, "get", null);
    for (Entry e : store.entries) {
      Object nv;
      if (other instanceof Dict<?, ?> d) {
        nv = d.containsKey(e.key()) ? d.get(e.key()) : null;
      } else {
        nv = Starlark.call(thread, get, ImmutableList.of(e.key(), MISSING), ImmutableMap.of());
        if (nv == MISSING) {
          nv = null;
        }
      }
      if (nv == null || !Starlark.checkedEquals(e.value(), nv)) {
        return false;
      }
    }
    return true;
  }

  /** Default for other.get() in eq(): a value no mapping holds. */
  private static final Object MISSING = new MissingValue();

  /** Has no state: shared by every evaluation. */
  private static final class MissingValue implements net.starlark.java.eval.StarlarkValue {
    @Override
    public void repr(Printer printer, StarlarkSemantics semantics) {
      printer.append("<missing>");
    }

    @Override
    public boolean isImmutable() {
      return true;
    }
  }

  private static int length(StarlarkThread thread, Object x) throws EvalException {
    if (x instanceof LarkyMultiDict md) {
      return md.store.entries.size();
    }
    if (x instanceof com.verygood.security.larky.modules.types.LarkyObject obj
        && obj.hasLenField()) {
      return ((StarlarkInt) obj.invoke(thread, obj.getField(PyProtocols.__LEN__),
          ImmutableList.of(), ImmutableMap.of())).toIntUnchecked();
    }
    int len = Starlark.len(x);
    if (len < 0) {
      throw Starlark.errorf("object of type '%s' has no len()", Starlark.type(x));
    }
    return len;
  }

  private void add(StarlarkThread thread, Object key, Object value)
      throws EvalException, InterruptedException {
    Object identity = identity(thread, key);
    Object k = checkKey(thread, key);
    checkMutable();
    store.entries.add(new Entry(identity, k, value));
    store.version++;
  }

  /**
   * extend() and update() (and the constructor, as extend under the type's name): at most one
   * positional argument (a multidict, a mapping or an iterable of pairs) plus keyword arguments.
   */
  private void extend(
      Tuple args, Dict<String, Object> kwargs, String name, boolean update, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (args.size() > 1) {
      throw Starlark.errorf(
          "TypeError: %s takes at most 1 positional argument (%d given)", name, args.size());
    }
    List<Entry> items = new ArrayList<>();
    if (!args.isEmpty()) {
      Object arg = args.get(0);
      if (kwargs.isEmpty() && arg instanceof LarkyMultiDict md) {
        // another multidict's entries, identities included
        items.addAll(md.store.entries);
        if (update) {
          updateItems(thread, items);
        } else {
          // re-keyed one at a time, as adding them did
          for (Entry e : items) {
            add(thread, e.key(), e.value());
          }
        }
        return;
      }
      if (arg instanceof Dict<?, ?> dict) {
        for (Map.Entry<?, ?> e : dict.entrySet()) {
          items.add(entry(thread, e.getKey(), e.getValue()));
        }
      } else {
        Object source = arg;
        if (Starlark.hasattr(thread.getSemantics(), arg, "items")) {
          Object itemsMethod =
              Starlark.getattr(thread.mutability(), thread.getSemantics(), arg, "items", null);
          source = Starlark.call(thread, itemsMethod, ImmutableList.of(), ImmutableMap.of());
        }
        for (Object item : Starlark.toIterable(source)) {
          items.add(pair(thread, name, item));
        }
      }
    }
    for (Map.Entry<String, Object> e : kwargs.entrySet()) {
      items.add(entry(thread, e.getKey(), e.getValue()));
    }
    charge(thread, items.size());
    if (update) {
      updateItems(thread, items);
      return;
    }
    if (!items.isEmpty()) {
      checkMutable();
    }
    for (Entry e : items) {
      store.entries.add(e);
      store.version++;
    }
  }

  private Entry entry(StarlarkThread thread, Object key, Object value)
      throws EvalException, InterruptedException {
    Object identity = identity(thread, key);
    return new Entry(identity, checkKey(thread, key), value);
  }

  private Entry pair(StarlarkThread thread, String name, Object item)
      throws EvalException, InterruptedException {
    if (item instanceof Sequence<?> seq && seq.size() == 2) {
      return entry(thread, seq.get(0), seq.get(1));
    }
    if (item instanceof String s && s.length() == 2) {
      return entry(thread, s.substring(0, 1), s.substring(1));
    }
    throw Starlark.errorf(
        "TypeError: %s expects (key, value) pairs; got %s",
        name, Starlark.repr(item, thread.getSemantics()));
  }

  private void updateItems(StarlarkThread thread, List<Entry> items) throws EvalException {
    if (items.isEmpty()) {
      return;
    }
    checkMutable();
    ArrayList<Entry> entries = store.entries;
    Map<Object, Integer> usedKeys = new HashMap<>();
    for (Entry item : items) {
      Starlark.checkHashable(item.identity());
      int start = usedKeys.getOrDefault(item.identity(), 0);
      boolean exhausted = true;
      for (int i = start; i < entries.size(); i++) {
        charge(thread, 1);
        if (same(entries.get(i).identity(), item.identity())) {
          usedKeys.put(item.identity(), i + 1);
          entries.set(i, item);
          exhausted = false;
          break;
        }
      }
      if (exhausted) {
        entries.add(item);
        usedKeys.put(item.identity(), entries.size());
      }
    }
    // drop the tails: entries of an updated key after the last one updated
    charge(thread, entries.size());
    int i = 0;
    while (i < entries.size()) {
      Integer pos = usedKeys.get(entries.get(i).identity());
      if (pos != null && i >= pos) {
        entries.remove(i);
      } else {
        i++;
      }
    }
    store.version++;
  }

  /** {@code md[key] = value}: replaces the first entry for the key and drops the others. */
  private void replace(StarlarkThread thread, Object key, Object value) throws EvalException {
    Object k = checkKey(thread, key);
    Object identity = identityUnchecked(thread, k);
    checkMutable();
    ArrayList<Entry> entries = store.entries;
    charge(thread, entries.size());
    int found = -1;
    for (int i = 0; i < entries.size(); i++) {
      if (same(entries.get(i).identity(), identity)) {
        entries.set(i, new Entry(identity, k, value));
        store.version++;
        found = i;
        break;
      }
    }
    if (found < 0) {
      entries.add(new Entry(identity, k, value));
      store.version++;
      return;
    }
    int i = found + 1;
    while (i < entries.size()) {
      if (same(entries.get(i).identity(), identity)) {
        entries.remove(i);
      } else {
        i++;
      }
    }
  }

  private void delitem(StarlarkThread thread, Object key) throws EvalException {
    Object identity = identityUnchecked(thread, key);
    ArrayList<Entry> entries = store.entries;
    charge(thread, entries.size());
    boolean found = false;
    for (int i = entries.size() - 1; i >= 0; i--) {
      if (same(entries.get(i).identity(), identity)) {
        checkMutable();
        entries.remove(i);
        found = true;
      }
    }
    if (!found) {
      throw keyError(thread, key);
    }
    store.version++;
  }

  private Object setdefault(StarlarkThread thread, Object key, Object dflt)
      throws EvalException, InterruptedException {
    Object identity = identity(thread, key);
    charge(thread, store.entries.size());
    for (Entry e : store.entries) {
      if (same(e.identity(), identity)) {
        return e.value();
      }
    }
    add(thread, key, dflt);
    return dflt;
  }

  private Object popone(StarlarkThread thread, Object key, @Nullable Object dflt)
      throws EvalException {
    Object identity = identityUnchecked(thread, key);
    ArrayList<Entry> entries = store.entries;
    charge(thread, entries.size());
    for (int i = 0; i < entries.size(); i++) {
      if (same(entries.get(i).identity(), identity)) {
        checkMutable();
        Object value = entries.remove(i).value();
        store.version++;
        return value;
      }
    }
    if (dflt != null) {
      return dflt;
    }
    throw keyError(thread, key);
  }

  private Object popall(StarlarkThread thread, Object key, @Nullable Object dflt)
      throws EvalException {
    Object identity = identityUnchecked(thread, key);
    ArrayList<Entry> entries = store.entries;
    charge(thread, entries.size());
    ArrayList<Object> ret = new ArrayList<>();
    for (int i = entries.size() - 1; i >= 0; i--) {
      if (same(entries.get(i).identity(), identity)) {
        checkMutable();
        ret.add(0, entries.remove(i).value());
        store.version++;
      }
    }
    if (!ret.isEmpty()) {
      return StarlarkList.copyOf(thread.mutability(), ret);
    }
    if (dflt != null) {
      return dflt;
    }
    throw keyError(thread, key);
  }

  /** A method of one multidict, as {@code md.name} returns it. */
  private static final class BoundMethod implements StarlarkCallable {
    private final LarkyMultiDict self;
    private final Method method;

    private BoundMethod(LarkyMultiDict self, Method method) {
      this.self = self;
      this.method = method;
    }

    @Override
    public String getName() {
      return method.starlarkName;
    }

    @Override
    public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs)
        throws EvalException, InterruptedException {
      return self.call(method, thread, args, kwargs);
    }

    @Override
    public void repr(Printer printer, StarlarkSemantics semantics) {
      printer.append("<built-in method " + method.starlarkName + " of " + self.typeName()
          + " object>");
    }
  }
}
