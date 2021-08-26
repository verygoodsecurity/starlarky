package net.starlark.java.eval;

import java.util.Iterator;

/**
 * A StarlarkIterator value is StarlarkIterable and may be iterated by Starlark language constructs
 * such as {@code for} loops, list and dict comprehensions, and {@code f(*args)}.
 *
 * <p>Functionally this interface is equivalent to {@code java.lang.Iterable}, but it additionally
 * affirms that the iterability of a Java class should be exposed to Starlark programs.
 */
public interface StarlarkIterator<T> extends StarlarkIterable<T>, Iterator<T> {

  @Override
  default Iterator<T> iterator() {
    return this;
  }

  @Override
  boolean hasNext();

  @Override
  T next();
}
