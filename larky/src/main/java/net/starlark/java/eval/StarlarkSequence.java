package net.starlark.java.eval;

import static java.lang.Math.min;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.jetbrains.annotations.NotNull;

public interface StarlarkSequence<K extends Collection<T>, T> extends Sequence<T>, Comparable<Sequence<T>> {

  // interface
  //Collection<T> collection();
  K collection();

  @Override
  Sequence<T> getSlice(Mutability mu, int start, int stop, int step);

  @NotNull
  @Override
  Iterator<T> iterator();


  /**
    * Compares two sequences of values.
   *
   *  Sequences compare equal if corresponding elements compare
    * equal using {@code iterator.next().equals(iterator.next())}.
   *
   *  Otherwise, the result is the ordered comparison of the first
    * element for which {@code !iterator.next().equals(iterator.next())}.
   *
   *  If one sequence is a prefix of another, the result is the
    * comparison of the sequence's sizes.
    *
    * @throws ClassCastException if any comparison failed.
    */
  // Suppress duplicated code because StarlarkSequence should be a
  // Collection and not necessarily a *list*.
  @SuppressWarnings("DuplicatedCode")
  @Override
  default int compareTo(@NotNull Sequence<T> o) {
    for (int i = 0; i < min(this.size(), o.size()); i++) {
      Object xelem = this.get(i);
      Object yelem = o.get(i);

      // First test for equality. This avoids an unnecessary
      // ordered comparison, which may be unsupported despite
      // the values being equal. Also, it is potentially more
      // expensive. For example, list==list need not look at
      // the elements if the lengths are unequal.
      if (xelem == yelem || xelem.equals(yelem)) {
        continue;
      }

      // The ordered comparison of unequal elements should
      // always be nonzero unless compareTo is inconsistent.
      int cmp = Starlark.compareUnchecked(xelem, yelem);
      if (cmp == 0) {
        throw new IllegalStateException(
            String.format(
                "x.equals(y) yet x.compareTo(y)==%d (x: %s, y: %s)",
                cmp, Starlark.type(xelem), Starlark.type(yelem)));
      }
      return cmp;
    }
    return Integer.compare(this.size(), o.size());
  }

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

  @NotNull
  @Override
  default <T1> T1 @NotNull [] toArray(@NotNull T1 @NotNull [] a) {
    if (a.length < collection().size()) {
      try {
        //noinspection unchecked
        return (T1[]) collection().toArray(a.getClass().getDeclaredConstructor().newInstance());
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    } else {
      final Iterator<T> iterator = collection().iterator();
      int i = 0;
      while(iterator.hasNext()) {
        T v = iterator.next();
        //noinspection unchecked
        a[i] = (T1) v;
        i++;
      }
      Arrays.fill(a, i, a.length, null);
      return a;
    }
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
