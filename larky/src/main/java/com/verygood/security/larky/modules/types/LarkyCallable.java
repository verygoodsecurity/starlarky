package com.verygood.security.larky.modules.types;

import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;

public interface LarkyCallable extends StarlarkCallable {

  Object get__call__();

  default StarlarkCallable callable() throws EvalException {
    Object callableO = get__call__();
    if (callableO instanceof StarlarkCallable) {
      return (StarlarkCallable) callableO;
    }
    //StarlarkCallable.super.call(thread, args, kwargs);
    throw Starlark.errorf(
      "'%s' object is not callable (either def __call__(*args, **kwargs) is not " +
      "defined or __call__ is defined but is not callable)",
      StarlarkUtil.richType(this));
  }

  @Override
  default String getName() {
    StarlarkCallable method;
    StringBuilder name = new StringBuilder(StarlarkUtil.richType(this));
    try {
      method = callable();
    } catch (EvalException ex) {
      Object methodO = get__call__();
      String methodType;
      String methodName;
      if(methodO != null) {
         methodType = StarlarkUtil.richType(methodO);
         methodName = Starlark.str(methodO);
      } else {
        methodType = "None";
        methodName = "None";
      }
      name.append(".")
        .append("__call__<type: ")
        .append(methodType)
        .append(", value=")
        .append(methodName)
        .append(">");
      return name.toString();
    }
    name.append(".").append(method.getName());
    return name.toString();
  }
}
