package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.types.structs.SimpleStruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

/** Native entry operations behind the Starlark multidict compatibility facade. */
@StarlarkBuiltin(name = "jmultidict", documented = false, doc = "Multidict entry operations.")
public final class MultiDictModule implements StarlarkValue {
  public static final MultiDictModule INSTANCE = new MultiDictModule();

  // The facade yields to the evaluator between chunks of a large collection.
  private static final int MAX_ENTRIES = 64;
  private static final int MAX_KEY_LENGTH = 256;
  private static final StarlarkInt UNSUPPORTED = StarlarkInt.of(-2);
  private static final StarlarkInt MISSING = StarlarkInt.of(-1);

  private MultiDictModule() {}

  @Override
  public boolean isImmutable() {
    return true;
  }

  @StarlarkMethod(
      name = "entries",
      documented = false,
      doc = "Build internal entries for a native dict or sequence of native pairs.",
      parameters = {@Param(name = "source"), @Param(name = "case_insensitive")},
      useStarlarkThread = true)
  public Object entries(Object source, boolean caseInsensitive, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (source instanceof Dict<?, ?> dict && dict.size() <= MAX_ENTRIES) {
      var entries = new Tuple[dict.size()];
      int index = 0;
      for (Map.Entry<?, ?> pair : dict.entrySet()) {
        Tuple entry = entry(pair.getKey(), pair.getValue(), caseInsensitive, thread);
        if (entry == null) {
          return Starlark.NONE;
        }
        entries[index++] = entry;
      }
      return StarlarkList.of(thread.mutability(), entries);
    }
    List<?> pairs = nativeSequence(source);
    if (pairs == null || pairs.size() > MAX_ENTRIES) {
      return Starlark.NONE;
    }
    return entryChunk(source, caseInsensitive, StarlarkInt.of(0), false, thread);
  }

  @StarlarkMethod(
      name = "chunk_size",
      structField = true,
      documented = false,
      doc = "Maximum entries per native chunk.")
  public StarlarkInt chunkSize() {
    return StarlarkInt.of(MAX_ENTRIES);
  }

  @StarlarkMethod(
      name = "sequence_size",
      documented = false,
      doc = "Number of elements in a native list or tuple, or -1.",
      parameters = {@Param(name = "source")})
  public StarlarkInt sequenceSize(Object source) {
    List<?> sequence = nativeSequence(source);
    return sequence == null ? MISSING : StarlarkInt.of(sequence.size());
  }

  @StarlarkMethod(
      name = "entry_chunk",
      documented = false,
      doc = "Convert a chunk of native pairs or backing entries.",
      parameters = {
        @Param(name = "source"), @Param(name = "case_insensitive"),
        @Param(name = "start"), @Param(name = "triples")
      },
      useStarlarkThread = true)
  public Object entryChunk(Object source, boolean caseInsensitive, StarlarkInt offset,
      boolean triples, StarlarkThread thread) throws EvalException, InterruptedException {
    List<?> pairs = nativeSequence(source);
    int start = offset.toInt("start");
    if (pairs == null || start < 0 || start > pairs.size()) {
      return Starlark.NONE;
    }
    int end = start + Math.min(MAX_ENTRIES, pairs.size() - start);
    if (pairs.size() > MAX_ENTRIES) {
      thread.incrementExecutedSteps(end - start);
    }
    var entries = new Tuple[end - start];
    int keyIndex = triples ? 1 : 0;
    for (int i = start; i < end; i++) {
      List<?> pair = nativeSequence(pairs.get(i));
      if (pair == null || pair.size() != keyIndex + 2) {
        return Starlark.NONE;
      }
      Tuple entry = entry(pair.get(keyIndex), pair.get(keyIndex + 1), caseInsensitive, thread);
      if (entry == null) {
        return Starlark.NONE;
      }
      entries[i - start] = entry;
    }
    return StarlarkList.of(thread.mutability(), entries);
  }

  private static List<?> nativeSequence(Object value) {
    return value instanceof Tuple || value instanceof StarlarkList<?> ? (List<?>) value : null;
  }

  private static Tuple entry(Object key, Object value, boolean caseInsensitive, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (!(key instanceof String text)) {
      return null;
    }
    String identity = caseInsensitive ? asciiTitle(text) : text;
    if (identity == null) {
      // Title casing can traverse a long string, so check before each delegated call.
      charge(thread, 0);
      Object title = Starlark.getattr(thread.mutability(), thread.getSemantics(), text, "title", null);
      identity = (String) Starlark.call(thread, title, Tuple.empty(), Dict.empty());
    }
    return Tuple.of(identity, text, value);
  }

  /** ASCII-only optimization; other strings call the existing Starlark title builtin. */
  private static String asciiTitle(String text) {
    if (text.length() > MAX_KEY_LENGTH) {
      return null;
    }
    char[] changed = null;
    boolean inWord = false;
    for (int i = 0; i < text.length(); i++) {
      char original = text.charAt(i);
      if (original > 127) {
        return null;
      }
      boolean upper = original >= 'A' && original <= 'Z';
      boolean lower = original >= 'a' && original <= 'z';
      char titled = original;
      if (inWord) {
        if (upper) {
          titled = (char) (original + ('a' - 'A'));
        }
      } else if (lower) {
        titled = (char) (original - ('a' - 'A'));
      }
      if (titled != original) {
        if (changed == null) {
          changed = text.toCharArray();
        }
        changed[i] = titled;
      }
      inWord = upper || lower;
    }
    return changed == null ? text : new String(changed);
  }

  @StarlarkMethod(
      name = "snapshot",
      documented = false,
      doc = "Copy the ordered items, keys, or values to a list.",
      parameters = {@Param(name = "source"), @Param(name = "view"),
                    @Param(name = "start", defaultValue = "-1")},
      useStarlarkThread = true)
  public Object snapshot(Object source, String view, StarlarkInt offset, StarlarkThread thread)
      throws EvalException {
    int column = switch (view) {
      case "items" -> 0;
      case "keys" -> 1;
      case "values" -> 2;
      default -> -1;
    };
    if (!(source instanceof StarlarkList<?> entries) || column < 0) {
      return Starlark.NONE;
    }
    int start = chunkStart(offset, entries.size());
    if (start < 0) {
      return Starlark.NONE;
    }
    int end = chunkEnd(entries, start, thread);
    var result = new Object[end - start];
    for (int i = start; i < end; i++) {
      Object item = entries.get(i);
      if (!(item instanceof Tuple tuple) || tuple.size() != 3) {
        return Starlark.NONE;
      }
      result[i - start] = column == 0 ? Tuple.of(tuple.get(1), tuple.get(2)) : tuple.get(column);
    }
    return StarlarkList.of(thread.mutability(), result);
  }

  @StarlarkMethod(
      name = "find",
      documented = false,
      doc = "Find the first matching identity, returning -1 on a miss or -2 for fallback.",
      parameters = {@Param(name = "source"), @Param(name = "identity"),
                    @Param(name = "start", defaultValue = "-1")},
      useStarlarkThread = true)
  public StarlarkInt find(Object source, Object identity, StarlarkInt offset, StarlarkThread thread)
      throws EvalException {
    if (!(source instanceof StarlarkList<?> entries)
        || !(identity instanceof String key) || key.length() > MAX_KEY_LENGTH) {
      return UNSUPPORTED;
    }
    int start = chunkStart(offset, entries.size());
    if (start < 0) {
      return UNSUPPORTED;
    }
    int end = chunkEnd(entries, start, thread);
    for (int i = start; i < end; i++) {
      String alias = stringIdentity(entries.get(i));
      if (alias == null) {
        return UNSUPPORTED;
      }
      if (key.equals(alias)) {
        return StarlarkInt.of(i);
      }
    }
    return MISSING;
  }

  @StarlarkMethod(
      name = "matching_values",
      documented = false,
      doc = "Copy the values for a native string identity in insertion order.",
      parameters = {@Param(name = "source"), @Param(name = "identity"),
                    @Param(name = "start", defaultValue = "-1")},
      useStarlarkThread = true)
  public Object matchingValues(Object source, Object identity, StarlarkInt offset,
      StarlarkThread thread) throws EvalException {
    if (!(source instanceof StarlarkList<?> entries)
        || !(identity instanceof String key) || key.length() > MAX_KEY_LENGTH) {
      return Starlark.NONE;
    }
    int start = chunkStart(offset, entries.size());
    if (start < 0) {
      return Starlark.NONE;
    }
    int end = chunkEnd(entries, start, thread);
    var result = new ArrayList<Object>();
    for (int i = start; i < end; i++) {
      Object item = entries.get(i);
      String alias = stringIdentity(item);
      if (alias == null) {
        return Starlark.NONE;
      }
      if (key.equals(alias)) {
        result.add(((Tuple) item).get(2));
      }
    }
    return StarlarkList.copyOf(thread.mutability(), result);
  }

  private static int chunkStart(StarlarkInt offset, int size) throws EvalException {
    int start = offset.toInt("start");
    return start == -1 && size <= MAX_ENTRIES ? 0 : start <= size ? start : -1;
  }

  private static int chunkEnd(List<?> entries, int start, StarlarkThread thread) {
    int count = Math.min(MAX_ENTRIES, entries.size() - start);
    if (entries.size() > MAX_ENTRIES) {
      thread.incrementExecutedSteps(count);
    }
    return start + count;
  }

  @StarlarkMethod(
      name = "same_object",
      documented = false,
      doc = "Check whether two values share the same object.",
      parameters = {@Param(name = "left"), @Param(name = "right")})
  public boolean sameObject(Object left, Object right) {
    return left == right;
  }

  @StarlarkMethod(
      name = "can_append",
      documented = false,
      doc = "Check whether the native append operation can handle the backing values.",
      parameters = {@Param(name = "target"), @Param(name = "source"), @Param(name = "version")})
  public boolean canAppend(Object target, Object source, Object version) {
    return target instanceof StarlarkList<?> && target != source && version instanceof StarlarkInt;
  }

  @StarlarkMethod(
      name = "append_chunk",
      documented = false,
      doc = "Append one prepared chunk and advance the backing version.",
      parameters = {@Param(name = "impl"), @Param(name = "source"), @Param(name = "start")},
      useStarlarkThread = true)
  public void appendChunk(SimpleStruct impl, StarlarkList<?> source, StarlarkInt offset,
      StarlarkThread thread) throws EvalException {
    Object target = impl.getField("_items", thread);
    Object rawVersion = impl.getField("_version", thread);
    int start = offset.toInt("start");
    if (!(target instanceof StarlarkList<?> items) || !(rawVersion instanceof StarlarkInt version)
        || target == source || start < 0 || start > source.size()) {
      throw Starlark.errorf("invalid multidict backing state");
    }
    int end = chunkEnd(source, start, thread);
    var chunk = new ArrayList<Object>(end - start);
    for (int i = start; i < end; i++) {
      chunk.add(source.get(i));
    }
    appendItems(items, chunk);
    impl.setField("_version", StarlarkInt.add(version, StarlarkInt.of(end - start)));
  }

  @SuppressWarnings("unchecked")
  private static void appendItems(StarlarkList<?> target, List<?> source) throws EvalException {
    ((StarlarkList<Object>) target).addElements(source);
  }

  @StarlarkMethod(
      name = "add",
      documented = false,
      doc = "Append an entry with a native string key.",
      parameters = {@Param(name = "impl"), @Param(name = "identity"),
                    @Param(name = "key"), @Param(name = "value")},
      useStarlarkThread = true)
  public boolean add(SimpleStruct impl, Object identity, Object key, Object value,
      StarlarkThread thread) throws EvalException {
    Backing backing = backing(impl, thread);
    if (backing == null || !(identity instanceof String) || !(key instanceof String)) {
      return false;
    }
    backing.entries().addElement(Tuple.of(identity, key, value));
    backing.bump(1);
    return true;
  }

  @StarlarkMethod(
      name = "clear",
      documented = false,
      doc = "Clear the native backing and advance the version.",
      parameters = {@Param(name = "impl")},
      useStarlarkThread = true)
  public boolean clear(SimpleStruct impl, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Backing backing = backing(impl, thread);
    if (backing == null) {
      return false;
    }
    if (backing.entries().size() > MAX_ENTRIES) {
      charge(thread, backing.entries().size());
    }
    backing.entries().clearElements();
    backing.bump(1);
    return true;
  }

  @StarlarkMethod(
      name = "replace",
      documented = false,
      doc = "Replace the first matching entry and remove its later duplicates.",
      parameters = {@Param(name = "impl"), @Param(name = "identity"),
                    @Param(name = "key"), @Param(name = "value")},
      useStarlarkThread = true)
  public boolean replace(SimpleStruct impl, Object identity, Object key, Object value,
      StarlarkThread thread) throws EvalException, InterruptedException {
    Backing backing = backing(impl, thread);
    if (backing == null || !shortString(identity)) {
      return false;
    }
    StarlarkList<Object> entries = backing.entries();
    Work work = new Work(entries.size() > MAX_ENTRIES);
    if (!stringEntries(entries, work, thread)) {
      return false;
    }
    int first = findIndex(entries, (String) identity, 0, work, thread);
    work.finish(thread);
    Tuple replacement = Tuple.of(identity, key, value);
    if (first == entries.size()) {
      entries.addElement(replacement);
      backing.bump(1);
      return true;
    }
    entries.setElementAt(first, replacement);
    backing.bump(1);
    for (int i = first + 1; i < entries.size();) {
      work.step(thread);
      if (identity.equals(stringIdentity(entries.get(i)))) {
        entries.removeElementAt(i);
      } else {
        i++;
      }
    }
    work.finish(thread);
    return true;
  }

  @StarlarkMethod(
      name = "update",
      documented = false,
      doc = "Apply the ordered update to native string entries.",
      parameters = {@Param(name = "impl"), @Param(name = "source")},
      useStarlarkThread = true)
  public boolean update(SimpleStruct impl, Object source, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Backing backing = backing(impl, thread);
    List<?> incoming = nativeSequence(source);
    if (backing == null || incoming == null || backing.entries() == source) {
      return false;
    }
    if (incoming.isEmpty()) {
      return true;
    }
    StarlarkList<Object> entries = backing.entries();
    Work work = new Work(entries.size() > MAX_ENTRIES || incoming.size() > MAX_ENTRIES);
    if (!stringEntries(entries, work, thread) || !stringEntries(incoming, work, thread)) {
      return false;
    }
    var usedKeys = new HashMap<String, Integer>();
    boolean changed = false;
    for (Object item : incoming) {
      work.step(thread);
      String key = stringIdentity(item);
      int index = findIndex(entries, key, usedKeys.getOrDefault(key, 0), work, thread);
      if (index == entries.size()) {
        entries.addElement(item);
      } else {
        entries.setElementAt(index, item);
      }
      if (!changed) {
        // Invalidate iterators even if a later checkpoint interrupts the update.
        backing.bump(1);
        changed = true;
      }
      usedKeys.put(key, index + 1);
    }
    // Preserve the facade's original index thresholds while removing tails.
    for (int i = 0; i < entries.size();) {
      work.step(thread);
      Integer threshold = usedKeys.get(stringIdentity(entries.get(i)));
      if (threshold != null && i >= threshold) {
        entries.removeElementAt(i);
      } else {
        i++;
      }
    }
    work.finish(thread);
    return true;
  }

  @StarlarkMethod(
      name = "pop_one",
      documented = false,
      doc = "Pop the first matching entry and return a one-element tuple.",
      parameters = {@Param(name = "impl"), @Param(name = "identity")},
      useStarlarkThread = true)
  public Object popOne(SimpleStruct impl, Object identity, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Backing backing = backing(impl, thread);
    if (backing == null || !shortString(identity)) {
      return Starlark.NONE;
    }
    StarlarkList<Object> entries = backing.entries();
    Work work = new Work(entries.size() > MAX_ENTRIES);
    if (!stringEntries(entries, work, thread)) {
      return Starlark.NONE;
    }
    int index = findIndex(entries, (String) identity, 0, work, thread);
    work.finish(thread);
    if (index == entries.size()) {
      return Tuple.empty();
    }
    Object value = ((Tuple) entries.get(index)).get(2);
    entries.removeElementAt(index);
    backing.bump(1);
    return Tuple.of(value);
  }

  @StarlarkMethod(
      name = "remove_all",
      documented = false,
      doc = "Remove matches from the right, optionally collecting the removed values.",
      parameters = {@Param(name = "impl"), @Param(name = "identity"), @Param(name = "collect")},
      useStarlarkThread = true)
  public Object removeAll(SimpleStruct impl, Object identity, boolean collect, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Backing backing = backing(impl, thread);
    if (backing == null || !shortString(identity)) {
      return Starlark.NONE;
    }
    StarlarkList<Object> entries = backing.entries();
    Work work = new Work(entries.size() > MAX_ENTRIES);
    if (!stringEntries(entries, work, thread)) {
      return Starlark.NONE;
    }
    var values = collect ? new ArrayList<Object>() : null;
    int removed = 0;
    for (int i = entries.size() - 1; i >= 0; i--) {
      work.step(thread);
      Tuple entry = (Tuple) entries.get(i);
      if (identity.equals(entry.get(0))) {
        entries.removeElementAt(i);
        removed++;
        if (values != null) {
          values.add(entry.get(2));
          backing.bump(removed);
        } else if (removed == 1) {
          backing.bump(1);
        }
      }
    }
    work.finish(thread);
    if (values != null) {
      return StarlarkList.copyOf(thread.mutability(), values);
    }
    return removed > 0;
  }

  @StarlarkMethod(
      name = "pop_item",
      documented = false,
      doc = "Pop the first pair from a native backing.",
      parameters = {@Param(name = "impl")},
      useStarlarkThread = true)
  public Object popItem(SimpleStruct impl, StarlarkThread thread) throws EvalException {
    Backing backing = backing(impl, thread);
    if (backing == null || backing.entries().isEmpty()
        || !(backing.entries().getFirst() instanceof Tuple tuple) || tuple.size() != 3) {
      return Starlark.NONE;
    }
    backing.entries().removeElementAt(0);
    backing.bump(1);
    return Tuple.of(tuple.get(1), tuple.get(2));
  }

  private record Backing(SimpleStruct impl, StarlarkList<Object> entries, StarlarkInt version) {
    void bump(int amount) throws EvalException {
      impl.setField("_version", StarlarkInt.add(version, StarlarkInt.of(amount)));
    }
  }

  @SuppressWarnings("unchecked")
  private static Backing backing(SimpleStruct impl, StarlarkThread thread) {
    Object target = impl.getField("_items", thread);
    Object version = impl.getField("_version", thread);
    if (target instanceof StarlarkList<?> entries && version instanceof StarlarkInt count) {
      return new Backing(impl, (StarlarkList<Object>) entries, count);
    }
    return null;
  }

  private static boolean shortString(Object value) {
    return value instanceof String s && s.length() <= MAX_KEY_LENGTH;
  }

  private static boolean stringEntries(List<?> entries, Work work, StarlarkThread thread)
      throws EvalException, InterruptedException {
    for (int i = 0; i < entries.size(); i++) {
      work.step(thread);
      if (!shortString(stringIdentity(entries.get(i)))) {
        work.finish(thread);
        return false;
      }
    }
    return true;
  }

  private static int findIndex(List<?> entries, String key, int start, Work work,
      StarlarkThread thread)
      throws EvalException, InterruptedException {
    for (int i = start; i < entries.size(); i++) {
      work.step(thread);
      if (key.equals(stringIdentity(entries.get(i)))) {
        return i;
      }
    }
    return entries.size();
  }

  /** Keep large loops interruptible without a clock read on each entry. */
  private static final class Work {
    private final boolean metered;
    private int pending;

    Work(boolean metered) {
      this.metered = metered;
    }

    void step(StarlarkThread thread) throws EvalException, InterruptedException {
      if (metered && ++pending == MAX_ENTRIES) {
        finish(thread);
      }
    }

    void finish(StarlarkThread thread) throws EvalException, InterruptedException {
      if (pending != 0) {
        charge(thread, pending);
        pending = 0;
      }
    }
  }

  private static void charge(StarlarkThread thread, int steps)
      throws EvalException, InterruptedException {
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedException();
    }
    if (thread.getExpirationMs() != Long.MAX_VALUE && thread.isExpired()) {
      throw Starlark.errorf("Starlark computation cancelled: past expiration date");
    }
    if (steps >= thread.getMaxExecutionSteps() - thread.getExecutedSteps()) {
      throw Starlark.errorf("Starlark computation cancelled: too many steps");
    }
    thread.incrementExecutedSteps(steps);
  }

  private static String stringIdentity(Object item) {
    return item instanceof Tuple tuple && tuple.size() == 3 && tuple.get(0) instanceof String key
        ? key : null;
  }
}
