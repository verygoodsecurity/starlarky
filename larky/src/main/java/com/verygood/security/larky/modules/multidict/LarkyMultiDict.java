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
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.verygood.security.larky.modules.types.LarkyCollection;
import com.verygood.security.larky.modules.types.LarkyInstance;
import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.modules.types.structs.SimpleStruct;
import com.verygood.security.larky.parser.CurrentEvaluation;
import com.verygood.security.larky.parser.StarlarkUtil;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
 * read-only proxies) and {@code VGSCIMultiDict}: an ordered list of (key, value) lines in which a
 * key may repeat. The methods behave as the {@code .star} implementation's did (ported from
 * multidict 5.1.0's {@code _multidict_py.py}), including its error messages and per-instance
 * version counter. Like any {@link LarkyInstance}, an instance's {@code __name__} and
 * {@code __class__} name the facade function that made it, and an attribute set on an instance
 * overrides the method of that name.
 *
 * <p>For Java hosts it is a read-only {@link ListMultimap}: {@link #entries()} gives every line in
 * insertion order, {@link #size()} counts lines, and {@link #get} gives every value of a key. It is
 * not a {@link Map}, which would show one value per key. Where it differs from Guava's contract:
 * <ul>
 *   <li>a case-insensitive multidict matches keys by {@code key.title()}, as a {@code TreeMap} with
 *       {@code String.CASE_INSENSITIVE_ORDER} does, while each entry keeps its own spelling;
 *   <li>its mutators throw {@link UnsupportedOperationException}: scripts change it through its
 *       Starlark methods, which check its mutability;
 *   <li>{@link #equals} and {@link #hashCode} are Starlark's {@code ==} and identity, not
 *       {@link Multimap#equals};
 *   <li>{@link #keys()} and {@link #asMap()} are copies, not views.
 * </ul>
 */
public final class LarkyMultiDict extends LarkyInstance
    implements LarkyCollection, ListMultimap<Object, Object> {

  /** {@code (identity, key, value)}: the identity is the key, or {@code key.title()} if case-insensitive. */
  private record Entry(Object identity, Object key, Object value) {}

  /** The entries and version a multidict shares with its proxies. */
  private static final class Store {
    private final ArrayList<Entry> entries = new ArrayList<>();
    private final Mutability mutability;
    /** The semantics of the evaluation that created it, for {@code title()} outside an evaluation. */
    private final StarlarkSemantics semantics;
    private int version = 1;

    private Store(StarlarkThread thread) {
      this.mutability = thread.mutability();
      this.semantics = thread.getSemantics();
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

  private static final String READ_ONLY =
      "a multidict is changed through its Starlark methods; its Java view is read-only";

  private final String name;
  private final Store store;
  private final boolean caseInsensitive;
  private final boolean proxy;
  /** What a proxy's copy() calls with its items: the facade's MultiDict or CIMultiDict. */
  @Nullable private final Object proxyCopyClass;
  /** str() lists the lines as {"key": repr(value), ...}, as VGSCIMultiDict's __str__ did. */
  private final boolean dictStr;

  private LarkyMultiDict(
      Object cls,
      String name,
      Store store,
      boolean caseInsensitive,
      boolean proxy,
      @Nullable Object proxyCopyClass,
      boolean dictStr) {
    super(cls, store.mutability);
    this.name = name;
    this.store = store;
    this.caseInsensitive = caseInsensitive;
    this.proxy = proxy;
    this.proxyCopyClass = proxyCopyClass;
    this.dictStr = dictStr;
  }

  /**
   * {@code MultiDict(*args, **kwargs)} or {@code CIMultiDict(*args, **kwargs)}: {@code cls} is the
   * facade function, the instance's {@code __class__}.
   */
  static LarkyMultiDict create(
      Object cls,
      String name,
      boolean caseInsensitive,
      boolean dictStr,
      Tuple args,
      Dict<String, Object> kwargs,
      StarlarkThread thread)
      throws EvalException, InterruptedException {
    LarkyMultiDict md =
        new LarkyMultiDict(cls, name, new Store(thread), caseInsensitive, false, null, dictStr);
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
    return new LarkyMultiDict(cls, name, md.store, caseInsensitive, true, copyClass, false);
  }

  @Override
  public String typeName() {
    return name;
  }

  /**
   * Gives {@code visitor} each (key, value) pair, in order, without copying them: what items()
   * returns, for hosts that read the headers natively. Returns false, visiting nothing, when an
   * items attribute set on the instance replaces items(); the host then calls it instead. The
   * visitor must not change the multidict.
   */
  public boolean visitItems(java.util.function.BiConsumer<Object, Object> visitor) {
    if (overridden("items")) {
      return false;
    }
    for (Entry e : store.entries) {
      visitor.accept(e.key(), e.value());
    }
    return true;
  }

  /** The multidict's change counter: 1 when created empty, plus one per change. */
  int version() {
    return store.version;
  }

  // Attributes: a multidict declares none; its methods come after the instance's own attributes

  @Nullable
  @Override
  protected Object getDeclared(String name, StarlarkThread thread) {
    return null;
  }

  @Override
  protected boolean setDeclared(String name, Object value, StarlarkThread thread) {
    return false;
  }

  @Override
  protected void addState(Map<String, Object> state) {}

  @Override
  protected ImmutableList<String> declaredNames() {
    return ImmutableList.of();
  }

  @Nullable
  @Override
  protected Object getMethod(String name) {
    Method method = METHODS.get(name);
    return method != null && has(method) ? new BoundMethod(this, method) : null;
  }

  @Override
  protected ImmutableList<String> methodNames() {
    ImmutableList.Builder<String> names = ImmutableList.builder();
    for (Method m : Method.values()) {
      if (has(m)) {
        names.add(m.starlarkName);
      }
    }
    return names.build();
  }

  private boolean has(Method method) {
    return !(proxy && method.mutableOnly);
  }

  private boolean overridden(String name) {
    return instanceAttribute(name) != null;
  }

  private Object callOverride(String name, Object... args) throws EvalException {
    return invoke(instanceAttribute(name), List.of(args));
  }

  // Operators: the native methods unless the instance overrides them

  @Override
  public Object getIndex(StarlarkThread thread, StarlarkSemantics semantics, Object key)
      throws EvalException {
    if (overridden(PyProtocols.__GETITEM__)) {
      return LarkyCollection.super.getIndex(thread, semantics, key);
    }
    return getone(thread, key, null);
  }

  @Override
  public void setIndex(StarlarkThread thread, StarlarkSemantics semantics, Object key, Object value)
      throws EvalException {
    if (proxy || overridden(PyProtocols.__SETITEM__)) {
      LarkyCollection.super.setIndex(thread, semantics, key, value);
      return;
    }
    Starlark.checkHashable(key);
    replace(thread, key, value);
  }

  @Override
  public boolean containsKey(StarlarkThread thread, StarlarkSemantics semantics, Object key)
      throws EvalException {
    if (overridden(PyProtocols.__CONTAINS__)) {
      return LarkyCollection.super.containsKey(thread, semantics, key);
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
      return LarkyCollection.super.iterator();
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
    try {
      if (overridden(PyProtocols.__BOOL__)) {
        return Starlark.truth(callOverride(PyProtocols.__BOOL__));
      }
      if (overridden(PyProtocols.__LEN__)) {
        return Starlark.truth(callOverride(PyProtocols.__LEN__));
      }
    } catch (EvalException e) {
      throw new RuntimeException(e);
    }
    return !store.entries.isEmpty();
  }

  @Override
  public void repr(Printer printer, StarlarkSemantics semantics) {
    if (overridden(PyProtocols.__REPR__)) {
      try {
        printer.append(Starlark.str(callOverride(PyProtocols.__REPR__), semantics));
      } catch (EvalException e) {
        throw new RuntimeException(e);
      }
      return;
    }
    charge(CurrentEvaluation.currentOrNull(), store.entries.size());
    printer.append(reprString(semantics));
  }

  @Override
  public void str(Printer printer, StarlarkSemantics semantics) {
    if (overridden(PyProtocols.__STR__) || !dictStr) {
      LarkyCollection.super.str(printer, semantics);
      return;
    }
    StarlarkThread thread = CurrentEvaluation.currentOrNull();
    Iterable<?> lines = store.entries;
    if (overridden("items")) {
      // as the Starlark __str__ did, from self.items(), which an override replaces
      try {
        lines = Starlark.toIterable(itemsAttr(CurrentEvaluation.requireThread()));
      } catch (EvalException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    charge(thread, store.entries.size());
    printer.append("{");
    String separator = "";
    for (Object line : lines) {
      Object key;
      Object value;
      if (line instanceof Entry e) {
        key = e.key();
        value = e.value();
      } else {
        Sequence<?> pair = (Sequence<?>) line;
        key = pair.get(0);
        value = pair.get(1);
      }
      printer.append(separator).append("\"").append(Starlark.str(key, semantics)).append("\": ")
          .append(Starlark.repr(value, semantics));
      separator = ", ";
    }
    printer.append("}");
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    try {
      if (overridden(PyProtocols.__EQ__)) {
        return Starlark.truth(callOverride(PyProtocols.__EQ__, obj));
      }
      // As for any struct, == consults __eq__ only when the other side is a struct or multidict too.
      if (!(obj instanceof SimpleStruct) && !(obj instanceof LarkyMultiDict)) {
        return false;
      }
      return eq(CurrentEvaluation.currentOrNull(), obj);
    } catch (EvalException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int hashCode() {
    if (overridden(PyProtocols.__HASH__)) {
      try {
        Object hash = callOverride(PyProtocols.__HASH__);
        if (hash instanceof Boolean b) {
          // bool is an int in Python: hash(True) == hash(1)
          return StarlarkInt.of(b ? 1 : 0).hashCode();
        }
        if (hash instanceof StarlarkInt) {
          return hash.hashCode();
        }
        throw Starlark.errorf("TypeError: __hash__ method should return an integer");
      } catch (EvalException e) {
        throw new RuntimeException(e);
      }
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
        Object cls = proxy ? proxyCopyClass : getField(PyProtocols.__CLASS__, thread);
        return Starlark.call(thread, cls, ImmutableList.of(itemsAttr(thread)), ImmutableMap.of());
      }
      case REDUCE -> {
        Args.bind(fn, args, kwargs, 0);
        if (proxy) {
          throw Starlark.errorf("TypeError: can't pickle %s objects", typeName());
        }
        return Tuple.pair(getField(PyProtocols.__CLASS__, thread), Tuple.of(itemsAttr(thread)));
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
    Object override = instanceAttribute("items");
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
    return "<" + name + "(" + body + ")>";
  }

  private boolean eq(@Nullable StarlarkThread thread, Object other)
      throws EvalException, InterruptedException {
    if (other instanceof LarkyMultiDict md
        && Starlark.checkedEquals(
            md.getField(PyProtocols.__CLASS__, thread), getField(PyProtocols.__CLASS__, thread))) {
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

  // ListMultimap: a read-only view for Java hosts. Reads charge no steps; they are the host's work.

  /** The identity a Java caller's key matches by: {@code key.title()} for a string key if case-insensitive. */
  @Nullable
  private Object javaIdentity(@Nullable Object key) {
    return caseInsensitive && key instanceof String s ? StarlarkStrings.title(s, store.semantics) : key;
  }

  private boolean matches(Entry entry, @Nullable Object identity) {
    return Objects.equals(entry.identity(), identity);
  }

  /** The number of lines. */
  @Override
  public int size() {
    return store.entries.size();
  }

  @Override
  public boolean isEmpty() {
    return store.entries.isEmpty();
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    Object identity = javaIdentity(key);
    for (Entry e : store.entries) {
      if (matches(e, identity)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    for (Entry e : store.entries) {
      if (Objects.equals(e.value(), value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsEntry(@Nullable Object key, @Nullable Object value) {
    Object identity = javaIdentity(key);
    for (Entry e : store.entries) {
      if (matches(e, identity) && Objects.equals(e.value(), value)) {
        return true;
      }
    }
    return false;
  }

  /** Every value of {@code key}, in order: a view that follows changes to the multidict. */
  @Override
  public List<Object> get(@Nullable Object key) {
    Object identity = javaIdentity(key);
    return new AbstractList<>() {
      @Override
      public Object get(int index) {
        int seen = 0;
        for (Entry e : store.entries) {
          if (matches(e, identity) && seen++ == index) {
            return e.value();
          }
        }
        throw new IndexOutOfBoundsException(index);
      }

      @Override
      public int size() {
        int count = 0;
        for (Entry e : store.entries) {
          if (matches(e, identity)) {
            count++;
          }
        }
        return count;
      }
    };
  }

  /** One key per identity, spelled as its first line spells it: a view. */
  @Override
  public Set<Object> keySet() {
    return new AbstractSet<>() {
      @Override
      public Iterator<Object> iterator() {
        Map<Object, Object> first = new LinkedHashMap<>();
        for (Entry e : store.entries) {
          first.putIfAbsent(e.identity(), e.key());
        }
        return Collections.unmodifiableCollection(first.values()).iterator();
      }

      @Override
      public int size() {
        Set<Object> identities = new LinkedHashSet<>();
        for (Entry e : store.entries) {
          identities.add(e.identity());
        }
        return identities.size();
      }

      @Override
      public boolean contains(Object key) {
        return containsKey(key);
      }
    };
  }

  /** Every line's key, as spelled: a copy. */
  @Override
  public Multiset<Object> keys() {
    ImmutableMultiset.Builder<Object> keys = ImmutableMultiset.builder();
    for (Entry e : store.entries) {
      keys.add(e.key());
    }
    return keys.build();
  }

  /** Every line's value, in order: a view. */
  @Override
  public List<Object> values() {
    return new AbstractList<>() {
      @Override
      public Object get(int index) {
        return store.entries.get(index).value();
      }

      @Override
      public int size() {
        return store.entries.size();
      }
    };
  }

  /** Every line as (key, value), in insertion order across keys: a view. */
  @Override
  public List<Map.Entry<Object, Object>> entries() {
    return new AbstractList<>() {
      @Override
      public Map.Entry<Object, Object> get(int index) {
        Entry e = store.entries.get(index);
        return Maps.immutableEntry(e.key(), e.value());
      }

      @Override
      public int size() {
        return store.entries.size();
      }
    };
  }

  /** Each key, spelled as its first line spells it, with all its values: a copy. */
  @Override
  public Map<Object, Collection<Object>> asMap() {
    Map<Object, Object> spelling = new LinkedHashMap<>();
    Map<Object, List<Object>> values = new LinkedHashMap<>();
    for (Entry e : store.entries) {
      spelling.putIfAbsent(e.identity(), e.key());
      values.computeIfAbsent(e.identity(), unused -> new ArrayList<>()).add(e.value());
    }
    Map<Object, Collection<Object>> map = new LinkedHashMap<>();
    for (Map.Entry<Object, List<Object>> e : values.entrySet()) {
      map.put(spelling.get(e.getKey()), Collections.unmodifiableList(e.getValue()));
    }
    return Collections.unmodifiableMap(map);
  }

  @Override
  public boolean put(Object key, Object value) {
    throw new UnsupportedOperationException(READ_ONLY);
  }

  @Override
  public boolean remove(@Nullable Object key, @Nullable Object value) {
    throw new UnsupportedOperationException(READ_ONLY);
  }

  @Override
  public boolean putAll(Object key, Iterable<?> values) {
    throw new UnsupportedOperationException(READ_ONLY);
  }

  @Override
  public boolean putAll(Multimap<?, ?> multimap) {
    throw new UnsupportedOperationException(READ_ONLY);
  }

  @Override
  public List<Object> replaceValues(Object key, Iterable<?> values) {
    throw new UnsupportedOperationException(READ_ONLY);
  }

  @Override
  public List<Object> removeAll(@Nullable Object key) {
    throw new UnsupportedOperationException(READ_ONLY);
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException(READ_ONLY);
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
