package net.starlark.java.eval;

import com.google.common.collect.ImmutableList;

/**
 * A tuple whose subclasses add names, such as Python's {@code collections.namedtuple}.
 *
 * <p>It is a {@link Tuple}, as a namedtuple is a tuple in Python: it equals, hashes like and
 * orders against the plain tuple of its elements, and indexing, unpacking, {@code in},
 * {@code len}, {@code +}, {@code *} and slicing work as for any tuple. Slicing, {@code +} and
 * {@code *} return plain tuples, as in Python. It lives in this package because {@link Tuple}'s
 * constructor and {@link Tuple#repeat} are package-private.
 */
public abstract class NamedTuple extends Tuple {

  /** The elements, as a plain tuple. */
  private final Tuple elems;

  protected NamedTuple(Iterable<?> elems) {
    this.elems = Tuple.copyOf(elems);
  }

  /** The elements as a plain (unnamed) tuple. */
  public final Tuple elements() {
    return elems;
  }

  @Override
  public Object get(int i) {
    return elems.get(i);
  }

  @Override
  public int size() {
    return elems.size();
  }

  @Override
  public boolean isImmutable() {
    return elems.isImmutable();
  }

  @Override
  public void checkHashable() throws EvalException {
    elems.checkHashable();
  }

  /** The hash of the plain tuple, so a namedtuple and that tuple are one dict key. */
  @Override
  public int hashCode() {
    return elems.hashCode();
  }

  @Override
  public boolean containsKey(StarlarkSemantics semantics, Object key) throws EvalException {
    return elems.containsKey(semantics, key);
  }

  @Override
  public boolean contains(Object o) {
    return elems.contains(o);
  }

  @Override
  public Tuple subList(int from, int to) {
    return Tuple.copyOf(elems.subList(from, to));
  }

  @Override
  public Object[] toArray() {
    return elems.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return elems.toArray(a);
  }

  @Override
  public ImmutableList<Object> getImmutableList() {
    return elems.getImmutableList();
  }

  @Override
  public Tuple getSlice(Mutability mu, int start, int stop, int step) throws EvalException {
    return (Tuple) elems.getSlice(mu, start, stop, step);
  }

  @Override
  Tuple repeat(StarlarkInt n) throws EvalException {
    return elems.repeat(n);
  }
}
