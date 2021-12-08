package net.starlark.java.eval;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;

import org.jetbrains.annotations.NotNull;


public interface StarlarkMapping<K, V>
  extends Map<K, V>,
     StarlarkIndexable.Threaded,
     Mutability.Freezable,
     StarlarkIterable<K> {

  void freeze(); // this.mutability = Mutability.IMMUTABLE;
  boolean updateIteratorCount(int delta);
  NavigableMap<K, V> contents(); // deterministic

  @StarlarkMethod(
      name = "get",
      doc =
          "Returns the value for <code>key</code> if <code>key</code> is in the dictionary, "
              + "else <code>default</code>. If <code>default</code> is not given, it defaults to "
              + "<code>None</code>, so that this method never throws an error.",
      parameters = {
        @Param(name = "key", doc = "The key to look for."),
        @Param(
            name = "default",
            defaultValue = "None",
            named = true,
            doc = "The default value to use (instead of None) if the key is not found.")
      },
      useStarlarkThread = true)
  // TODO(adonovan): This method is named get0 as a temporary workaround for a bug in
  // StarlarkAnnotations.getStarlarkMethod. The two 'get' methods cause it to get
  // confused as to which one has the annotation. Fix it and remove "2" suffix.
  default Object get0(Object key, Object defaultValue, StarlarkThread thread) throws EvalException {
    Object v = this.get(key);
    if (v != null) {
      return v;
    }

    // This statement is executed for its effect, which is to throw "unhashable"
    // if key is unhashable, instead of returning defaultValue.
    // I think this is a bug: the correct behavior is simply 'return defaultValue'.
    // See https://github.com/bazelbuild/starlark/issues/65.
    containsKey(thread, thread.getSemantics(), key);

    return defaultValue;
  }

  @StarlarkMethod(
      name = "pop",
      doc =
          "Removes a <code>key</code> from the dict, and returns the associated value. "
              + "If no entry with that key was found, remove nothing and return the specified "
              + "<code>default</code> value; if no default value was specified, fail instead.",
      parameters = {
        @Param(name = "key", doc = "The key."),
        @Param(
            name = "default",
            defaultValue = "unbound",
            named = true,
            doc = "a default value if the key is absent."),
      },
      useStarlarkThread = true)
  default Object pop(Object key, Object defaultValue, StarlarkThread thread) throws EvalException {
    Object value = removeEntry(key);
    if (value != null) {
      return value;
    }

    Starlark.checkHashable(key);

    if (defaultValue != Starlark.UNBOUND) {
      return defaultValue;
    }
    // TODO(adonovan): improve error; this ain't Python.
    throw Starlark.errorf("KeyError: %s", Starlark.repr(key));
  }

  @StarlarkMethod(
      name = "popitem",
      doc =
          "Remove and return the first <code>(key, value)</code> pair from the dictionary. "
              + "<code>popitem</code> is useful to destructively iterate over a dictionary, "
              + "as often used in set algorithms. "
              + "If the dictionary is empty, the <code>popitem</code> call fails.")
  default Tuple popitem() throws EvalException {
    if (isEmpty()) {
      throw Starlark.errorf("popitem: empty dictionary");
    }

    Starlark.checkMutable(this);

    Iterator<Entry<K, V>> iterator = contents().entrySet().iterator();
    Entry<K, V> entry = iterator.next();
    iterator.remove();
    return Tuple.pair(entry.getKey(), entry.getValue());
  }

  @StarlarkMethod(
      name = "setdefault",
      doc =
          "If <code>key</code> is in the dictionary, return its value. "
              + "If not, insert key with a value of <code>default</code> "
              + "and return <code>default</code>. "
              + "<code>default</code> defaults to <code>None</code>.",
      parameters = {
        @Param(name = "key", doc = "The key."),
        @Param(
            name = "default",
            defaultValue = "None",
            named = true,
            doc = "a default value if the key is absent."),
      })
  default V setdefault(K key, V defaultValue) throws EvalException {
    Starlark.checkMutable(this);
    Starlark.checkHashable(key);

    V prev = contents().putIfAbsent(key, defaultValue); // see class doc comment
    return prev != null ? prev : defaultValue;
  }

  @StarlarkMethod(
      name = "update",
      doc =
          "Updates the dictionary first with the optional positional argument, <code>pairs</code>, "
              + " then with the optional keyword arguments\n"
              + "If the positional argument is present, it must be a dict, iterable, or None.\n"
              + "If it is a dict, then its key/value pairs are inserted into this dict. "
              + "If it is an iterable, it must provide a sequence of pairs (or other iterables "
              + "of length 2), each of which is treated as a key/value pair to be inserted.\n"
              + "Each keyword argument <code>name=value</code> causes the name/value "
              + "pair to be inserted into this dict.",
      parameters = {
        @Param(
            name = "pairs",
            defaultValue = "[]",
            doc =
                "Either a dictionary or a list of entries. Entries must be tuples or lists with "
                    + "exactly two elements: key, value."),
      },
      extraKeywords = @Param(name = "kwargs", doc = "Dictionary of additional entries."),
      useStarlarkThread = true)
  default void update(Object pairs, Dict<String, Object> kwargs, StarlarkThread thread)
      throws EvalException {
    Starlark.checkMutable(this);
    Dict<Object, Object> dict = Dict.copyOf(thread.mutability(), this); // see class doc comment
    update("update", dict, pairs, kwargs);
    //noinspection unchecked
    this.contents().putAll(Collections.unmodifiableMap((Map<? extends K, ? extends V>) dict));
  }

  // Common implementation of dict(pairs, **kwargs) and dict.update(pairs, **kwargs).
  static void update(
    String funcname, Dict<Object, Object> dict, Object pairs, Dict<String, Object> kwargs)
      throws EvalException {
    Dict.update(funcname, dict, pairs, kwargs);
  }

  @StarlarkMethod(
      name = "values",
      doc =
          "Returns the list of values:"
              + "<pre class=\"language-python\">"
              + "{2: \"a\", 4: \"b\", 1: \"c\"}.values() == [\"a\", \"b\", \"c\"]</pre>\n",
      useStarlarkThread = true)
  default StarlarkList<?> values0(@NotNull StarlarkThread thread) throws EvalException {
    return StarlarkList.copyOf(thread.mutability(), values());
  }

  @StarlarkMethod(
      name = "items",
      doc =
          "Returns the list of key-value tuples:"
              + "<pre class=\"language-python\">"
              + "{2: \"a\", 4: \"b\", 1: \"c\"}.items() == [(2, \"a\"), (4, \"b\"), (1, \"c\")]"
              + "</pre>\n",
      useStarlarkThread = true)
  default StarlarkList<?> items(StarlarkThread thread) throws EvalException {
    Object[] array = new Object[size()];
    int i = 0;
    for (Map.Entry<?, ?> e : entrySet()) {
      array[i++] = Tuple.pair(e.getKey(), e.getValue());
    }
    return StarlarkEvalWrapper.zeroCopyList(thread.mutability(), array);
  }

  @StarlarkMethod(
      name = "keys",
      doc =
          "Returns the list of keys:"
              + "<pre class=\"language-python\">{2: \"a\", 4: \"b\", 1: \"c\"}.keys() == [2, 4, 1]"
              + "</pre>\n",
      useStarlarkThread = true)
  default StarlarkList<?> keys(StarlarkThread thread) throws EvalException {
    Object[] array = new Object[size()];
    int i = 0;
    for (Map.Entry<?, ?> e : entrySet()) {
      array[i++] = e.getKey();
    }
    return StarlarkEvalWrapper.zeroCopyList(thread.mutability(), array);
  }

  /**
   * Puts an entry into a dict, after validating that mutation is allowed.
   *
   * @param key the key of the added entry
   * @param value the value of the added entry
   * @throws EvalException if the key is invalid or the dict is frozen
   */
  default void putEntry(K key, V value) throws EvalException {
    Starlark.checkMutable(this);
    Starlark.checkHashable(key);
    contents().put(key, value);
  }

  /**
   * Puts all the entries from a given map into the dict, after validating that mutation is allowed.
   *
   * @param map the map whose entries are added
   * @throws EvalException if some key is invalid or the dict is frozen
   */
  default <K2 extends K, V2 extends V> void putEntries(Map<K2, V2> map) throws EvalException {
    Starlark.checkMutable(this);
    for (Map.Entry<K2, V2> e : map.entrySet()) {
      K2 k = e.getKey();
      Starlark.checkHashable(k);
      contents().put(k, e.getValue());
    }
  }

  /**
   * Deletes the entry associated with the given key.
   *
   * @param key the key to delete
   * @return the value associated to the key, or {@code null} if not present
   * @throws EvalException if the dict is frozen
   */
  default V removeEntry(Object key) throws EvalException {
    Starlark.checkMutable(this);
    return contents().remove(key);
  }

  /**
   * Clears the dict.
   *
   * @throws EvalException if the dict is frozen
   */
  @StarlarkMethod(name = "clear", doc = "Remove all items from the dictionary.")
  default void clearEntries() throws EvalException {
    Starlark.checkMutable(this);
    contents().clear();
  }

  @Override
  default boolean truth() {
    return !isEmpty();
  }

  @Override
  default boolean isImmutable() {
    return mutability().isFrozen();
  }


  @Override
  default void unsafeShallowFreeze() {
    Mutability.Freezable.checkUnsafeShallowFreezePrecondition(this);
    freeze();
  }

  @Override
  default void repr(Printer printer) {
     printer.printList(entrySet(), "{", ", ", "}");
   }

  @Override
  default void checkHashable() throws EvalException {
    // Even a frozen dict is unhashable.
    throw Starlark.errorf("unhashable type: 'dict'");
  }

  @Override
  default Object getIndex(StarlarkThread thread, StarlarkSemantics semantics, Object key) throws EvalException {
    Object v = get(key);
    if (v == null) {
      throw Starlark.errorf("key %s not found in dictionary", Starlark.repr(key));
    }
    return v;
  }

  @Override
  default boolean containsKey(StarlarkThread thread, StarlarkSemantics semantics, Object key) throws EvalException {
    Starlark.checkHashable(key);
    return this.containsKey(key);
  }

  @Override
  default Iterator<K> iterator() {
    return contents().keySet().iterator();
  }

// java.util.Map accessors

  @Override
  default boolean containsKey(Object key) {
    return contents().containsKey(key);
  }

  @Override
  default boolean containsValue(Object value) {
    return contents().containsValue(value);
  }

  @Override
  default Set<Entry<K, V>> entrySet() {
    return Collections.unmodifiableMap(contents()).entrySet();
  }

  @Override
  default V get(Object key) {
    return contents().get(key);
  }

  @Override
  default boolean isEmpty() {
    return contents().isEmpty();
  }

  @Override
  default Set<K> keySet() {
    return Collections.unmodifiableMap(contents()).keySet();
  }

  @Override
  default int size() {
    return contents().size();
  }

  @Override
  default Collection<V> values() {
    return Collections.unmodifiableMap(contents()).values();
  }

  // disallowed java.util.Map update operations

  @Deprecated // use clearEntries
  @Override
  default void clear() {
    throw new UnsupportedOperationException();
  }

  @Deprecated // use putEntry
  @Override
  default V put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Deprecated // use putEntries
  @Override
  default void putAll(@NotNull Map<? extends K, ? extends V> map) {
    throw new UnsupportedOperationException();
  }

  @Deprecated // use removeEntry
  @Override
  default V remove(Object key) {
    throw new UnsupportedOperationException();
  }

}

