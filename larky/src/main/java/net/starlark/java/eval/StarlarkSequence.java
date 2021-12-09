package net.starlark.java.eval;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.jetbrains.annotations.NotNull;

public interface StarlarkSequence<K extends Collection<T>, T> extends StarlarkIndexable.Threaded, Sequence<T> {

  // interface
  //Collection<T> collection();
  K collection();

  @Override
  Sequence<T> getSlice(Mutability mu, int start, int stop, int step);

  @NotNull
  @Override
  Iterator<T> iterator();

  // defaults

  @Override
  default void checkHashable() throws EvalException {
    for (Object x : collection()) {
      Starlark.checkHashable(x);
    }
  }

  @Override
  default boolean isImmutable() {
    for (Object x : collection()) {
      if (!Starlark.isImmutable(x)) {
        return false;
      }
    }
    return true;
  }

  @Override
  default T getIndex(StarlarkThread starlarkThread, StarlarkSemantics semantics, Object key) throws EvalException {
    return Sequence.super.getIndex(semantics, key);
  }

  @Override
  default boolean containsKey(StarlarkThread starlarkThread, StarlarkSemantics semantics, Object key) throws EvalException {
    return Sequence.super.containsKey(semantics, key);
  }

  @Override
  default T getIndex(StarlarkSemantics semantics, Object key) throws EvalException {
    return Sequence.super.getIndex(semantics, key);
  }

  @Override
  default boolean containsKey(StarlarkSemantics semantics, Object key) throws EvalException {
    return Sequence.super.containsKey(semantics, key);
  }


  @Override
  default int size() {
    return collection().size();
  }

  @Override
  default boolean isEmpty() {
    return collection().isEmpty();
  }

  @Override
  default boolean contains(Object o) {
    return collection().contains(o);
  }

  @Override
  default Object[] toArray() {
    return collection().size() != 0
             ? collection().toArray(new Object[0])
             : new Object[0];
  }

  /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
   int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

  /**
   * Reallocates the array being used within toArray when the iterator
   * returned more elements than expected, and finishes filling it from
   * the iterator.
   *
   * @param r the array, replete with previously stored elements
   * @param it the in-progress iterator over this collection
   * @return array containing the elements in the given array, plus any
   *         further elements returned by the iterator, trimmed to size
   */
  @SuppressWarnings("unchecked")
  static <T> T[] finishToArray(T[] r, Iterator<?> it) {
      int i = r.length;
      while (it.hasNext()) {
          int cap = r.length;
          if (i == cap) {
              int newCap = cap + (cap >> 1) + 1;
              // overflow-conscious code
              if (newCap - MAX_ARRAY_SIZE > 0)
                  newCap = hugeCapacity(cap + 1);
              r = Arrays.copyOf(r, newCap);
          }
          r[i++] = (T)it.next();
      }
      // trim if overallocated
      return (i == r.length) ? r : Arrays.copyOf(r, i);
  }

  static int hugeCapacity(int minCapacity) {
      if (minCapacity < 0) // overflow
          throw new OutOfMemoryError
              ("Required array size too large");
      return (minCapacity > MAX_ARRAY_SIZE) ?
          Integer.MAX_VALUE :
          MAX_ARRAY_SIZE;
  }

  @NotNull
  @Override
  default <T1> T1 @NotNull [] toArray(@NotNull T1 @NotNull [] a) {
    //      ArrayList<Object> list = new ArrayList<>();
    //      Collections.addAll(list, positional);
    //      Iterables.addAll(list, ((Iterable<?>) value));
    //      positional = list.toArray();
    final int size = collection().size();
    @SuppressWarnings("unchecked")
    T1[] r =
      a.length >= size
        ? a
        : (T1[]) java.lang.reflect.Array
           .newInstance(a.getClass().getComponentType(), size);
    Iterator<T> it = collection().iterator();

    for (int i = 0; i < r.length; i++) {
      if (!it.hasNext()) { // fewer elements than expected
        if (a == r) {
          r[i] = null; // null-terminate
        } else if (a.length < i) {
          return Arrays.copyOf(r, i);
        } else {
          System.arraycopy(r, 0, a, 0, i);
          if (a.length > i) {
            //noinspection ConstantConditions
            a[i] = null;
          }
        }
        return a;
      }
      //noinspection unchecked
      r[i] = (T1) it.next();
    }
    // more elements than expected
    return it.hasNext() ? finishToArray(r, it) : r;
  }

  @Override
  default boolean add(T t) {
    return collection().add(t);
  }

  @Override
  default boolean remove(Object o) {
    return collection().remove(o);
  }

  @Override
  default boolean containsAll(@NotNull Collection<?> c) {
    return collection().containsAll(c);
  }

  @Override
  default boolean addAll(@NotNull Collection<? extends T> c) {
    return collection().addAll(c);
  }

  @Override
  default boolean addAll(int index, @NotNull Collection<? extends T> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  default boolean removeAll(@NotNull Collection<?> c) {
    return collection().removeAll(c);
  }

  @Override
  default boolean retainAll(@NotNull Collection<?> c) {
    return collection().retainAll(c);
  }

  @Override
  default void clear() {
    collection().clear();
  }

  @Override
  default T get(int index) {
    int i = 0;
    final Iterator<T> iterator = collection().iterator();
    T v = iterator.next();
    while(i < index) {
      i++;
      v = iterator.next();
    }
    return v;
  }

  @Override
  default T set(int index, T element) {
    throw new UnsupportedOperationException();
  }

  @Override
  default void add(int index, T element) {
    throw new UnsupportedOperationException();
  }

  @Override
  default T remove(int index) {
    int i = 0;
    final Iterator<T> iterator = collection().iterator();
    T v = iterator.next();
    while(i < index) {
      i++;
      v = iterator.next();
    }
    iterator.remove();
    return v;
  }

  @Override
  default int indexOf(Object o) {
    int i = 0;
    final Iterator<T> iterator = collection().iterator();
    T v = iterator.next();
    while(!v.equals(o)) {
      i++;
      v = iterator.next();
    }
    return i;
  }

  @Override
  default int lastIndexOf(Object o) {
    final Iterator<T> iterator = collection().iterator();

    int i = -1;
    int pos = 0;
    T v = iterator.next();
    while(iterator.hasNext()) {
      if(v.equals(o)) {
        pos = i;
      }
      i++;
      v = iterator.next();
    }
    return pos;
  }

  @NotNull
  @Override
  default ListIterator<T> listIterator() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  default ListIterator<T> listIterator(int index) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  default List<T> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }
}
