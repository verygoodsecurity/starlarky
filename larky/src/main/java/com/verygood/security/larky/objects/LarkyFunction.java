package com.verygood.security.larky.objects;


import com.verygood.security.larky.objects.descriptor.LarkyDescriptor;
import com.verygood.security.larky.objects.type.ForwardingLarkyType;
import com.verygood.security.larky.objects.type.LarkyType;
import com.verygood.security.larky.objects.type.LarkyTypeObject;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wraps a {@link StarlarkFunction} with a descriptor to mimic python's bound method behavior.
 *
 * This is described in detail in
 * <a href="https://docs.python.org/3.10/howto/descriptor.html#functions-and-methods">Python's descriptor tutorial</a>.
 *
 * <blockquote><pre>This means that functions are non-data descriptors that return bound methods during dotted lookup
 * from an instance</pre></blockquote>
 */
@StarlarkBuiltin(name = "function")
public class LarkyFunction extends LarkyDescriptor implements ForwardingLarkyType, StarlarkCallable {
  public static final LarkyType TYPE = LarkyTypeObject.createBuiltinType("function");

  @Nullable LarkyType im_class;

  LarkyFunction(@NotNull LarkyType forType, @NotNull StarlarkCallable function, @NotNull StarlarkThread thread) {
    super(function.getName(), function, forType, thread);
    this.im_class = null;
  }

  @Contract("_, _ -> new")
  public static @NotNull LarkyFunction create(@NotNull StarlarkCallable function, @NotNull StarlarkThread thread) {
    return new LarkyFunction(TYPE, function, thread);
  }

  @Override
  public LarkyType delegate() {
    return __class__();
  }

  @Override
  public LarkyType getBoundOwner() {
    return im_class;
  }

  @Override
  public void bindToOwner(LarkyType cls) {
    im_class = cls;
  }

  @Override
  public String __repr__() {
    String result = "<" + this.__name__() + " ";
    if (isBound()) {
      assert getBoundOwner() != null;
      result += getBoundOwner().__name__() + ".";
    }
    result += this.fget.getName() + ">";
    return result;
  }

  @Override
  public String getName() {
    return __repr__();
  }

  @Override
  public void debugPrint(Printer p) {
    repr(p);
  }

  @Override
  public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
    return Starlark.call(thread, this.fget, args, kwargs);
  }

  @Override
  public Object __get__(Object obj, LarkyType type, @Nullable StarlarkThread thread) throws EvalException, InterruptedException {
    if (obj == null) {
      return this;
    } else {
      final PyObject self = check(obj, thread);
      // A possible optimization here would be to cache the LarkyMethod
      // creation based on the this.fget and this.im_class.
      //
      // This would reduce memory allocation, as this will probably be a
      // hot codepath.
      return LarkyMethod.create(
        this.fget, self,
        thread != null ? thread : self.getCurrentThread()
      );
    }
  }
}
