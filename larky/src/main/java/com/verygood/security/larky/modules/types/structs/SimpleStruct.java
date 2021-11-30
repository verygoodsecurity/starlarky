package com.verygood.security.larky.modules.types.structs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Iterator;
import java.util.Map;

import com.verygood.security.larky.modules.types.LarkyCallable;
import com.verygood.security.larky.modules.types.LarkyIndexable;
import com.verygood.security.larky.modules.types.LarkyIterator;
import com.verygood.security.larky.modules.types.PyProtocols;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.HasBinary;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkIterable;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// A trivial struct-like class with Starlark fields defined by a map.
public class SimpleStruct implements LarkyIndexable, LarkyCallable, StarlarkIterable<Object>, HasBinary, Comparable<Object> {

  final Map<String, Object> fields;
  final StarlarkThread currentThread;

  public static SimpleStruct create(Map<String, Object> kwargs) {
    return new SimpleStruct(kwargs, null);
  }

  public static SimpleStruct immutable(Dict<String, Object> kwargs, StarlarkThread thread) {
    return new ImmutableStruct(ImmutableMap.copyOf(kwargs), thread);
  }

  public static SimpleStruct mutable(Dict<String, Object> kwargs, StarlarkThread thread) {
    return new MutableStruct(kwargs, thread);
  }

  protected SimpleStruct(Map<String, Object> fields, StarlarkThread currentThread) {
    this.currentThread = currentThread;
    this.fields = fields;
  }

  @StarlarkMethod(name = PyProtocols.__DICT__, structField = true)
  public Dict<String, Object> dunderDict() throws EvalException {
    return composeAndFillDunderDictBuilder().build(Mutability.IMMUTABLE);
  }

  @Override
  public StarlarkThread getCurrentThread() {
    return currentThread;
  }

  @Override
  public ImmutableList<String> getFieldNames() {
    return ImmutableList.copyOf(fields.keySet());
  }

  @Override
  public Object getValue(String name) throws EvalException {
    if(name == null
        || !fields.containsKey(name)
        || fields.getOrDefault(name, null) == null) {
      return null;
    }

    return fields.get(name);
  }


  @Override
  public void repr(Printer p) {
    try {
      if (hasReprField()) {
        final StarlarkCallable reprCallable = (StarlarkCallable) getField(PyProtocols.__REPR__);
        if (reprCallable != null) {
          p.append((String)invoke(reprCallable));
          return;
        }
      }
    } catch (EvalException ex) {
      throw new RuntimeException(ex);
    }
    p.append("<class '").append(type()).append("'>");
  }

  @Override
  public void debugPrint(Printer p) {
    // This repr function prints only the fields.
    // Any methods are still accessible through dir/getattr/hasattr.
    p.append(type());
    p.append("(");
    String sep = "";
    for (Map.Entry<String, Object> e : fields.entrySet()) {
      p.append(sep).append(e.getKey()).append(" = ").repr(e.getValue());
      sep = ", ";
    }
    p.append(")");
  }

  /**
   * Avoid un-necessary allocation if we need to override the immutability of the `__dict__` in a
   * subclass for the caller.
   * */
  protected Dict.Builder<String, Object> composeAndFillDunderDictBuilder() throws EvalException {
    StarlarkThread thread = getCurrentThread();
    StarlarkList<String> keys = Starlark.dir(thread.mutability(), thread.getSemantics(), this);
    Dict.Builder<String, Object> builder = Dict.builder();
    for(String k : keys) {
      // obviously, ignore the actual __dict__ key since we're in this method already
      if(k.equals(PyProtocols.__DICT__)) {
        continue;
      }
      Object value = getValue(k);
      builder.put(k,  value != null ? value : Starlark.NONE);
    }

    return builder;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SimpleStruct)) {
      return false;
    }
    if (this == obj) {
      return true;
    }

    boolean result;
    try {
      result = StructBinOp.richComparison(
        this, obj, PyProtocols.__EQ__, PyProtocols.__NE__, this.getCurrentThread()
      );
    } catch (EvalException e) {
      result = false;
    }
    return result;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /**
   * The below does not belong in LarkyObject because LarkyObject does not dictate what operations
   * should exist on an object. That is left to the interface implementer.
   *
   * However, for SimpleStruct and its hierarchy tree, in Larky, we can simply "tack-on" the
   * magic method (i.e. __len__ or __contains__, etc.) and we expect various operations to work
   * on that object, which is why we want to enable binaryOp on SimpleStruct.
   */
  @Nullable
  @Override
  public Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    return StructBinOp.operatorDispatch(this, op, that, thisLeft, this.getCurrentThread());
  }

  @Override
  public boolean containsKey(StarlarkThread starlarkThread, StarlarkSemantics semantics, Object key) throws EvalException {
    final Object result = StructBinOp.operatorDispatch(this, TokenKind.IN, key, false, starlarkThread);
    if(result == null) {
      throw Starlark.errorf(
              "unsupported binary operation: %s %s %s", Starlark.type(key), TokenKind.IN, type());
    }
    return (boolean) result;
  }

  @NotNull
  @Override
  public Iterator<Object> iterator() {
    try {
      return LarkyIterator.LarkyObjectIterator.of(this, getCurrentThread());
    } catch (EvalException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object get__call__() {
    try {
      return getField(PyProtocols.__CALL__);
    } catch (EvalException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
    // we have to pass the execution thread here b/c otherwise
    // we will pass the thread that was responsible for capturing the
    // the closure -- which is not what we want.
    return invoke(thread, callable(), args, kwargs);
  }

  private static final TokenKind[] COMPARE_OPNAMES = new TokenKind[]{
    TokenKind.LESS,
    TokenKind.LESS_EQUALS,
    TokenKind.EQUALS_EQUALS,
    TokenKind.NOT_EQUALS,
    TokenKind.GREATER,
    TokenKind.GREATER_EQUALS
  };

  @Override
  public int compareTo(@NotNull Object o) {
    SimpleStruct other = (SimpleStruct) o;

    try {
      final boolean lt = (Boolean) StructBinOp.operatorDispatch(
        this,
        TokenKind.LESS,
        other,
        true,
        this.getCurrentThread()
      );
      if (lt) {
        return -1;
      }
      final boolean gt = (boolean) StructBinOp.operatorDispatch(
        this,
        TokenKind.GREATER,
        other,
        true,
        this.getCurrentThread()
      );
      if (gt) {
        return 1;
      }
    } catch (EvalException e) {
      throw new RuntimeException(e);
    }
    return 0;
  }
}
