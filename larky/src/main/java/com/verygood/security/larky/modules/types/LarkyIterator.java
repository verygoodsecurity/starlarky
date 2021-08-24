package com.verygood.security.larky.modules.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.verygood.security.larky.modules.types.results.LarkyStopIteration;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkIterator;

public interface LarkyIterator extends LarkyObject, StarlarkIterator<Object> {

  default boolean hasNextField() throws EvalException {
    return getField(PyProtocols.__NEXT__) != null;
  }

  default boolean hasIterField() throws EvalException {
    return getField(PyProtocols.__ITER__) != null;
  }

  // previously when __iter__ method was not defined,
  // it fell back to __getitem__ by successively calling __getitem__ with increasing values
  // until it gives index out of range error.
  default boolean hasGetItemField() throws EvalException {
    return getField(PyProtocols.__GETITEM__) != null;
  }

  /**
   * Returns the name of the type of LarkyIterator as if by the Starlark
   * expression {@code type(x)}.
   */
  default String type() {
    return "iterator";
  }

  // __iter__() => hasIterField() ? invoke() : hasGetItemField() ? return invoke() : return iterator()
  // __next__() => hasNextField() ? invoke() : return if not hasNext(): raise StopIterable else return next()

  Object __next__() throws EvalException;

  default StarlarkIterator<?> __iter__() throws EvalException {
    return this;
  }

  static LarkyIterator of(Object obj) throws EvalException {
    if (obj instanceof LarkyIterator) {
      return (LarkyIterator) obj;
    } else {
      throw new RuntimeException();
    }
  }

  static List<Object> toJList(LarkyIterator iter) throws EvalException {
      List<Object> list = new ArrayList<>();
      while (true) {
          try {
              list.add(iter.__next__());
          } catch (LarkyStopIteration exp) {
              return list;
          }
      }
  }

  static Iterator<Object> toJIterator(LarkyIterator iter) throws EvalException {
    Object starlarkObject;
    try {
      starlarkObject = iter.__next__();
    } catch (LarkyStopIteration exp) {
        return Collections.emptyIterator();
    }

    return new Iterator<Object>() {
      private Object elem = starlarkObject;

      @Override
      public boolean hasNext() {
          return elem != null;
      }

      @Override
      public Object next() {
        if (elem != null) {
          Object ret = elem;
            try {
                elem = iter.__next__();
            } catch (LarkyStopIteration exp) {
                elem = null;
            } catch (EvalException exp) {
                throw new RuntimeException(exp);
            }
            return ret;
        } else {
            throw new IllegalStateException("end of iterator");
        }
      }
    };
  }

//
//  /**
//   * Exposes a Python iter as a Java Iterator.
//   */
//  abstract class WrappedIterIterator<E> implements Iterator<E> {
//
//      private final StarlarkIterable<E> iter;
//      private Object next;
//      private boolean checkedForNext;
//
//      public WrappedIterIterator(StarlarkIterable<E> iter) {
//          this.iter = iter;
//      }
//
//      public boolean hasNext() {
//          if (!checkedForNext) {
//              try {
//                  next = PyObject.iterNext(iter);
//              } catch (PyException e) {
//                  if (e.match(Py.StopIteration)) {
//                      next = null;
//                  } else {
//                      throw e;
//                  }
//              }
//              checkedForNext = true;
//          }
//          return next != null;
//      }
//
//      /**
//       * Subclasses must implement this to turn the type returned by the iter to the type expected by
//       * Java.
//       */
//      public abstract E next();
//
//      public PyObject getNext() {
//          if (!hasNext()) {
//              throw new NoSuchElementException("End of the line, bub");
//          }
//          PyObject toReturn = next;
//          checkedForNext = false;
//          next = null;
//          return toReturn;
//      }
//
//      public void remove() {
//          throw new UnsupportedOperationException("Can't remove from a Python iterator");
//      }
//  }

}
