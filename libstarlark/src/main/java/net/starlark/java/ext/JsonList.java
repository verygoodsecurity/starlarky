package net.starlark.java.ext;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public interface JsonList<T> extends List<T> {
  List<T> get();

  @Override default int size() { return get().size(); }
  @Override default boolean isEmpty() { return get().isEmpty(); }
  @Override default boolean contains(Object o) { return get().contains(o); }
  @Override default Iterator<T> iterator() { return get().iterator(); }
  @Override default Object[] toArray() { return get().toArray(); }
  @Override default <T1> T1[] toArray(T1[] a) { return get().toArray(a); }
  @Override default boolean add(T t) { return get().add(t); }
  @Override default boolean remove(Object o) { return get().remove(o); }
  @Override default boolean containsAll(Collection<?> c) { return get().containsAll(c); }
  @Override default boolean addAll(Collection<? extends T> c) { return get().addAll(c); }
  @Override default boolean addAll(int index, Collection<? extends T> c) { return get().addAll(index, c); }
  @Override default boolean removeAll(Collection<?> c) { return get().removeAll(c); }
  @Override default boolean retainAll(Collection<?> c) { return get().retainAll(c); }
  @Override default void clear() { get().clear(); }
  @Override default T get(int index) { return get().get(index); }
  @Override default T set(int index, T element) { return get().set(index, element); }
  @Override default void add(int index, T element) { get().add(index, element); }
  @Override default T remove(int index) { return get().remove(index); }
  @Override default int indexOf(Object o) { return get().indexOf(o); }
  @Override default int lastIndexOf(Object o) { return get().lastIndexOf(o); }
  @Override default ListIterator<T> listIterator() { return get().listIterator(); }
  @Override default ListIterator<T> listIterator(int index) { return get().listIterator(index); }
  @Override default List<T> subList(int fromIndex, int toIndex) { return get().subList(fromIndex, toIndex); }
  @Override default void replaceAll(UnaryOperator<T> operator) { get().replaceAll(operator); }
  @Override default void sort(Comparator<? super T> c) { get().sort(c); }
  @Override default Spliterator<T> spliterator() { return get().spliterator(); }
  @Override default boolean removeIf(Predicate<? super T> filter) { return get().removeIf(filter); }
  @Override default Stream<T> stream() { return get().stream(); }
  @Override default Stream<T> parallelStream() { return get().parallelStream(); }
  @Override default void forEach(Consumer<? super T> action) { get().forEach(action); }
}

