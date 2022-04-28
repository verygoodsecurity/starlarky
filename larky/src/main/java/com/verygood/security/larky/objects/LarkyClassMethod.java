package com.verygood.security.larky.objects;

import java.util.Objects;

import com.verygood.security.larky.objects.descriptor.LarkyDescriptor;
import com.verygood.security.larky.objects.descriptor.LarkyNonDataDescriptor;
import com.verygood.security.larky.objects.type.LarkyType;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import org.jetbrains.annotations.Nullable;

/**
 * The classmethod builtin function.
 */
@StarlarkBuiltin(name = "classmethod")
public class LarkyClassMethod extends LarkyStaticMethod {

  public static final LarkyType TYPE = LarkyTypeObject.createBuiltinType("classmethod");
  public static final LarkyClassMethod INSTANCE = new LarkyClassMethod();

  LarkyClassMethod() {
  }

  public static LarkyClassMethod getInstance() {
    return INSTANCE;
  }

  @Override
  public LarkyType delegate() {
    return TYPE;
  }

  @Override
  public PyObject __new__(Tuple args, Dict<String, Object> kwargs, StarlarkThread thread) {
    return new LarkyClassMethodInstance((StarlarkFunction) args.get(0), thread);
  }

  @Override
  public String getName() {
    return TYPE.__name__();
  }

  public static class LarkyClassMethodInstance extends LarkyFunction {

    private LarkyClassMethodInstance(StarlarkCallable function, StarlarkThread thread) {
      super(LarkyClassMethod.TYPE, function, thread);
    }

    @StarlarkMethod(name = "__func__", structField = true)
    public StarlarkCallable wrapped() {
      return this.fget;
    }

    @StarlarkMethod(name = "__self__", structField = true)
    public LarkyType self() {
      return this.im_class;
    }

    @Override
    public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
      return Starlark.call(thread, this.fget, Tuple.concat(Tuple.of(this.im_class), args), kwargs);
    }

    @Override
    public Object __get__(Object obj, LarkyType type, @Nullable StarlarkThread thread) throws EvalException, InterruptedException {
      if (type == null && obj instanceof PyObject) {
        type = ((PyObject) obj).typeClass();
      }
      if (obj != null) {
        check(obj, thread);
      }
      thread = thread != null ? thread : getCurrentThread();
      if (LarkyNonDataDescriptor.isNonDataDescriptor(this.fget)) {
        return ((LarkyDescriptor) this.fget).__get__(type, type, thread);
      }
      return LarkyMethod.create(this.fget, Objects.requireNonNull(type), thread);
    }
  }
}
