package com.verygood.security.larky.modules.types;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Supplier;

import com.verygood.security.larky.modules.types.results.LarkyIndexError;
import com.verygood.security.larky.modules.types.results.LarkyStopIteration;
import com.verygood.security.larky.modules.utils.StringCache;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.HasBinary;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkIterable;
import net.starlark.java.eval.StarlarkIterator;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.Nullable;

public abstract class LarkyIterator implements HasBinary, LarkyObject, StarlarkIterator<Object> {

  final protected ImmutableMap<String, StarlarkCallable> fields = ImmutableMap.of(
    PyProtocols.__NEXT__, new StarlarkCallable() {
      @Override
      public String getName() {
        return PyProtocols.__NEXT__;
      }

      @Override
      public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException {
        return __next__();
      }
    },
    PyProtocols.__ITER__, new StarlarkCallable() {
      @Override
      public String getName() {
        return PyProtocols.__ITER__;
      }

      @Override
      public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException {
        return __iter__();
      }
    }
  );

  final protected ImmutableSet<String> fieldNames = fields.keySet();

  // __iter__() => hasIterField() ? invoke() : hasGetItemField() ? return invoke() : return iterator()
  // __next__() => hasNextField() ? invoke() : return if not hasNext(): raise StopIterable else return next()
  private StarlarkThread currentThread;

  public static LarkyIterator from(Object obj, StarlarkThread thread) throws EvalException {
    // TODO: add iter(dict)..
    if (obj instanceof LarkyIterator) {
      final LarkyIterator obj1 = (LarkyIterator) obj;
      obj1.setCurrentThread(thread);
      return obj1;
    } else if (obj instanceof String) {
      return LarkyStringIterator.of((String) obj, thread);
    } else if (obj instanceof StarlarkIterable) {
      final Iterable<?> iterable = Starlark.toIterable(obj);
      return LarkyIterableIterator.of(iterable, thread);  // TODO: fromIterable()?
    } else if (obj instanceof Iterable) {
      return LarkyIterableIterator.of((Iterable<?>) obj, thread);  // TODO: fromIterable()?
    } else if (obj instanceof LarkyObject) {
      return LarkyObjectIterator.of((LarkyObject) obj, thread);
    }
    throw Starlark.errorf("TypeError: argument of type '%s' is not iterable", Starlark.type(obj));

  }

  /**
   * Returns the name of the type of LarkyIterator as if by the Starlark expression {@code
   * type(x)}.
   */
  public String type() {
    return "iterator";
  }

  @Nullable
  @Override
  public Object getValue(String name) throws EvalException {
    return fields.getOrDefault(name, null);
  }

  @Override
  public ImmutableCollection<String> getFieldNames() {
    return fieldNames;
  }

  public LarkyIterator __iter__() throws EvalException {
    return this;
  }

  abstract public Object __next__() throws EvalException;

  @Override
  public Object next() {
    try {
      return this.invoke(getField(PyProtocols.__NEXT__));
    } catch (EvalException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public StarlarkThread getCurrentThread() {
    return this.currentThread;
  }

  public void setCurrentThread(StarlarkThread thread) {
    this.currentThread = thread;
  }

  @Override
  public void repr(Printer p) {
    p.append("<")
      .append(type()).append(" at 0x").append(System.identityHashCode(this))
      .append(">");
  }

  @Nullable
  @Override
  public Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    //noinspection SwitchStatementWithTooFewBranches
    switch (op) {
      case IN:
        if (thisLeft) {
          if (!(that instanceof LarkyIterator)) {
            throw Starlark.errorf("TypeError: argument of type '%s' is not iterable", Starlark.type(that));
          }
          return Iterables.contains((Iterable<?>) that, this);
        } else {
          return Iterables.contains(this, that);
        }

      default:
        // unsupported binary operation!
        return null;
    }
  }

  private static class LarkyStringIterator extends LarkyIterator {

    private final String obj;
    private final int length;
    private int pos = 0;

    private LarkyStringIterator(String obj) {
      this.obj = obj;
      this.length = obj.length();
    }

    public static LarkyStringIterator of(String obj, StarlarkThread thread) {
      final LarkyStringIterator rval = new LarkyStringIterator(obj);
      rval.setCurrentThread(thread);
      return rval;
    }

    @Override
    public Object __next__() throws EvalException {
      if (hasNext()) {
        return StringCache.valueOf(obj.charAt(pos++));
      }
      throw LarkyStopIteration.getInstance();
    }

    @Override
    public boolean hasNext() {
      return length != 0 && pos < length;
    }

    @Override
    public String type() {
      return "str_iterator";
    }
  }

  private static class LarkyIterableIterator extends LarkyIterator {
    private final Iterator<?> iterator;
    private final String type;

    private LarkyIterableIterator(Iterable<?> obj) {
      this.iterator = obj.iterator();
      this.type = Starlark.type(obj) + "_iterator";

    }

    public static LarkyIterableIterator of(Iterable<?> obj, StarlarkThread thread) {
      final LarkyIterableIterator rval = new LarkyIterableIterator(obj);
      rval.setCurrentThread(thread);
      return rval;
    }

    @Override
    public String type() {
      return this.type;
    }

    @Override
    public Object __next__() throws EvalException {
      if (hasNext()) {
        return iterator.next();
      }
      throw LarkyStopIteration.getInstance();
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

  }

  private static class LarkyObjectIterator extends LarkyIterator {
    private final String type;
    protected Supplier<Object> iterator_method;
    protected boolean checkedForNext;
    protected Object nextVal;

    private LarkyObjectIterator(Supplier<Object> obj, String type) {
      this.iterator_method = obj;
      this.type = type;
    }

    public static LarkyObjectIterator of(LarkyObject obj, StarlarkThread thread) throws EvalException {
      final LarkyObjectIterator larkIter;
      final Object iter_func = obj.getField(PyProtocols.__ITER__);

      if (iter_func == null) {
        // check to see if it has __getitem__ and __len__
        larkIter = LarkyGetItemIterator.of(obj, thread);
      } else if (!StarlarkUtil.isCallable(iter_func)) {
        throw Starlark.errorf("TypeError: '%s'.__iter__ is not callable ", obj.type());
      } else {
        larkIter = new LarkyObjectIterator(invokable(obj, (StarlarkCallable) iter_func), obj.type());
      }
      larkIter.setCurrentThread(thread);
      return larkIter;
    }

    // TODO(mahmoudimus): should larkyobject extend all the various interfaces and map them
    //                    to __dunder__ methods?
    static Supplier<Object> invokable(LarkyObject obj, StarlarkCallable dunder_iter) throws EvalException {
      final LarkyObject iterator = (LarkyObject) obj.invoke(dunder_iter);
      /*
         From the documentation (https://docs.python.org/3/reference/datamodel.html#object.__iter__):

             Iterator objects also need to implement this method; _they are required to
             return themselves_ (emphasis added).

         For more information on iterator objects, see Iterator Types here:
          (https://docs.python.org/3/library/stdtypes.html#typeiter)
       */

      if (iterator == null || obj != iterator) { // this is an instance identity check!
        throw Starlark.errorf(
          "ValueError: __iter__() on iterator object (%s) are required to return" +
            "themselves (https://docs.python.org/3/reference/datamodel.html#object.__iter__)",
          obj.type());
      }

      // make sure it has __next__
      if (iterator.getField(PyProtocols.__NEXT__) == null) {
        throw Starlark.errorf("iter() returned non-iterator of type '%s'", obj.type());
      }

      return () -> {
        try {
          return iterator.invoke(iterator.getField(PyProtocols.__NEXT__));
        } catch (EvalException e) {
          if (e instanceof LarkyStopIteration) {
            // NOTE: If an error is thrown here, we will
            return null;
          } else {
            throw new RuntimeException(e);
          }
        }
      };
    }

    @Override
    public String type() {
      return this.type + "_iterator";
    }

    @Override
    public Object __next__() throws EvalException {
      if (!hasNext()) {
        this.iterator_method = null;
        throw LarkyStopIteration.getInstance();
      }
      Object rval = nextVal;
      checkedForNext = false;
      nextVal = null;
      return rval;
    }

    @Override
    public boolean hasNext() {
      if (!checkedForNext) {
        nextVal = this.iterator_method != null ? this.iterator_method.get() : null;
        checkedForNext = true;
      }
      return nextVal != null;
    }

    static class LarkyGetItemIterator extends LarkyObjectIterator {

      private final int length;
      private int pos = 0;

      private LarkyGetItemIterator(Supplier<Object> gi_func, String type, int length) {
        super(gi_func, type);
        this.length = length;
      }

      public static LarkyGetItemIterator of(LarkyObject obj, StarlarkThread thread) throws EvalException {
        final Object gi_func = obj.getField(PyProtocols.__GETITEM__);
        if (gi_func == null) {
          throw Starlark.errorf("TypeError: '%s' object is not iterable", obj.type());
        } else if (!StarlarkUtil.isCallable(gi_func)) {
          throw Starlark.errorf("TypeError: '%s'.__getitem__ is not callable ", obj.type());
        }

        final Object len_func = obj.getField(PyProtocols.__LEN__);
        int len = -1;
        if (len_func != null) {
          if (!StarlarkUtil.isCallable(len_func)) {
            throw Starlark.errorf("TypeError: '%s'.__len__ is not callable ", obj.type());
          }
          len = Starlark.toInt(obj.invoke(len_func), "len_func in LarkyGetItemIterator");
        }
        final LarkyGetItemIterator rval = new LarkyGetItemIterator(() -> gi_func, obj.type(), len);
        rval.setCurrentThread(thread);
        return rval;
      }

      @Override
      public boolean hasNext() {
        if (!checkedForNext && (length == -1 || length != 0 && pos < length)) {
          try {
            nextVal = this.invoke(this.iterator_method.get(), Tuple.of(StarlarkInt.of(pos++)));
          }
          catch(EvalException e) {
            nextVal = null;
            throw new RuntimeException(e);
          }
          if (length < 0 && nextVal == LarkyIndexError.getInstance()) {
            // infinite iterator, so check to see if it's an IndexError
            nextVal = null;
          }
          checkedForNext = true;
        }
        return nextVal != null;
      }
    }
  }

  public static class LarkyCallableIterator extends LarkyObjectIterator {

    private final Object sentinelO;

    private LarkyCallableIterator(Supplier<Object> obj, String type, Object sentinelO) {
      super(obj, type);
      this.sentinelO = sentinelO;
    }

    public static LarkyCallableIterator of(StarlarkCallable method, Object sentinelO, StarlarkThread thread) throws EvalException {
      if(!StarlarkUtil.isCallable(method) || StarlarkUtil.isNullOrNoneOrUnbound(sentinelO)) {
        throw Starlark.errorf("TypeError: iter(v, w): v must be callable");
      }
      final LarkyCallableIterator rval = new LarkyCallableIterator(() -> method, Starlark.type(method), sentinelO);
      rval.setCurrentThread(thread);
      return rval;
    }

    @Override
    public boolean hasNext() {
      if (!checkedForNext) {
        try {
          nextVal = this.invoke(this.iterator_method.get());
        }
        catch(EvalException e) {
          nextVal = null;
          throw new RuntimeException(e);
        }
        // the callable is called until it returns the sentinel.
        // TODO: should we check to see if `__eq__` is defined?
        if (Objects.equals(nextVal, sentinelO) || nextVal == LarkyStopIteration.getInstance()) {
          nextVal = null;
        }
        checkedForNext = true;
      }
      return nextVal != null;
    }
  }

}
