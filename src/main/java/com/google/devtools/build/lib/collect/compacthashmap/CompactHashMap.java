// Copyright 2019 The Bazel Authors. All rights reserved.
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
/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.build.lib.collect.compacthashmap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * CompactHashMap is an implementation of a Map. All optional operations (put and remove) are
 * supported. Null keys and values are supported.
 *
 * <p>{@code containsKey(k)}, {@code put(k, v)} and {@code remove(k)} are all (expected and
 * amortized) constant time operations. Expected in the hashtable sense (depends on the hash
 * function doing a good job of distributing the elements to the buckets to a distribution not far
 * from uniform), and amortized since some operations can trigger a hash table resize.
 *
 * <p>Unlike {@code java.util.HashMap}, iteration is only proportional to the actual {@code size()},
 * which is optimal, and <i>not</i> the size of the internal hashtable, which could be much larger
 * than {@code size()}. Furthermore, this structure places significantly reduced load on the garbage
 * collector by only using a constant number of internal objects.
 *
 * <p>If there are no removals, then iteration order for the {@link #entrySet}, {@link #keySet}, and
 * {@link #values} views is the same as insertion order. Any removal invalidates any ordering
 * guarantees.
 *
 * <p>This class should not be assumed to be universally superior to {@code java.util.HashMap}.
 * Generally speaking, this class reduces object allocation and memory consumption at the price of
 * moderately increased constant factors of CPU. Only use this class when there is a specific reason
 * to prioritize memory over CPU.
 *
 * @author Louis Wasserman
 */
public class CompactHashMap<K, V> extends AbstractMap<K, V> implements Serializable {
  // A partial copy of com.google.common.collect.Hashing.
  private static final int C1 = 0xcc9e2d51;
  private static final int C2 = 0x1b873593;

  /*
   * This method was rewritten in Java from an intermediate step of the Murmur hash function in
   * http://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp, which contained the
   * following header:
   *
   * MurmurHash3 was written by Austin Appleby, and is placed in the public domain. The author
   * hereby disclaims copyright to this source code.
   */
  private static int smear(int hashCode) {
    return C2 * Integer.rotateLeft(hashCode * C1, 15);
  }

  private static int smearedHash(@Nullable Object o) {
    return smear((o == null) ? 0 : o.hashCode());
  }

  private static final int MAX_TABLE_SIZE = Ints.MAX_POWER_OF_TWO;

  private static int closedTableSize(int expectedEntries, double loadFactor) {
    // Get the recommended table size.
    // Round down to the nearest power of 2.
    expectedEntries = Math.max(expectedEntries, 2);
    int tableSize = Integer.highestOneBit(expectedEntries);
    // Check to make sure that we will not exceed the maximum load factor.
    if (expectedEntries > (int) (loadFactor * tableSize)) {
      tableSize <<= 1;
      return (tableSize > 0) ? tableSize : MAX_TABLE_SIZE;
    }
    return tableSize;
  }

  static boolean needsResizing(int size, int tableSize, double loadFactor) {
    return size > loadFactor * tableSize && tableSize < MAX_TABLE_SIZE;
  }

  /*
   * TODO: Make this a drop-in replacement for j.u. versions, actually drop them in, and test the
   * world. Figure out what sort of space-time tradeoff we're actually going to get here with the
   * *Map variants. Followon optimizations, such as using 16-bit indices for small collections, will
   * take more work to implement. This class is particularly hard to benchmark, because the benefit
   * is not only in less allocation, but also having the GC do less work to scan the heap because of
   * fewer references, which is particularly hard to quantify.
   */

  /** Creates an empty {@code CompactHashMap} instance. */
  public static <K, V> CompactHashMap<K, V> create() {
    return new CompactHashMap<>();
  }

  /**
   * Creates a {@code CompactHashMap} instance, with a high enough "initial capacity" that it
   * <i>should</i> hold {@code expectedSize} elements without growth.
   *
   * @param expectedSize the number of elements you expect to add to the returned set
   * @return a new, empty {@code CompactHashMap} with enough capacity to hold {@code expectedSize}
   *     elements without resizing
   * @throws IllegalArgumentException if {@code expectedSize} is negative
   */
  public static <K, V> CompactHashMap<K, V> createWithExpectedSize(int expectedSize) {
    return new CompactHashMap<>(expectedSize);
  }

  private static final float LOAD_FACTOR = 1.0f;

  /** Bitmask that selects the low 32 bits. */
  private static final long NEXT_MASK = (1L << 32) - 1;

  /** Bitmask that selects the high 32 bits. */
  private static final long HASH_MASK = ~NEXT_MASK;

  // TODO(bazel-team): decide default size
  static final int DEFAULT_SIZE = 3;

  // used to indicate blank table entries
  static final int UNSET = -1;

  /**
   * The hashtable. Its values are indexes to the keys, values, and entries arrays.
   *
   * <p>Currently, the UNSET value means "null pointer", and any non negative value x is the actual
   * index.
   *
   * <p>Its size must be a power of two.
   */
  private transient int[] table;

  /**
   * Contains the logical entries, in the range of [0, size()). The high 32 bits of each long is the
   * smeared hash of the element, whereas the low 32 bits is the "next" pointer (pointing to the
   * next entry in the bucket chain). The pointers in [size(), entries.length) are all "null"
   * (UNSET).
   */
  @VisibleForTesting transient long[] entries;

  /**
   * The keys of the entries in the map, in the range of [0, size()). The keys in [size(),
   * keys.length) are all {@code null}.
   */
  @VisibleForTesting transient Object[] keys;

  /**
   * The values of the entries in the map, in the range of [0, size()). The values in [size(),
   * values.length) are all {@code null}.
   */
  @VisibleForTesting transient Object[] values;

  /**
   * Keeps track of modifications of this set, to make it possible to throw
   * ConcurrentModificationException in the iterator. Note that we choose not to make this volatile,
   * so we do less of a "best effort" to track such errors, for better performance.
   */
  transient int modCount;

  /** The number of elements contained in the set. */
  private transient int size;

  /** Constructs a new empty instance of {@code CompactHashMap}. */
  CompactHashMap() {
    init(DEFAULT_SIZE);
  }

  /**
   * Constructs a new instance of {@code CompactHashMap} with the specified capacity.
   *
   * @param expectedSize the initial capacity of this {@code CompactHashMap}.
   */
  CompactHashMap(int expectedSize) {
    init(expectedSize);
  }

  /** Pseudoconstructor for serialization support. */
  void init(int expectedSize) {
    Preconditions.checkArgument(expectedSize >= 0, "Expected size must be non-negative");
    this.modCount = Math.max(1, expectedSize); // Save expectedSize for use in allocArrays()
  }

  /** Returns whether arrays need to be allocated. */
  boolean needsAllocArrays() {
    return table == null;
  }

  /** Handle lazy allocation of arrays. */
  void allocArrays() {
    checkState(needsAllocArrays(), "Arrays already allocated");

    int expectedSize = modCount;
    int buckets = closedTableSize(expectedSize, LOAD_FACTOR);
    this.table = newTable(buckets);

    this.entries = newEntries(expectedSize);
    this.keys = new Object[expectedSize];
    this.values = new Object[expectedSize];
  }

  private static int[] newTable(int size) {
    int[] array = new int[size];
    Arrays.fill(array, UNSET);
    return array;
  }

  private static long[] newEntries(int size) {
    long[] array = new long[size];
    Arrays.fill(array, UNSET);
    return array;
  }

  private int hashTableMask() {
    return table.length - 1;
  }

  private static int getHash(long entry) {
    return (int) (entry >>> 32);
  }

  /** Returns the index, or UNSET if the pointer is "null" */
  private static int getNext(long entry) {
    return (int) entry;
  }

  /** Returns a new entry value by changing the "next" index of an existing entry */
  private static long swapNext(long entry, int newNext) {
    return (HASH_MASK & entry) | (NEXT_MASK & newNext);
  }

  /**
   * Mark an access of the specified entry. Used only in {@code CompactLinkedHashMap} for LRU
   * ordering.
   */
  void accessEntry(int index) {
    // no-op by default
  }

  @CanIgnoreReturnValue
  @Override
  public @Nullable V put(@Nullable K key, @Nullable V value) {
    if (needsAllocArrays()) {
      allocArrays();
    }
    long[] entries = this.entries;
    Object[] keys = this.keys;
    Object[] values = this.values;

    int hash = smearedHash(key);
    int tableIndex = hash & hashTableMask();
    int newEntryIndex = this.size; // current size, and pointer to the entry to be appended
    int next = table[tableIndex];
    if (next == UNSET) { // uninitialized bucket
      table[tableIndex] = newEntryIndex;
    } else {
      int last;
      long entry;
      do {
        last = next;
        entry = entries[next];
        if (getHash(entry) == hash && Objects.equal(key, keys[next])) {
          @SuppressWarnings("unchecked") // known to be a V
          @Nullable
          V oldValue = (V) values[next];

          values[next] = value;
          accessEntry(next);
          return oldValue;
        }
        next = getNext(entry);
      } while (next != UNSET);
      entries[last] = swapNext(entry, newEntryIndex);
    }
    if (newEntryIndex == Integer.MAX_VALUE) {
      throw new IllegalStateException("Cannot contain more than Integer.MAX_VALUE elements!");
    }
    int newSize = newEntryIndex + 1;
    resizeMeMaybe(newSize);
    insertEntry(newEntryIndex, key, value, hash);
    this.size = newSize;
    int oldCapacity = table.length;
    if (needsResizing(newEntryIndex, oldCapacity, LOAD_FACTOR)) {
      resizeTable(2 * oldCapacity);
    }
    modCount++;
    return null;
  }

  /**
   * Creates a fresh entry with the specified object at the specified position in the entry arrays.
   */
  void insertEntry(int entryIndex, @Nullable K key, @Nullable V value, int hash) {
    this.entries[entryIndex] = ((long) hash << 32) | (NEXT_MASK & UNSET);
    this.keys[entryIndex] = key;
    this.values[entryIndex] = value;
  }

  /** Resizes the entries storage if necessary. */
  private void resizeMeMaybe(int newSize) {
    int entriesSize = entries.length;
    if (newSize > entriesSize) {
      int newCapacity = entriesSize + Math.max(1, entriesSize >>> 1);
      if (newCapacity < 0) {
        newCapacity = Integer.MAX_VALUE;
      }
      if (newCapacity != entriesSize) {
        resizeEntries(newCapacity);
      }
    }
  }

  /**
   * Resizes the internal entries array to the specified capacity, which may be greater or less than
   * the current capacity.
   */
  void resizeEntries(int newCapacity) {
    this.keys = Arrays.copyOf(keys, newCapacity);
    this.values = Arrays.copyOf(values, newCapacity);
    long[] entries = this.entries;
    int oldCapacity = entries.length;
    entries = Arrays.copyOf(entries, newCapacity);
    if (newCapacity > oldCapacity) {
      Arrays.fill(entries, oldCapacity, newCapacity, UNSET);
    }
    this.entries = entries;
  }

  private void resizeTable(int newCapacity) { // newCapacity always a power of two
    int[] newTable = newTable(newCapacity);
    long[] entries = this.entries;

    int mask = newTable.length - 1;
    for (int i = 0; i < size; i++) {
      long oldEntry = entries[i];
      int hash = getHash(oldEntry);
      int tableIndex = hash & mask;
      int next = newTable[tableIndex];
      newTable[tableIndex] = i;
      entries[i] = ((long) hash << 32) | (NEXT_MASK & next);
    }

    this.table = newTable;
  }

  private int indexOf(@Nullable Object key) {
    if (needsAllocArrays()) {
      return -1;
    }
    int hash = smearedHash(key);
    int next = table[hash & hashTableMask()];
    while (next != UNSET) {
      long entry = entries[next];
      if (getHash(entry) == hash && Objects.equal(key, keys[next])) {
        return next;
      }
      next = getNext(entry);
    }
    return -1;
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    return indexOf(key) != -1;
  }

  @SuppressWarnings("unchecked") // values only contains Vs
  @Override
  public V get(@Nullable Object key) {
    int index = indexOf(key);
    accessEntry(index);
    return (index == -1) ? null : (V) values[index];
  }

  @CanIgnoreReturnValue
  @Override
  public @Nullable V remove(@Nullable Object key) {
    if (needsAllocArrays()) {
      return null;
    }
    return remove(key, smearedHash(key));
  }

  private @Nullable V remove(@Nullable Object key, int hash) {
    int tableIndex = hash & hashTableMask();
    int next = table[tableIndex];
    if (next == UNSET) { // empty bucket
      return null;
    }
    int last = UNSET;
    do {
      if (getHash(entries[next]) == hash && Objects.equal(key, keys[next])) {
        @SuppressWarnings("unchecked") // values only contains Vs
        @Nullable
        V oldValue = (V) values[next];

        if (last == UNSET) {
          // we need to update the root link from table[]
          table[tableIndex] = getNext(entries[next]);
        } else {
          // we need to update the link from the chain
          entries[last] = swapNext(entries[last], getNext(entries[next]));
        }

        moveLastEntry(next);
        size--;
        modCount++;
        return oldValue;
      }
      last = next;
      next = getNext(entries[next]);
    } while (next != UNSET);
    return null;
  }

  @CanIgnoreReturnValue
  private V removeEntry(int entryIndex) {
    return remove(keys[entryIndex], getHash(entries[entryIndex]));
  }

  /**
   * Moves the last entry in the entry array into {@code dstIndex}, and nulls out its old position.
   */
  void moveLastEntry(int dstIndex) {
    int srcIndex = size() - 1;
    if (dstIndex < srcIndex) {
      // move last entry to deleted spot
      keys[dstIndex] = keys[srcIndex];
      values[dstIndex] = values[srcIndex];
      keys[srcIndex] = null;
      values[srcIndex] = null;

      // move the last entry to the removed spot, just like we moved the element
      long lastEntry = entries[srcIndex];
      entries[dstIndex] = lastEntry;
      entries[srcIndex] = UNSET;

      // also need to update whoever's "next" pointer was pointing to the last entry place
      // reusing "tableIndex" and "next"; these variables were no longer needed
      int tableIndex = getHash(lastEntry) & hashTableMask();
      int lastNext = table[tableIndex];
      if (lastNext == srcIndex) {
        // we need to update the root pointer
        table[tableIndex] = dstIndex;
      } else {
        // we need to update a pointer in an entry
        int previous;
        long entry;
        do {
          previous = lastNext;
          lastNext = getNext(entry = entries[lastNext]);
        } while (lastNext != srcIndex);
        // here, entries[previous] points to the old entry location; update it
        entries[previous] = swapNext(entry, dstIndex);
      }
    } else {
      keys[dstIndex] = null;
      values[dstIndex] = null;
      entries[dstIndex] = UNSET;
    }
  }

  int firstEntryIndex() {
    return isEmpty() ? -1 : 0;
  }

  int getSuccessor(int entryIndex) {
    return (entryIndex + 1 < size) ? entryIndex + 1 : -1;
  }

  /**
   * Updates the index an iterator is pointing to after a call to remove: returns the index of the
   * entry that should be looked at after a removal on indexRemoved, with indexBeforeRemove as the
   * index that *was* the next entry that would be looked at.
   */
  int adjustAfterRemove(int indexBeforeRemove, @SuppressWarnings("unused") int indexRemoved) {
    return indexBeforeRemove - 1;
  }

  private abstract class Itr<T> implements Iterator<T> {
    int expectedModCount = modCount;
    int currentIndex = firstEntryIndex();
    int indexToRemove = -1;

    @Override
    public boolean hasNext() {
      return currentIndex >= 0;
    }

    abstract T getOutput(int entry);

    @Override
    public T next() {
      checkForConcurrentModification();
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      indexToRemove = currentIndex;
      T result = getOutput(currentIndex);
      currentIndex = getSuccessor(currentIndex);
      return result;
    }

    @Override
    public void remove() {
      checkForConcurrentModification();
      checkState(indexToRemove >= 0, "no calls to next() since the last call to remove()");
      expectedModCount++;
      removeEntry(indexToRemove);
      currentIndex = adjustAfterRemove(currentIndex, indexToRemove);
      indexToRemove = -1;
    }

    private void checkForConcurrentModification() {
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked") // keys/values only contains Ks/Vs
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    checkNotNull(function);
    for (int i = 0; i < size; i++) {
      values[i] = function.apply((K) keys[i], (V) values[i]);
    }
  }

  private transient Set<K> keySetView;

  @Override
  public Set<K> keySet() {
    return (keySetView == null) ? keySetView = createKeySet() : keySetView;
  }

  Set<K> createKeySet() {
    return new KeySetView();
  }

  class KeySetView extends AbstractSet<K> {
    KeySetView() {}

    @Override
    public int size() {
      return size;
    }

    @Override
    public Object[] toArray() {
      if (needsAllocArrays()) {
        return new Object[0];
      }
      return Arrays.copyOf(keys, size);
    }

    @Override
    public boolean remove(@Nullable Object o) {
      int index = indexOf(o);
      if (index == -1) {
        return false;
      } else {
        removeEntry(index);
        return true;
      }
    }

    @Override
    public Iterator<K> iterator() {
      return keySetIterator();
    }

    @Override
    public Spliterator<K> spliterator() {
      if (needsAllocArrays()) {
        return Spliterators.spliterator(new Object[0], Spliterator.DISTINCT | Spliterator.ORDERED);
      }
      return Spliterators.spliterator(keys, 0, size, Spliterator.DISTINCT | Spliterator.ORDERED);
    }

    @Override
    @SuppressWarnings("unchecked") // keys contains only Ks
    public void forEach(Consumer<? super K> action) {
      checkNotNull(action);
      for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
        action.accept((K) keys[i]); // unchecked
      }
    }
  }

  Iterator<K> keySetIterator() {
    return new Itr<K>() {
      @SuppressWarnings("unchecked") // keys only contains Ks
      @Override
      K getOutput(int entry) {
        return (K) keys[entry];
      }
    };
  }

  @Override
  @SuppressWarnings("unchecked") // keys/values contains only Ks/Vs
  public void forEach(BiConsumer<? super K, ? super V> action) {
    checkNotNull(action);
    for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
      action.accept((K) keys[i], (V) values[i]);
    }
  }

  private transient Set<Entry<K, V>> entrySetView;

  @Override
  public Set<Entry<K, V>> entrySet() {
    return (entrySetView == null) ? entrySetView = createEntrySet() : entrySetView;
  }

  Set<Entry<K, V>> createEntrySet() {
    return new EntrySetView();
  }

  class EntrySetView extends AbstractSet<Entry<K, V>> {
    @Override
    public int size() {
      return size;
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return entrySetIterator();
    }

    @Override
    public boolean contains(@Nullable Object o) {
      if (o instanceof Entry) {
        Entry<?, ?> entry = (Entry<?, ?>) o;
        int index = indexOf(entry.getKey());
        return index != -1 && Objects.equal(values[index], entry.getValue());
      }
      return false;
    }

    @Override
    public boolean remove(@Nullable Object o) {
      if (o instanceof Entry) {
        Entry<?, ?> entry = (Entry<?, ?>) o;
        int index = indexOf(entry.getKey());
        if (index != -1 && Objects.equal(values[index], entry.getValue())) {
          removeEntry(index);
          return true;
        }
      }
      return false;
    }
  }

  Iterator<Entry<K, V>> entrySetIterator() {
    return new Itr<Entry<K, V>>() {
      @Override
      Entry<K, V> getOutput(int entry) {
        return new MapEntry(entry);
      }
    };
  }

  final class MapEntry implements Map.Entry<K, V> {
    private final @Nullable K key;

    private int lastKnownIndex;

    @SuppressWarnings("unchecked") // keys only contains Ks
    MapEntry(int index) {
      this.key = (K) keys[index];
      this.lastKnownIndex = index;
    }

    @Override
    public K getKey() {
      return key;
    }

    private void updateLastKnownIndex() {
      if (lastKnownIndex == -1
          || lastKnownIndex >= size()
          || !Objects.equal(key, keys[lastKnownIndex])) {
        lastKnownIndex = indexOf(key);
      }
    }

    @SuppressWarnings("unchecked") // values only contains Vs
    @Override
    public V getValue() {
      updateLastKnownIndex();
      return (lastKnownIndex == -1) ? null : (V) values[lastKnownIndex];
    }

    @SuppressWarnings("unchecked") // values only contains Vs
    @Override
    public V setValue(V value) {
      updateLastKnownIndex();
      if (lastKnownIndex == -1) {
        put(key, value);
        return null;
      } else {
        V old = (V) values[lastKnownIndex];
        values[lastKnownIndex] = value;
        return old;
      }
    }

    @Override
    public boolean equals(@Nullable Object object) {
      if (object instanceof Entry) {
        Entry<?, ?> that = (Entry<?, ?>) object;
        return Objects.equal(this.getKey(), that.getKey())
            && Objects.equal(this.getValue(), that.getValue());
      }
      return false;
    }

    @Override
    public int hashCode() {
      K k = getKey();
      V v = getValue();
      return ((k == null) ? 0 : k.hashCode()) ^ ((v == null) ? 0 : v.hashCode());
    }

    /** Returns a string representation of the form {@code {key}={value}}. */
    @Override
    public String toString() {
      return getKey() + "=" + getValue();
    }
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    for (int i = 0; i < size; i++) {
      if (Objects.equal(value, values[i])) {
        return true;
      }
    }
    return false;
  }

  private transient Collection<V> valuesView;

  @Override
  public Collection<V> values() {
    return (valuesView == null) ? valuesView = createValues() : valuesView;
  }

  Collection<V> createValues() {
    return new ValuesView();
  }

  class ValuesView extends AbstractCollection<V> {
    ValuesView() {}

    @Override
    public int size() {
      return size;
    }

    @Override
    public Iterator<V> iterator() {
      return valuesIterator();
    }

    @Override
    @SuppressWarnings("unchecked") // values contains only Vs
    public void forEach(Consumer<? super V> action) {
      checkNotNull(action);
      for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
        action.accept((V) values[i]);
      }
    }

    @Override
    public Spliterator<V> spliterator() {
      if (needsAllocArrays()) {
        return Spliterators.spliterator(new Object[0], Spliterator.ORDERED);
      }
      return Spliterators.spliterator(values, 0, size, Spliterator.ORDERED);
    }

    @Override
    public Object[] toArray() {
      if (needsAllocArrays()) {
        return new Object[0];
      }
      return Arrays.copyOf(values, size);
    }
  }

  Iterator<V> valuesIterator() {
    return new Itr<V>() {
      @SuppressWarnings("unchecked") // values only contains Vs
      @Override
      V getOutput(int entry) {
        return (V) values[entry];
      }
    };
  }

  /**
   * Ensures that this {@code CompactHashMap} has the smallest representation in memory, given its
   * current size.
   */
  public void trimToSize() {
    if (needsAllocArrays()) {
      return;
    }
    int size = this.size;
    if (size < entries.length) {
      resizeEntries(size);
    }
    int minimumTableSize = closedTableSize(size, LOAD_FACTOR);
    if (minimumTableSize < table.length) {
      resizeTable(minimumTableSize);
    }
  }

  @Override
  public void clear() {
    if (needsAllocArrays()) {
      return;
    }
    modCount++;
    Arrays.fill(keys, 0, size, null);
    Arrays.fill(values, 0, size, null);
    Arrays.fill(table, UNSET);
    Arrays.fill(entries, 0, size, UNSET);
    this.size = 0;
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeInt(size);
    for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
      stream.writeObject(keys[i]);
      stream.writeObject(values[i]);
    }
  }

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    int elementCount = stream.readInt();
    if (elementCount < 0) {
      throw new InvalidObjectException("Invalid size: " + elementCount);
    }
    init(elementCount);
    for (int i = 0; i < elementCount; i++) {
      K key = (K) stream.readObject();
      V value = (V) stream.readObject();
      put(key, value);
    }
  }
}
