package com.verygood.security.larky.modules.types.structs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

import com.verygood.security.larky.modules.types.LarkyCallable;
import com.verygood.security.larky.modules.types.LarkyCollection;
import com.verygood.security.larky.modules.types.LarkyObject;
import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.HasBinary;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// A trivial struct-like class with Starlark fields defined by a map.
public class SimpleStruct implements LarkyCallable, LarkyCollection, HasBinary, Comparable<Object> {

  private static final TokenKind[] COMPARE_OPNAMES = new TokenKind[]{
    TokenKind.LESS,
    TokenKind.LESS_EQUALS,
    TokenKind.EQUALS_EQUALS,
    TokenKind.NOT_EQUALS,
    TokenKind.GREATER,
    TokenKind.GREATER_EQUALS
  };
  final Map<String, Object> fields;
  final StarlarkThread currentThread;

  protected SimpleStruct(Map<String, Object> fields, StarlarkThread currentThread) {
    this.currentThread = currentThread;
    this.fields = fields;
  }

  public static SimpleStruct immutable(Dict<String, Object> kwargs, StarlarkThread thread) {
    return new ImmutableStruct(ImmutableMap.copyOf(kwargs), thread);
  }

  public static SimpleStruct mutable(Dict<String, Object> kwargs, StarlarkThread thread) {
    return new MutableStruct(kwargs, thread);
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
  public Object getField(String name, StarlarkThread thread) {
    if (
      (name == null
         // we do not support null as field values, signifying does not exist
         || this.fields.getOrDefault(name, null) == null
      ) && this.fields.get(PyProtocols.__GETATTR__) == null) {
      return null;
    }

    if (!fields.containsKey(name)) {
      // if there's an object with a __getattr__, it will be invoked..
      Object getAttrMethod = this.fields.get(PyProtocols.__GETATTR__);
      StarlarkThread evalThread = thread == null ? getCurrentThread() : thread;
      try {
        return Starlark.call(evalThread, getAttrMethod, Tuple.of(name), Dict.empty());
      } catch (EvalException | InterruptedException e) {
        throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(e, evalThread);
      }
    }
    return fields.get(name);
  }

  @Override
  public void repr(Printer p) {
    try {
      if (hasReprField()) {
        final StarlarkCallable reprCallable = (StarlarkCallable) getField(PyProtocols.__REPR__);
        if (reprCallable != null) {
          try {
            p.append(invoke(reprCallable).toString());
            return;
          } catch (EvalException ex) {
            if (!ex.getMessage().contains("'__repr__' called recursively")) {
              throw ex;
            }
          }
        }
      }
    } catch (EvalException ex) {
      throw new RuntimeException(ex);
    }
    p.append("<class '").append(typeName()).append("'>");
  }

  @Override
  public void debugPrint(Printer p) {
    // This repr function prints only the fields.
    // Any methods are still accessible through dir/getattr/hasattr.
    p.append(typeName());
    p.append("(");
    String sep = "";
    for (Map.Entry<String, Object> e : fields.entrySet()) {
      p.append(sep).append(e.getKey()).append(" = ").repr(e.getValue());
      sep = ", ";
    }
    p.append(")");
  }

  /**
   * Avoid un-necessary allocation if we need to override the immutability of the `__dict__` in a subclass for the
   * caller.
   */
  protected Dict.Builder<String, Object> composeAndFillDunderDictBuilder() throws EvalException {
    StarlarkThread thread = getCurrentThread();
    StarlarkList<String> keys = Starlark.dir(thread.mutability(), thread.getSemantics(), this);
    Dict.Builder<String, Object> builder = Dict.builder();
    for (String k : keys) {
      // obviously, ignore the actual __dict__ key since we're in this method already
      if (k.equals(PyProtocols.__DICT__)) {
        continue;
      }
      Object value = getValue(k);
      builder.put(k, value != null ? value : Starlark.NONE);
    }

    return builder;
  }
  @Override
  public boolean truth() {
    // __bool__() is used to implement truth value testing and the built-in operation bool();
    final Object dunderBool = getField(PyProtocols.__BOOL__, getCurrentThread());
    final Object dunderLen = getField(PyProtocols.__LEN__, getCurrentThread());
    final Object res;
    if(dunderBool != null) {
      try {
        res = invoke(dunderBool);
      } catch (EvalException e) {
        throw new RuntimeException(e);
      }
      // it should return False or True.
      if(!(res instanceof Boolean)) {
        throw new RuntimeException("TypeError: __bool__ should return bool, returned " + StarlarkUtil.richType(res));
      }
      return (boolean) res;
    }
    else if(dunderLen != null/*false*/) {
      try {
         res = invoke(dunderLen);
       } catch (EvalException e) {
        throw new RuntimeException(e);
      }
      // it should return non-zero integer (or boolean)
      if(res instanceof Boolean) {
        return Starlark.truth(res);
      }
      else if(res instanceof StarlarkInt
                || (res instanceof LarkyObject && ((LarkyObject)res).isCoerceableToInt())) {
        final StarlarkInt asInt;
        if(res instanceof LarkyObject) {
          try {
            asInt = ((LarkyObject) res).coerceToInt(getCurrentThread());
          } catch (EvalException e) {
            throw new RuntimeException(e);
          }
        } else {
          asInt = (StarlarkInt) res;
        }
        switch(asInt.signum()) {
          case 0:
            return false;
          case 1:
            return true;
          case -1: // if it's a negative number
            // fallthrough
          default:
            throw new RuntimeException("ValueError: __len__() of " + typeName() + "should return >= 0, returned: " + asInt);
        }
      }
      throw new RuntimeException("TypeError: '" + StarlarkUtil.richType(res) + "' object cannot be interpreted as an integer");
    }
    /*
      If a class defines neither __len__() nor __bool__(), all its instances
      are considered true.
    */
    return true;
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
        this, obj, PyProtocols.__EQ__, PyProtocols.__EQ__, this.getCurrentThread()
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
   * The below does not belong in LarkyObject because LarkyObject does not dictate what operations should exist on an
   * object. That is left to the interface implementer.
   *
   * However, for SimpleStruct and its hierarchy tree, in Larky, we can simply "tack-on" the magic method (i.e. __len__
   * or __contains__, etc.) and we expect various operations to work on that object, which is why we want to enable
   * binaryOp on SimpleStruct.
   */
  @Nullable
  @Override
  public Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    return StructBinOp.operatorDispatch(this, op, that, thisLeft, this.getCurrentThread());
  }

  @Override
  public Object get__call__() {
    return getField(PyProtocols.__CALL__);
  }

  @Override
  public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
    // we have to pass the execution thread here b/c otherwise
    // we will pass the thread that was responsible for capturing the
    // the closure -- which is not what we want.
    return invoke(thread, callable(), args, kwargs);
  }

  @Override
  public int compareTo(@NotNull Object o) {
    SimpleStruct other = (SimpleStruct) o;
    Object result;
    final boolean lt;
    final boolean gt;

    try {
      // This code is a bit tricky. If we return null from operatorDispatch,
      // it most likely not a proper comparison operation.
      //
      // To make the IDE happy, we have to do the checks below.
      result = StructBinOp.operatorDispatch(
        this,
        TokenKind.LESS,
        other,
        true,
        this.getCurrentThread()
      );
      if (result instanceof Boolean) {
        lt = (boolean) result;
        if (lt) {
          return -1;
        }
      }
      result = StructBinOp.operatorDispatch(
        this,
        TokenKind.GREATER,
        other,
        true,
        this.getCurrentThread()
      );
      if (result instanceof Boolean) {
        gt = (boolean) result;
        if (gt) {
          return 1;
        }
      }
      // if result is null, let's throw an Error
      if (result == null) {
        throw new RuntimeException(String.format(
          "unsupported binary operation: %s and %s",
          this, other
        ));
      }
    } catch (EvalException e) {
      throw new RuntimeException(e);
    }
    return 0;
  }

}
