package com.verygood.security.larky.objects;

import com.verygood.security.larky.objects.type.LarkyBaseObjectType;
import com.verygood.security.larky.objects.type.LarkyType;
import com.verygood.security.larky.objects.type.LarkyTypeObject;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Create a bound instance method object.
 */
public class LarkyMethod extends LarkyFunction {

  public static final LarkyTypeObject TYPE = new LarkyTypeObject(Origin.BUILTIN, "method", Dict.empty());
  static {
    LarkyType.setupInheritanceHierarchy(TYPE, new LarkyType[]{(LarkyType) LarkyBaseObjectType.getInstance()});
  }

  PyObject im_self;


  private LarkyMethod(@NotNull StarlarkCallable function, @NotNull PyObject im_self, @NotNull StarlarkThread thread) {
    super(TYPE, function, thread);
    this.im_self = im_self;
    bind(im_self.typeClass());
  }

  @Contract("_, _, _ -> new")
  public static @NotNull LarkyMethod create(@NotNull StarlarkCallable function, @NotNull PyObject im_self, @NotNull StarlarkThread thread) {
    return new LarkyMethod(function, im_self, thread);
  }

  @StarlarkMethod(name = "__objclass__", structField = true)
  public LarkyType objClass() {
    return getBoundOwner();
  }

  @StarlarkMethod(name = "__func__", structField = true)
  public StarlarkCallable __func__() {
    return this.fget;
  }

  @StarlarkMethod(name = "__self__", structField = true)
  public PyObject __self__() {
    return this.im_self;
  }

  @Override
  public String __repr__() {
    String result;
    if (this.im_self != null) {
      //  <bound method O.__init__ of <__main__.O object at 0x10731ea60>>
      result = "<bound method ";
      if(isBound()) {
        assert getBoundOwner() != null;
        result += getBoundOwner().__name__() + "." ;
      }
      result += this.fget.getName() + " of " + this.im_self + ">";
    } else {
      result = super.__repr__();
    }
    return result;
  }

  @Override
  public String getName() {
    return __repr__();
  }

  @Override
  public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
    return Starlark.call(thread, this.fget, Tuple.concat(Tuple.of(this.im_self), args), kwargs);
  }

  @Override
  public Object __get__(Object obj, LarkyType type, @Nullable StarlarkThread thread) throws EvalException {
    if (obj == null) {
      throw new EvalException("__get__(None, None) is invalid");
    }
    return this;
  }

  @Override
  public LarkyType delegate() {
    return this.im_class;
  }

  @Override
  public StarlarkThread getCurrentThread() {
    return this.im_self.getCurrentThread();
 }


  @Override
  public LarkyType getBoundOwner() {
    return im_class;
  }

  @Override
  public void bindToOwner(LarkyType cls) {
    im_class = cls;
  }
}